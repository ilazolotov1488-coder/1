package space.visuals.client.screens;

import net.minecraft.client.gui.DrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.Random;

/**
 * Космические эффекты: звёзды + кометы.
 * Движение основано на реальном времени (ms) — плавное при любом FPS.
 */
public class SpaceEffects {

    // ── Звёзды ────────────────────────────────────────────────────────────────
    private static final int STARS = 220;
    private final float[] sx   = new float[STARS];
    private final float[] sy   = new float[STARS];
    private final float[] ssz  = new float[STARS];
    private final float[] sspd = new float[STARS]; // пикселей в секунду (нормализованных)
    private final float[] sph  = new float[STARS];
    private final float[] salp = new float[STARS];

    // ── Кометы ────────────────────────────────────────────────────────────────
    private static final int COMETS = 15;
    private final float[] cx    = new float[COMETS];
    private final float[] cy    = new float[COMETS];
    private final float[] cspd  = new float[COMETS];
    private final float[] clen  = new float[COMETS];
    private final float[] calp  = new float[COMETS];
    private final float[] cang  = new float[COMETS];
    private final int[]   cdir  = new int[COMETS]; // 0=сверху, 1=слева, 2=справа, 3=снизу
    private final boolean[] cact = new boolean[COMETS];
    private long lastSpawn = 0;
    private int nextComet  = 0;

    private final Random rng;
    private boolean ready   = false;
    private long    lastMs  = 0; // для delta time

    public SpaceEffects() {
        rng = new Random(System.nanoTime());
    }

    private void init() {
        for (int i = 0; i < STARS; i++) {
            sx[i]   = rng.nextFloat();
            sy[i]   = rng.nextFloat();
            ssz[i]  = 0.7f + rng.nextFloat() * 1.5f;
            sspd[i] = 0.004f + rng.nextFloat() * 0.010f; // очень медленно
            sph[i]  = rng.nextFloat() * (float)(Math.PI * 2);
            salp[i] = 0.3f + rng.nextFloat() * 0.7f;
        }
        for (int i = 0; i < COMETS; i++) {
            cact[i] = false;
        }
        ready  = true;
        lastMs = System.currentTimeMillis();
    }

    private void spawnComet() {
        int i = nextComet % COMETS;
        nextComet++;
        calp[i] = 0f;
        cact[i] = true;
        cspd[i] = 0.08f + rng.nextFloat() * 0.10f;
        clen[i] = 55f + rng.nextFloat() * 95f;

        // Случайное направление со всех сторон
        int dir = rng.nextInt(4);
        cdir[i] = dir;
        switch (dir) {
            case 0 -> { // сверху → вниз-вправо
                cx[i] = 0.05f + rng.nextFloat() * 0.9f;
                cy[i] = -0.05f;
                cang[i] = (float)Math.toRadians(16 + rng.nextFloat() * 16);
            }
            case 1 -> { // слева → вправо
                cx[i] = -0.05f;
                cy[i] = 0.05f + rng.nextFloat() * 0.6f;
                cang[i] = (float)Math.toRadians(-(5 + rng.nextFloat() * 15));
            }
            case 2 -> { // справа → влево
                cx[i] = 1.05f;
                cy[i] = 0.05f + rng.nextFloat() * 0.6f;
                cang[i] = (float)Math.toRadians(180 + 5 + rng.nextFloat() * 15);
            }
            default -> { // снизу → вверх
                cx[i] = 0.05f + rng.nextFloat() * 0.9f;
                cy[i] = 1.05f;
                cang[i] = (float)Math.toRadians(180 + 16 + rng.nextFloat() * 16);
            }
        }
    }

    /** Вызывать каждый кадр. */
    public void tick(long now) {
        if (!ready) { init(); return; }

        float dt = Math.min((now - lastMs) / 1000f, 0.1f); // секунды, макс 100мс
        lastMs = now;

        // Звёзды
        for (int i = 0; i < STARS; i++) {
            sy[i] += sspd[i] * dt;
            if (sy[i] > 1.02f) {
                sy[i] = -0.02f;
                sx[i] = rng.nextFloat();
            }
        }

        // Кометы — спавн каждые ~1.1с
        if (now - lastSpawn > 1100) {
            spawnComet();
            lastSpawn = now;
        }
        for (int i = 0; i < COMETS; i++) {
            if (!cact[i] && calp[i] <= 0f) continue;
            float cosA = (float)Math.cos(cang[i]);
            float sinA = (float)Math.sin(cang[i]);
            cx[i] += cspd[i] * cosA * dt;
            cy[i] += cspd[i] * sinA * dt;
            // Плавное появление
            if (cact[i] && calp[i] < 1f) calp[i] = Math.min(1f, calp[i] + 3f * dt);
            // Исчезновение когда вышла за экран
            boolean outOfBounds = cx[i] > 1.1f || cx[i] < -0.1f || cy[i] > 1.1f || cy[i] < -0.1f;
            if (outOfBounds) {
                cact[i] = false;
                calp[i] = Math.max(0f, calp[i] - 4f * dt);
            }
        }
    }

    /** Рисует эффекты. alpha — fade-in экрана. */
    public void render(DrawContext ctx, int W, int H, float alpha, long now) {
        render(ctx, W, H, alpha, now, 0, 0);
    }

    /** Рисует эффекты с offset (для рендера внутри бокса). */
    public void render(DrawContext ctx, int W, int H, float alpha, long now, int offX, int offY) {
        if (!ready) return;

        // Звёзды
        for (int i = 0; i < STARS; i++) {
            float twinkle = (float)(Math.sin(now * 0.0012 + sph[i]) * 0.3 + 0.7);
            int a = (int)(salp[i] * twinkle * alpha * 215);
            if (a <= 2) continue;
            int px = offX + (int)(sx[i] * W);
            int py = offY + (int)(sy[i] * H);
            int sz = Math.max(1, (int)ssz[i]);
            ctx.fill(px, py, px + sz, py + sz, new ColorRGBA(205, 218, 255, a).getRGB());
        }

        // Кометы
        for (int i = 0; i < COMETS; i++) {
            float ca = calp[i];
            if (ca <= 0.01f) continue;
            float headX = offX + cx[i] * W;
            float headY = offY + cy[i] * H;
            // Хвост в обратном направлении движения
            float cosA = -(float)Math.cos(cang[i]);
            float sinA = -(float)Math.sin(cang[i]);
            int segs = 20;
            for (int s = 0; s < segs; s++) {
                float t  = (float)s / segs;
                float px = headX + cosA * clen[i] * t;
                float py = headY + sinA * clen[i] * t;
                float fade = (1f - t) * (1f - t);
                int sa = (int)(fade * ca * alpha * 240);
                if (sa <= 2) continue;
                int thick = s < 2 ? 2 : 1;
                ctx.fill((int)px, (int)py, (int)px + thick + 1, (int)py + 1,
                    new ColorRGBA(228, 238, 255, sa).getRGB());
            }
        }
    }
}
