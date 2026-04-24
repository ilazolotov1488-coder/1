package space.visuals.client.screens;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.Random;

/**
 * Космические эффекты: звёзды (текстура star.png) + кометы.
 * Движение основано на реальном времени (ms) — плавное при любом FPS.
 */
public class SpaceEffects {

    private static final Identifier STAR_TEX = Identifier.of("space", "hud/particles/star.png");

    // ── Звёзды ────────────────────────────────────────────────────────────────
    private static final int STARS = 220;
    private final float[] sx   = new float[STARS];
    private final float[] sy   = new float[STARS];
    private final float[] ssz  = new float[STARS]; // размер в пикселях
    private final float[] sspd = new float[STARS];
    private final float[] sph  = new float[STARS];
    private final float[] salp = new float[STARS];
    private final float[] srot = new float[STARS]; // угол поворота

    // ── Кометы ────────────────────────────────────────────────────────────────
    private static final int COMETS = 15;
    private final float[] cx    = new float[COMETS];
    private final float[] cy    = new float[COMETS];
    private final float[] cspd  = new float[COMETS];
    private final float[] clen  = new float[COMETS];
    private final float[] calp  = new float[COMETS];
    private final float[] cang  = new float[COMETS];
    private final int[]   cdir  = new int[COMETS];
    private final boolean[] cact = new boolean[COMETS];
    private long lastSpawn = 0;
    private int nextComet  = 0;

    private final Random rng;
    private boolean ready   = false;
    private long    lastMs  = 0;

    public SpaceEffects() {
        rng = new Random(System.nanoTime());
    }

    private void init() {
        for (int i = 0; i < STARS; i++) {
            sx[i]   = rng.nextFloat();
            sy[i]   = rng.nextFloat();
            // Чуть меньше чем в модуле HitParticles (там size=0.2f в мировых единицах)
            ssz[i]  = 2.5f + rng.nextFloat() * 3.5f; // 2.5–6 пикселей
            sspd[i] = 0.004f + rng.nextFloat() * 0.010f;
            sph[i]  = rng.nextFloat() * (float)(Math.PI * 2);
            salp[i] = 0.3f + rng.nextFloat() * 0.7f;
            srot[i] = rng.nextFloat() * 360f;
        }
        for (int i = 0; i < COMETS; i++) cact[i] = false;
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
        int dir = rng.nextInt(4);
        cdir[i] = dir;
        switch (dir) {
            case 0 -> { cx[i] = 0.05f + rng.nextFloat() * 0.9f; cy[i] = -0.05f; cang[i] = (float)Math.toRadians(16 + rng.nextFloat() * 16); }
            case 1 -> { cx[i] = -0.05f; cy[i] = 0.05f + rng.nextFloat() * 0.6f; cang[i] = (float)Math.toRadians(-(5 + rng.nextFloat() * 15)); }
            case 2 -> { cx[i] = 1.05f; cy[i] = 0.05f + rng.nextFloat() * 0.6f; cang[i] = (float)Math.toRadians(180 + 5 + rng.nextFloat() * 15); }
            default -> { cx[i] = 0.05f + rng.nextFloat() * 0.9f; cy[i] = 1.05f; cang[i] = (float)Math.toRadians(180 + 16 + rng.nextFloat() * 16); }
        }
    }

    public void tick(long now) {
        if (!ready) { init(); return; }
        float dt = Math.min((now - lastMs) / 1000f, 0.1f);
        lastMs = now;
        for (int i = 0; i < STARS; i++) {
            sy[i] += sspd[i] * dt;
            srot[i] += 15f * dt; // медленное вращение
            if (sy[i] > 1.02f) { sy[i] = -0.02f; sx[i] = rng.nextFloat(); }
        }
        if (now - lastSpawn > 1100) { spawnComet(); lastSpawn = now; }
        for (int i = 0; i < COMETS; i++) {
            if (!cact[i] && calp[i] <= 0f) continue;
            float cosA = (float)Math.cos(cang[i]);
            float sinA = (float)Math.sin(cang[i]);
            cx[i] += cspd[i] * cosA * dt;
            cy[i] += cspd[i] * sinA * dt;
            if (cact[i] && calp[i] < 1f) calp[i] = Math.min(1f, calp[i] + 3f * dt);
            boolean out = cx[i] > 1.1f || cx[i] < -0.1f || cy[i] > 1.1f || cy[i] < -0.1f;
            if (out) { cact[i] = false; calp[i] = Math.max(0f, calp[i] - 4f * dt); }
        }
    }

    public void render(DrawContext ctx, int W, int H, float alpha, long now) {
        render(ctx, W, H, alpha, now, 0, 0);
    }

    public void render(DrawContext ctx, int W, int H, float alpha, long now, int offX, int offY) {
        if (!ready) return;

        // Звёзды — рендерим батчем через текстуру star.png
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, STAR_TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasVerts = false;

        for (int i = 0; i < STARS; i++) {
            float twinkle = (float)(Math.sin(now * 0.0012 + sph[i]) * 0.3 + 0.7);
            float a = salp[i] * twinkle * alpha;
            if (a <= 0.01f) continue;

            float px = offX + sx[i] * W;
            float py = offY + sy[i] * H;
            float half = ssz[i] / 2f;
            float rot  = (float)Math.toRadians(srot[i]);
            float cosR = (float)Math.cos(rot);
            float sinR = (float)Math.sin(rot);

            // Четыре угла с поворотом вокруг центра
            float[] vx = { -half, half, half, -half };
            float[] vy = { -half, -half, half, half };
            float[] u  = { 0, 1, 1, 0 };
            float[] v  = { 0, 0, 1, 1 };

            // Цвет звезды — белый с лёгким голубым оттенком
            int r = 220, g = 225, b = 255;
            int ai = (int)(a * 200);

            for (int k = 0; k < 4; k++) {
                float rx = vx[k] * cosR - vy[k] * sinR + px;
                float ry = vx[k] * sinR + vy[k] * cosR + py;
                buf.vertex(rx, ry, 0).texture(u[k], v[k]).color(r, g, b, ai);
            }
            hasVerts = true;
        }

        if (hasVerts) {
            BuiltBuffer built = buf.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        } else {
            buf.endNullable();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // Летящие звёзды с хвостом
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        for (int i = 0; i < COMETS; i++) {
            float ca = calp[i];
            if (ca <= 0.01f) continue;

            float headX = offX + cx[i] * W;
            float headY = offY + cy[i] * H;
            // Направление хвоста — обратное движению
            float tailDirX = -(float)Math.cos(cang[i]);
            float tailDirY = -(float)Math.sin(cang[i]);
            float tailLen  = clen[i];

            // ── Хвост — градиентная лента ──────────────────────────────────
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder tailBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            boolean hasTail = false;

            // Перпендикуляр к направлению хвоста
            float perpX = -tailDirY;
            float perpY =  tailDirX;
            int tailSegs = 24;
            for (int s = 0; s <= tailSegs; s++) {
                float t     = (float) s / tailSegs;
                float tx    = headX + tailDirX * tailLen * t;
                float ty    = headY + tailDirY * tailLen * t;
                float width = (1f - t) * 2.5f; // сужается к концу
                float fade  = (1f - t) * (1f - t) * ca * alpha;
                int   sa    = (int)(fade * 200);
                if (sa < 2) sa = 0;
                // Цвет: голубовато-белый у головы → фиолетовый к хвосту
                int r = (int)(220 - t * 80);
                int g = (int)(225 - t * 100);
                int b = 255;
                tailBuf.vertex(tx + perpX * width, ty + perpY * width, 0).color(r, g, b, sa);
                tailBuf.vertex(tx - perpX * width, ty - perpY * width, 0).color(r, g, b, sa);
                hasTail = true;
            }
            if (hasTail) {
                BuiltBuffer built = tailBuf.endNullable();
                if (built != null) BufferRenderer.drawWithGlobalProgram(built);
            } else {
                tailBuf.endNullable();
            }

            // ── Голова — текстура star.png ──────────────────────────────────
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, STAR_TEX);
            float starSize = 5f + ca * 3f;
            float half = starSize / 2f;
            // Поворот по направлению движения
            float rot  = cang[i];
            float cosR = (float)Math.cos(rot);
            float sinR = (float)Math.sin(rot);
            int sa = (int)(ca * alpha * 230);

            BufferBuilder starBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float[] vx2 = { -half,  half, half, -half };
            float[] vy2 = { -half, -half, half,  half };
            float[] u2  = { 0, 1, 1, 0 };
            float[] v2  = { 0, 0, 1, 1 };
            for (int k = 0; k < 4; k++) {
                float rx = vx2[k] * cosR - vy2[k] * sinR + headX;
                float ry = vx2[k] * sinR + vy2[k] * cosR + headY;
                starBuf.vertex(rx, ry, 0).texture(u2[k], v2[k]).color(220, 230, 255, sa);
            }
            BuiltBuffer built = starBuf.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
