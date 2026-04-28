package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import space.visuals.Zenith;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.level.Render3DUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "LineGlyphs", category = Category.RENDER, description = "Падающие линии-глифы в мире")
public final class LineGlyphs extends Module {

    public static final LineGlyphs INSTANCE = new LineGlyphs();

    private final BooleanSetting glowing   = new BooleanSetting("Свечение", true);
    private final ModeSetting    colorMode = new ModeSetting("Цвет", "Клиентский", "Кастом", "Двойной");
    private final ColorSetting   color1    = new ColorSetting("Цвет 1", new ColorRGBA(100, 200, 255));
    private final ColorSetting   color2    = new ColorSetting("Цвет 2", new ColorRGBA(255, 100, 200));
    private final NumberSetting  count     = new NumberSetting("Количество", 120f, 10f, 300f, 10f);
    private final NumberSetting  radius    = new NumberSetting("Дальность", 20f, 5f, 60f, 1f);

    private static final float FALL_SPEED = 0.07f;

    // Статические направления — не создаём новые Vec3d каждый кадр
    private static final Vec3d[] DIRS = {
        new Vec3d( 1,  0,  0),
        new Vec3d(-1,  0,  0),
        new Vec3d( 0,  1,  0),
        new Vec3d( 0, -1,  0),
        new Vec3d( 0,  0,  1),
        new Vec3d( 0,  0, -1)
    };

    private final List<FallingLine> lines  = new ArrayList<>();
    private final Random            random = new Random();

    // Кэш цветов — обновляем раз в 3 кадра
    private final int[] colorCache = new int[300];
    private int colorCacheFrame = 0;

    private LineGlyphs() {}

    @Override
    public void onEnable() {
        super.onEnable();
        generateLines();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lines.clear();
    }

    private void generateLines() {
        lines.clear();
        if (mc.player == null) return;
        for (int i = 0; i < (int) count.getCurrent(); i++) {
            lines.add(new FallingLine(random, mc.player.getPos(), radius.getCurrent()));
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getPos();

        int targetCount = (int) count.getCurrent();
        float maxRadius = radius.getCurrent();
        int toSpawn = targetCount - lines.size();

        Iterator<FallingLine> it = lines.iterator();
        while (it.hasNext()) {
            FallingLine line = it.next();
            line.update(playerPos, FALL_SPEED, 2f, 5f, 17f);
            if (line.shouldRespawn(playerPos, maxRadius)) {
                it.remove();
                toSpawn++;
            }
        }

        for (int i = 0; i < toSpawn; i++) {
            lines.add(new FallingLine(random, playerPos, maxRadius));
        }

        // Обновляем кэш цветов раз в 3 кадра
        colorCacheFrame++;
        if (colorCacheFrame >= 3) {
            colorCacheFrame = 0;
            int sz = Math.min(lines.size(), colorCache.length);
            for (int i = 0; i < sz; i++) {
                colorCache[i] = computeColor(i);
            }
        }

        renderLines();
    }

    private void renderLines() {
        if (lines.isEmpty()) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        float maxDist = radius.getCurrent() + 10f;
        float maxDistSq = maxDist * maxDist;
        boolean doGlow = glowing.isEnabled();
        float lineWidth = doGlow ? 2.5f : 1.5f;

        for (int li = 0; li < lines.size(); li++) {
            FallingLine line = lines.get(li);
            if (line.points.size() < 2) continue;

            // Проверяем дистанцию один раз по первой точке линии — не для каждого сегмента
            Vec3d head = line.points.peekLast();
            if (head == null) continue;
            double dx = head.x - camPos.x, dy = head.y - camPos.y, dz = head.z - camPos.z;
            if (dx*dx + dy*dy + dz*dz > maxDistSq * 4) continue;

            int baseColor = li < colorCache.length ? colorCache[li] : computeColor(li);
            int pointCount = line.points.size();
            int idx = 0;

            Vec3d prev = null;
            for (Vec3d pt : line.points) {
                if (prev != null) {
                    float segAlpha = (float) idx / pointCount;
                    int segColor = applyAlpha(baseColor, (int)(segAlpha * 255));
                    Render3DUtil.drawLine(prev, pt, segColor, lineWidth, true);
                    if (doGlow) {
                        Render3DUtil.drawLine(prev, pt, applyAlpha(baseColor, (int)(segAlpha * 100)), 4.0f, true);
                    }
                }
                prev = pt;
                idx++;
            }
        }
    }

    private int computeColor(int index) {
        return switch (colorMode.get()) {
            case "Кастом"  -> color1.getColor().getRGB();
            case "Двойной" -> {
                float t = (index % 10) / 10f;
                yield color1.getColor().mix(color2.getColor(), t).getRGB();
            }
            default -> Zenith.getInstance().getThemeManager().getClientColor(index * 10).getRGB();
        };
    }

    private static int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.min(255, Math.max(0, alpha)) << 24);
    }

    // ── Внутренний класс ──────────────────────────────────────────────────────

    private static class FallingLine {
        // ArrayDeque вместо ArrayList — O(1) удаление с обоих концов
        final Deque<Vec3d> points = new ArrayDeque<>();
        Vec3d  currentDirection;
        double distanceTraveled   = 0;
        double currentSegmentLength;
        double totalLength        = 0; // накопленная длина — не пересчитываем каждый кадр

        FallingLine(Random rng, Vec3d playerPos, float maxRadius) {
            double spawnRadius = maxRadius * 0.4 + rng.nextDouble() * maxRadius * 0.6;
            double theta = rng.nextDouble() * 2 * Math.PI;
            double phi   = Math.acos(2 * rng.nextDouble() - 1);

            double x = playerPos.x + spawnRadius * Math.sin(phi) * Math.cos(theta);
            double y = playerPos.y + 1 + rng.nextDouble() * 3 + spawnRadius * Math.cos(phi) * 0.4;
            double z = playerPos.z + spawnRadius * Math.sin(phi) * Math.sin(theta);

            points.add(new Vec3d(x, y, z));
            currentDirection     = DIRS[rng.nextInt(6)];
            currentSegmentLength = 0.5 + rng.nextDouble() * 1.5;
        }

        void update(Vec3d playerPos, float speed, float segLen, float zigzag, float maxLen) {
            if (points.isEmpty()) return;
            Vec3d last = points.peekLast();
            Vec3d next = last.add(currentDirection.multiply(speed));
            points.addLast(next);

            // Обновляем накопленную длину инкрементально
            totalLength += speed;
            distanceTraveled += speed;

            if (distanceTraveled >= currentSegmentLength) {
                // Используем статические направления — без new Vec3d
                currentDirection     = DIRS[(int)(Math.random() * 6)];
                distanceTraveled     = 0;
                currentSegmentLength = 0.5 + Math.random() * 1.5;
            }

            // Обрезаем хвост — O(1) с ArrayDeque
            while (totalLength > maxLen && points.size() > 2) {
                Vec3d first  = points.peekFirst();
                Vec3d second = points.stream().skip(1).findFirst().orElse(first);
                double removed = first.distanceTo(second);
                totalLength -= removed;
                points.pollFirst();
            }
        }

        boolean shouldRespawn(Vec3d playerPos, float maxRadius) {
            if (points.isEmpty()) return true;
            Vec3d last = points.peekLast();
            return last.distanceTo(playerPos) > maxRadius + 5f;
        }
    }
}
