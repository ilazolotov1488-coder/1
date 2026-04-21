package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Красивая система трейлов за курсором с частицами
 */
public class MouseTrailEffect {
    
    private final List<TrailParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    
    private float lastMouseX = -1;
    private float lastMouseY = -1;
    private long lastSpawnTime = 0;
    
    // Настройки
    private static final int SPAWN_INTERVAL = 8; // мс между спавном частиц
    private static final int MAX_PARTICLES = 150;
    private static final float MIN_VELOCITY = 0.3f;
    private static final float MAX_VELOCITY = 1.2f;
    
    // Цветовая палитра (фиолетово-синяя)
    private static final ColorRGBA[] COLORS = {
        new ColorRGBA(108, 99, 210, 255),   // фиолетовый
        new ColorRGBA(130, 120, 240, 255),  // светло-фиолетовый
        new ColorRGBA(90, 150, 255, 255),   // голубой
        new ColorRGBA(160, 100, 255, 255),  // розово-фиолетовый
        new ColorRGBA(70, 180, 255, 255),   // яркий голубой
    };
    
    public void update(float mouseX, float mouseY) {
        long now = System.currentTimeMillis();
        
        // Спавним частицы если мышка двигается
        if (lastMouseX != -1 && lastMouseY != -1) {
            float dx = mouseX - lastMouseX;
            float dy = mouseY - lastMouseY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            // Спавним больше частиц при быстром движении
            if (dist > 2 && now - lastSpawnTime > SPAWN_INTERVAL) {
                int count = Math.min(3, (int)(dist / 10) + 1);
                for (int i = 0; i < count; i++) {
                    spawnParticle(mouseX, mouseY, dx, dy);
                }
                lastSpawnTime = now;
            }
        }
        
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        
        // Обновляем частицы
        Iterator<TrailParticle> it = particles.iterator();
        while (it.hasNext()) {
            TrailParticle p = it.next();
            p.update();
            if (p.isDead()) {
                it.remove();
            }
        }
        
        // Ограничиваем количество
        while (particles.size() > MAX_PARTICLES) {
            particles.remove(0);
        }
    }
    
    private void spawnParticle(float x, float y, float dx, float dy) {
        // Случайное направление с небольшим влиянием движения мыши
        float angle = (float) (random.nextFloat() * Math.PI * 2);
        float speed = MIN_VELOCITY + random.nextFloat() * (MAX_VELOCITY - MIN_VELOCITY);
        
        float vx = (float) Math.cos(angle) * speed + dx * 0.1f;
        float vy = (float) Math.sin(angle) * speed + dy * 0.1f;
        
        // Небольшой разброс позиции
        float px = x + (random.nextFloat() - 0.5f) * 4;
        float py = y + (random.nextFloat() - 0.5f) * 4;
        
        // Случайный цвет из палитры
        ColorRGBA color = COLORS[random.nextInt(COLORS.length)];
        
        // Случайный размер
        float size = 2f + random.nextFloat() * 3f;
        
        // Случайное время жизни
        int lifetime = 400 + random.nextInt(400);
        
        particles.add(new TrailParticle(px, py, vx, vy, size, color, lifetime));
    }
    
    public void render(DrawContext ctx, float alpha) {
        for (TrailParticle p : particles) {
            p.render(ctx, alpha);
        }
    }
    
    public void reset() {
        particles.clear();
        lastMouseX = -1;
        lastMouseY = -1;
    }
    
    private static class TrailParticle {
        private float x, y;
        private float vx, vy;
        private final float size;
        private final ColorRGBA color;
        private final int maxLifetime;
        private int lifetime;
        
        // Физика
        private static final float GRAVITY = 0.02f;
        private static final float DRAG = 0.98f;
        
        public TrailParticle(float x, float y, float vx, float vy, float size, ColorRGBA color, int lifetime) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.maxLifetime = lifetime;
            this.lifetime = lifetime;
        }
        
        public void update() {
            // Применяем скорость
            x += vx;
            y += vy;
            
            // Гравитация и сопротивление
            vy += GRAVITY;
            vx *= DRAG;
            vy *= DRAG;
            
            // Уменьшаем время жизни
            lifetime--;
        }
        
        public void render(DrawContext ctx, float globalAlpha) {
            if (lifetime <= 0) return;
            
            // Fade out эффект
            float lifeRatio = (float) lifetime / maxLifetime;
            float fadeAlpha = lifeRatio * globalAlpha;
            
            // Размер уменьшается со временем
            float currentSize = size * (0.3f + lifeRatio * 0.7f);
            
            // Рисуем частицу с свечением
            try {
                // Внешнее свечение
                DrawUtil.drawShadow(ctx.getMatrices(), 
                    x - currentSize * 2, y - currentSize * 2, 
                    currentSize * 4, currentSize * 4,
                    currentSize * 3, BorderRadius.all(currentSize * 2),
                    color.mulAlpha(fadeAlpha * 0.3f));
                
                // Основная частица
                DrawUtil.drawRoundedRect(ctx.getMatrices(), 
                    x - currentSize / 2, y - currentSize / 2, 
                    currentSize, currentSize,
                    BorderRadius.all(currentSize / 2),
                    color.mulAlpha(fadeAlpha));
                
                // Яркое ядро
                DrawUtil.drawRoundedRect(ctx.getMatrices(), 
                    x - currentSize / 4, y - currentSize / 4, 
                    currentSize / 2, currentSize / 2,
                    BorderRadius.all(currentSize / 4),
                    ColorRGBA.WHITE.mulAlpha(fadeAlpha * 0.6f));
                    
            } catch (Exception ignored) {
                // Fallback если шейдеры не работают
                ctx.fill((int)(x - currentSize/2), (int)(y - currentSize/2), 
                        (int)(x + currentSize/2), (int)(y + currentSize/2), 
                        color.mulAlpha(fadeAlpha).getRGB());
            }
        }
        
        public boolean isDead() {
            return lifetime <= 0;
        }
    }
}
