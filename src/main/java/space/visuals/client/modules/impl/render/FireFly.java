package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "FireFly", category = Category.RENDER, description = "Светлячки вокруг игрока")
public final class FireFly extends Module {
    public static final FireFly INSTANCE = new FireFly();

    private final NumberSetting count       = new NumberSetting("Количество", 20f, 10f, 30f, 1f);
    private final BooleanSetting themeColor = new BooleanSetting("Цвет от темы", true);
    private final space.visuals.client.modules.api.setting.impl.ColorSetting customColor =
            new space.visuals.client.modules.api.setting.impl.ColorSetting(
                    "Цвет", new ColorRGBA(255, 215, 0, 255), () -> !themeColor.isEnabled());

    private static final Identifier GLOW_TEX = Zenith.id("textures/bloom.png");
    private static final float SPEED        = 0.35f;
    private static final float SPAWN_RADIUS = 35f;
    private static final int   TRAIL_LENGTH = 70;

    private final List<FireFlyEntity> particles = new ArrayList<>();
    private final Random random = new Random();

    private FireFly() {}

    @Override
    public void onEnable()  { particles.clear(); super.onEnable(); }
    @Override
    public void onDisable() { particles.clear(); super.onDisable(); }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getPos();
        float maxSpeed  = SPEED * 1.5f;

        particles.forEach(p -> p.update(SPEED, maxSpeed, playerPos));
        particles.removeIf(p -> p.isDead(playerPos.x, playerPos.y, playerPos.z));

        int target = (int) count.getCurrent();
        while (particles.size() > target) particles.remove(particles.size() - 1);
        while (particles.size() < target) spawnParticle(playerPos);
    }

    private void spawnParticle(Vec3d playerPos) {
        double distance = random.nextDouble() * (SPAWN_RADIUS - 5) + 5;
        double yawRad   = Math.toRadians(random.nextDouble() * 360);
        double xOffset  = -Math.sin(yawRad) * distance;
        double zOffset  =  Math.cos(yawRad) * distance;
        double yOffset  = (random.nextDouble() - 0.3) * 8 + 1;

        double vYaw   = Math.toRadians(random.nextDouble() * 360);
        double vPitch = Math.toRadians((random.nextDouble() - 0.5) * 60);
        double velX   = -Math.sin(vYaw) * Math.cos(vPitch) * SPEED;
        double velY   =  Math.sin(vPitch) * SPEED * 0.5;
        double velZ   =  Math.cos(vYaw) * Math.cos(vPitch) * SPEED;

        int[] randomColors = {0xFFFFD700, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFFFF69B4, 0xFFFFA500, 0xFF00BFFF};
        int randomColor = randomColors[random.nextInt(randomColors.length)];

        particles.add(new FireFlyEntity(
                playerPos.x + xOffset, playerPos.y + yOffset, playerPos.z + zOffset,
                velX, velY, velZ, randomColor, TRAIL_LENGTH));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        float  tickDelta = event.getPartialTicks();
        Vec3d  cameraPos = mc.gameRenderer.getCamera().getPos();
        Camera camera    = mc.gameRenderer.getCamera();
        MatrixStack stack = event.getMatrix();

        boolean useTheme = themeColor.isEnabled();
        int clrTheme = getThemeColor();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEX);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);
        RenderSystem.enableDepthTest();

        BufferBuilder buffer = null;

        // ── Хвосты ────────────────────────────────────────────────────────────
        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f globalMatrix = stack.peek().getPositionMatrix();

        for (FireFlyEntity particle : particles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f || particle.trail.size() < 2) continue;

            int renderColor = useTheme ? clrTheme : particle.baseRandomColor;
            double px = particle.getInterpolatedX(tickDelta);
            double py = particle.getInterpolatedY(tickDelta);
            double pz = particle.getInterpolatedZ(tickDelta);

            List<Vec3d> points = new ArrayList<>();
            points.add(new Vec3d(px, py, pz));
            for (TrailPoint p : particle.trail) points.add(new Vec3d(p.x, p.y, p.z));

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3d current = points.get(i);
                Vec3d next    = points.get(i + 1);

                float t1 = (float) i / (points.size() - 1);
                float t2 = (float)(i + 1) / (points.size() - 1);
                float w1 = 0.12f * (1f - t1);
                float w2 = 0.12f * (1f - t2);
                float a1 = (1f - t1) * (1f - t1) * lifeAlpha * 0.6f;
                float a2 = (1f - t2) * (1f - t2) * lifeAlpha * 0.6f;
                if (a1 <= 0.01f && a2 <= 0.01f) continue;

                Vec3d dir = current.subtract(next);
                if (dir.lengthSquared() < 0.0001) continue;

                Vec3d cross1 = dir.crossProduct(cameraPos.subtract(current));
                Vec3d cross2 = dir.crossProduct(cameraPos.subtract(next));
                if (cross1.lengthSquared() < 0.0001 || cross2.lengthSquared() < 0.0001) continue;

                Vec3d right1 = cross1.normalize().multiply(w1);
                Vec3d right2 = cross2.normalize().multiply(w2);

                int c1 = setAlpha(renderColor, (int)(a1 * 255));
                int c2 = setAlpha(renderColor, (int)(a2 * 255));

                if (buffer == null) buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                buffer.vertex(globalMatrix, (float)(current.x+right1.x), (float)(current.y+right1.y), (float)(current.z+right1.z)).texture(0, t1).color(c1);
                buffer.vertex(globalMatrix, (float)(current.x-right1.x), (float)(current.y-right1.y), (float)(current.z-right1.z)).texture(1, t1).color(c1);
                buffer.vertex(globalMatrix, (float)(next.x-right2.x),    (float)(next.y-right2.y),    (float)(next.z-right2.z)   ).texture(1, t2).color(c2);
                buffer.vertex(globalMatrix, (float)(next.x+right2.x),    (float)(next.y+right2.y),    (float)(next.z+right2.z)   ).texture(0, t2).color(c2);
            }
        }
        stack.pop();

        // ── Головы (billboard glow) ───────────────────────────────────────────
        for (FireFlyEntity particle : particles) {
            float lifeAlpha = particle.getLifeAlpha();
            if (lifeAlpha <= 0.01f) continue;

            float pulseFloat = particle.getPulseAlpha() / 255f;
            float finalAlpha = pulseFloat * lifeAlpha;
            if (finalAlpha <= 0.01f) continue;

            int renderColor = useTheme ? clrTheme : particle.baseRandomColor;
            double px = particle.getInterpolatedX(tickDelta);
            double py = particle.getInterpolatedY(tickDelta);
            double pz = particle.getInterpolatedZ(tickDelta);

            stack.push();
            stack.translate(px - cameraPos.x, py - cameraPos.y, pz - cameraPos.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f localMatrix = stack.peek().getPositionMatrix();

            if (buffer == null) buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            drawQuad(buffer, localMatrix, 0.35f, renderColor,   finalAlpha * 0.6f);
            drawQuad(buffer, localMatrix, 0.22f, renderColor,   finalAlpha);
            drawQuad(buffer, localMatrix, 0.10f, 0xFFFFFFFF,    finalAlpha);

            stack.pop();
        }

        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
    }

    private void drawQuad(BufferBuilder buf, Matrix4f m, float size, int color, float alphaMod) {
        if (alphaMod <= 0.01f) return;
        int c = setAlpha(color, (int)(alphaMod * 255));
        buf.vertex(m, -size, -size, 0).texture(0, 0).color(c);
        buf.vertex(m, -size,  size, 0).texture(0, 1).color(c);
        buf.vertex(m,  size,  size, 0).texture(1, 1).color(c);
        buf.vertex(m,  size, -size, 0).texture(1, 0).color(c);
    }

    private int getThemeColor() {
        if (!themeColor.isEnabled()) {
            ColorRGBA c = customColor.getColor();
            return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }
        ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private static int setAlpha(int color, int a) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    private static class TrailPoint {
        double x, y, z;
        TrailPoint(double x, double y, double z) { this.x=x; this.y=y; this.z=z; }
    }

    private static class FireFlyEntity {
        double x, y, z, prevX, prevY, prevZ;
        double velX, velY, velZ;
        double targetVelX, targetVelY, targetVelZ;
        final int baseRandomColor;
        final long spawnTime;
        long lastDirectionChange;
        final List<TrailPoint> trail = new ArrayList<>();
        final int maxTrailLength;
        final Random random = new Random();

        FireFlyEntity(double x, double y, double z,
                      double velX, double velY, double velZ,
                      int baseRandomColor, int maxTrailLength) {
            this.x=x; this.y=y; this.z=z;
            this.prevX=x; this.prevY=y; this.prevZ=z;
            this.velX=velX; this.velY=velY; this.velZ=velZ;
            this.targetVelX=velX; this.targetVelY=velY; this.targetVelZ=velZ;
            this.baseRandomColor=baseRandomColor;
            this.maxTrailLength=maxTrailLength;
            this.spawnTime=System.currentTimeMillis();
            this.lastDirectionChange=System.currentTimeMillis();
        }

        void update(float speedMult, float maxSpeed, Vec3d playerPos) {
            prevX=x; prevY=y; prevZ=z;

            if (System.currentTimeMillis() - lastDirectionChange > 2000 + random.nextInt(2000)) {
                double angle = Math.toRadians(random.nextDouble() * 360);
                double pitch = Math.toRadians((random.nextDouble() - 0.5) * 40);
                targetVelX = -Math.sin(angle) * Math.cos(pitch) * speedMult;
                targetVelY =  Math.sin(pitch) * speedMult * 0.3;
                targetVelZ =  Math.cos(angle) * Math.cos(pitch) * speedMult;
                lastDirectionChange = System.currentTimeMillis();
            }

            double dx = playerPos.x - x, dy = playerPos.y + 1.0 - y, dz = playerPos.z - z;
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist > SPAWN_RADIUS) {
                targetVelX += (dx/dist) * speedMult * 0.15;
                targetVelY += (dy/dist) * speedMult * 0.15;
                targetVelZ += (dz/dist) * speedMult * 0.15;
            }

            double lf = 0.02;
            velX += (targetVelX - velX) * lf;
            velY += (targetVelY - velY) * lf;
            velZ += (targetVelZ - velZ) * lf;

            double w = 0.03;
            velX += (random.nextDouble()-0.5)*w;
            velY += (random.nextDouble()-0.5)*w;
            velZ += (random.nextDouble()-0.5)*w;

            velX = MathHelper.clamp(velX, -maxSpeed, maxSpeed);
            velY = MathHelper.clamp(velY, -maxSpeed, maxSpeed);
            velZ = MathHelper.clamp(velZ, -maxSpeed, maxSpeed);

            x+=velX; y+=velY; z+=velZ;
            trail.add(0, new TrailPoint(x, y, z));
            while (trail.size() > maxTrailLength) trail.remove(trail.size()-1);
        }

        boolean isDead(double px, double py, double pz) {
            double dx=x-px, dy=y-py, dz=z-pz;
            return dx*dx+dy*dy+dz*dz > 80*80;
        }

        double getInterpolatedX(float td) { return MathHelper.lerp(td, prevX, x); }
        double getInterpolatedY(float td) { return MathHelper.lerp(td, prevY, y); }
        double getInterpolatedZ(float td) { return MathHelper.lerp(td, prevZ, z); }

        int getPulseAlpha() {
            double pulse = 0.8 + 0.2 * Math.sin((System.currentTimeMillis() - spawnTime) / 200.0);
            return (int)(pulse * 255);
        }

        float getLifeAlpha() {
            long age = System.currentTimeMillis() - spawnTime;
            return age < 1000 ? age / 1000f : 1f;
        }
    }
}
