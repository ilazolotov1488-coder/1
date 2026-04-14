package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventAttack;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "Particles", category = Category.RENDER, description = "Частицы вокруг игрока")
public final class ParticlesModule extends Module {
    public static final ParticlesModule INSTANCE = new ParticlesModule();

    private final ModeSetting type = new ModeSetting("Режим", "Звёздочки", "Сердечки", "Звёздочки", "Сияние");
    private final BooleanSetting onIdle    = new BooleanSetting("При бездействии", false);
    private final BooleanSetting onRun     = new BooleanSetting("При беге", false);
    private final BooleanSetting onHit     = new BooleanSetting("При ударе", true);
    private final BooleanSetting onPearl   = new BooleanSetting("При перле", true);
    private final BooleanSetting onTrident = new BooleanSetting("При трезубце", true);
    private final BooleanSetting onArrow   = new BooleanSetting("При стреле", true);
    private final NumberSetting idleCount  = new NumberSetting("Кол-во (бездействие)", 10f, 2f, 40f, 1f,
            () -> onIdle.isEnabled());

    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    private ParticlesModule() {}

    // ── Events ────────────────────────────────────────────────────────────────

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        particles.removeIf(p -> p.isDead());
        if (particles.isEmpty()) return;

        Camera cam = mc.gameRenderer.getCamera();
        Quaternionf rot = new Quaternionf(cam.getRotation());

        Vector3f right   = new Vector3f(1, 0, 0).rotate(rot);
        Vector3f up      = new Vector3f(0, 1, 0).rotate(rot);
        Vector3f forward = new Vector3f(0, 0, 1).rotate(rot);

        Vec3d camPos = cam.getPos();
        Vec3d fwd = new Vec3d(forward.x, forward.y, forward.z);

        MatrixStack ms = event.getMatrix();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();

        space.visuals.utility.render.display.base.color.ColorRGBA theme = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();

        for (Particle p : particles) {
            p.update();

            Vec3d toCenter = p.pos.subtract(camPos);
            if (fwd.dotProduct(toCenter) <= 0) continue;

            double half = p.size * 0.5;
            Vec3d hr = new Vec3d(right.x, right.y, right.z).multiply(half);
            Vec3d hu = new Vec3d(up.x, up.y, up.z).multiply(half);

            Vec3d p0 = toCenter.subtract(hr).subtract(hu);
            Vec3d p1 = toCenter.add(hr).subtract(hu);
            Vec3d p2 = toCenter.add(hr).add(hu);
            Vec3d p3 = toCenter.subtract(hr).add(hu);

            int alpha = (int)(p.alpha * 255) & 0xFF;
            int color = (theme.getRGB() & 0x00FFFFFF) | (alpha << 24);

            buf.vertex(m, (float)p0.x, (float)p0.y, (float)p0.z).color(color);
            buf.vertex(m, (float)p1.x, (float)p1.y, (float)p1.z).color(color);
            buf.vertex(m, (float)p2.x, (float)p2.y, (float)p2.z).color(color);
            buf.vertex(m, (float)p3.x, (float)p3.y, (float)p3.z).color(color);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        // При беге
        if (onRun.isEnabled() && isMoving()) {
            double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x
                    + mc.player.getVelocity().z * mc.player.getVelocity().z);
            if (speed > 0.05) {
                double dirX = -mc.player.getVelocity().x / speed;
                double dirZ = -mc.player.getVelocity().z / speed;
                double dist = 0.5 + speed * 1.5;
                double px = mc.player.getX() + dirX * dist + rng(-0.35, 0.35);
                double py = mc.player.getY() + rng(0.2, mc.player.getHeight() + 0.1);
                double pz = mc.player.getZ() + dirZ * dist + rng(-0.35, 0.35);
                if (!inBlock(px, py, pz)) {
                    spawnParticle(px, py, pz,
                            new Vec3d(dirX * 0.0075, rng(-0.005, 0.001), dirZ * 0.0075),
                            0.3f, rng(1500, 2000));
                }
            }
        }

        // При бездействии
        if (onIdle.isEnabled()) {
            Vec3d base = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getHeight() / 2.0, mc.player.getZ());
            int cnt = (int) idleCount.getCurrent();
            for (int i = 0; i < cnt; i++) {
                double dist = rng(7, 35);
                double angle = Math.toRadians(rng(0, 360));
                double height = rng(-7, 25);
                Vec3d spawn = base.add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                if (inBlock(spawn.x, spawn.y, spawn.z)) continue;
                double phi = Math.toRadians(rng(0, 360));
                double spd = rng(0.015, 0.03);
                spawnParticle(spawn.x, spawn.y, spawn.z,
                        new Vec3d(Math.cos(phi) * spd, rng(-spd * 0.1, spd * 0.1), Math.sin(phi) * spd),
                        0.3f, rng(1500, 2000));
            }
        }

        // Перлы, трезубцы, стрелы
        for (Entity entity : mc.world.getEntities()) {
            if (onPearl.isEnabled() && entity instanceof EnderPearlEntity pearl) {
                spawnTrail(pearl.getPos(), 1);
            }
            if (onTrident.isEnabled() && entity instanceof TridentEntity trident) {
                spawnTrail(trident.getPos(), 1);
            }
            if (onArrow.isEnabled() && entity instanceof ArrowEntity arrow) {
                spawnTrail(arrow.getPos(), 1);
            }
        }
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (!onHit.isEnabled()) return;
        if (event.getAction() != EventAttack.Action.PRE) return;
        if (mc.player == null || mc.world == null) return;
        Entity target = event.getTarget();
        if (target == null) return;

        for (int i = 0; i < 35; i++) {
            double tx = target.getX() + rng(-0.4, 0.4);
            double ty = target.getY() + rng(-0.4, target.getHeight() + 0.4);
            double tz = target.getZ() + rng(-0.4, 0.4);
            if (inBlock(tx, ty, tz)) continue;
            spawnParticle(tx, ty, tz,
                    new Vec3d(rng(-0.8, 0.8) * 0.075, rng(-0.25, 1.4) * 0.075, rng(-0.8, 0.8) * 0.075),
                    0.3f, rng(1000, 1200));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void spawnTrail(Vec3d pos, int count) {
        for (int i = 0; i < count * 2; i++) {
            double angle = Math.toRadians(rng(0, 360));
            double spd = rng(0.015, 0.075);
            Vec3d spawn = pos.add(Math.cos(angle) * 0, rng(0.1, 0.35), Math.sin(angle) * 0);
            if (inBlock(spawn.x, spawn.y, spawn.z)) continue;
            spawnParticle(spawn.x, spawn.y, spawn.z,
                    new Vec3d(Math.cos(angle) * spd, rng(-spd * 0.4, spd * 0.4), Math.sin(angle) * spd),
                    0.25f, rng(2400, 2800));
        }
    }

    private void spawnParticle(double x, double y, double z, Vec3d vel, float size, double life) {
        particles.add(new Particle(new Vec3d(x, y, z), vel, size, (long) life));
    }

    private boolean inBlock(double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos bp = BlockPos.ofFloored(x, y, z);
        return mc.world.getBlockState(bp).isSolidBlock(mc.world, bp);
    }

    private boolean isMoving() {
        Vec3d v = mc.player.getVelocity();
        return Math.abs(v.x) > 0.01 || Math.abs(v.z) > 0.01;
    }

    private double rng(double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }

    @Override
    public void onDisable() {
        particles.clear();
        super.onDisable();
    }

    // ── Particle ──────────────────────────────────────────────────────────────

    private static class Particle {
        Vec3d pos;
        Vec3d vel;
        float size;
        long lifeTime;
        float alpha = 1f;
        long born = System.currentTimeMillis();
        long lastNs = System.nanoTime();
        static final double GRAVITY = 0.00005;

        Particle(Vec3d pos, Vec3d vel, float size, long lifeTime) {
            this.pos = pos; this.vel = vel; this.size = size; this.lifeTime = lifeTime;
        }

        boolean isDead() { return System.currentTimeMillis() - born >= lifeTime; }

        void update() {
            long nowNs = System.nanoTime();
            double dt = (nowNs - lastNs) / 1_000_000_000.0;
            lastNs = nowNs;

            float progress = Math.min(1f, (float)(System.currentTimeMillis() - born) / lifeTime);
            double factor = Math.pow(1.0 - progress, 3.0);

            double nx = pos.x + vel.x * factor * (dt * 60);
            double ny = pos.y + vel.y * factor * (dt * 60);
            double nz = pos.z + vel.z * factor * (dt * 60);

            pos = new Vec3d(nx, ny, nz);
            vel = new Vec3d(vel.x * 0.9999, vel.y * 0.9999 - GRAVITY, vel.z * 0.9999);
            alpha = 1f - progress;
        }
    }

}
