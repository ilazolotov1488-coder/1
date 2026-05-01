package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import com.adl.nativeprotect.Native;
import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "SoulESP", category = Category.RENDER, description = "Показывает душу игрока после смерти")
public final class SoulESP extends Module {
    public static final SoulESP INSTANCE = new SoulESP();

    private static final float DURATION = 3.0f;
    private static final float HEIGHT   = 3.5f;

    // "След" — только для себя при приземлении
    private final BooleanSetting trail = new BooleanSetting("След", false);

    private final List<Ghost> ghosts = new ArrayList<>();

    // Для отслеживания приземления себя
    private boolean wasOnGround = false;
    // Трейл-призраки (только свои, при приземлении)
    private final List<Ghost> trailGhosts = new ArrayList<>();
    private static final float TRAIL_DURATION = 1.2f;

    private SoulESP() {}

    // ── Смерть игрока → создаём душу ─────────────────────────────────────────

    @Native
    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (mc.player == null || mc.world == null) return;

        if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
            byte status = packet.getStatus();
            if (status != 3 && status != 35) return; // смерть (3) или тотем (35)

            Entity entity = packet.getEntity(mc.world);
            if (!(entity instanceof PlayerEntity player)) return;

            ghosts.add(ghostFromPlayer(player, System.currentTimeMillis()));
        }
    }

    // ── Приземление → трейл-призрак (только себя) ────────────────────────────

    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || !trail.isEnabled()) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        boolean onGround = mc.player.isOnGround();
        // Момент приземления: был в воздухе → стал на земле
        if (!wasOnGround && onGround) {
            trailGhosts.add(ghostFromPlayer(mc.player, System.currentTimeMillis()));
        }
        wasOnGround = onGround;
    }

    // ── Рендер ────────────────────────────────────────────────────────────────

    @Native
    @EventTarget
    public void onRender3D(EventRender3D e) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float dur      = DURATION * 1000f;
        float trailDur = TRAIL_DURATION * 1000f;

        ghosts.removeIf(g -> (now - g.time) >= dur);
        trailGhosts.removeIf(g -> (now - g.time) >= trailDur);

        if (ghosts.isEmpty() && trailGhosts.isEmpty()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        MatrixStack m = e.getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Обычные души (смерть игроков)
        for (Ghost g : ghosts) {
            float t = (now - g.time) / dur;
            if (t >= 1f) continue;
            float alpha = (1f - t) * 0.6f;
            float rise  = HEIGHT * ease(t);
            renderGhost(m, cam, g, alpha, rise);
        }

        // Трейл-призраки (приземление себя)
        for (Ghost g : trailGhosts) {
            float t = (now - g.time) / trailDur;
            if (t >= 1f) continue;
            float alpha = (1f - t) * 0.45f;
            renderGhost(m, cam, g, alpha, 0f); // не поднимаются
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ── Рендер одного призрака ────────────────────────────────────────────────

    @Native
    private void renderGhost(MatrixStack m, Vec3d cam, Ghost g, float alpha, float rise) {
        ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        float r  = c.getRed()   / 255f;
        float gr = c.getGreen() / 255f;
        float b  = c.getBlue()  / 255f;

        float u     = 1f / 16f;
        float swing = MathHelper.sin(g.limbAngle * 0.6662f) * 0.6f * g.limbSpeed;

        m.push();
        m.translate(g.pos.x - cam.x, g.pos.y - cam.y + rise, g.pos.z - cam.z);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - g.yaw));
        m.scale(-1f, -1f, 1f);
        m.translate(0, -1.5, 0);

        // Приседание
        if (g.sneak) {
            m.translate(0, 0.2, 0);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28f));
        }

        // Наклон головы по pitch
        float headPitch = g.pitch;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Голова (с поворотом по pitch)
        m.push();
        m.translate(0, 0, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
        box(buf, m.peek().getPositionMatrix(), -4*u, -8*u, -4*u, 8*u, 8*u, 8*u, r, gr, b, alpha);
        m.pop();

        // Тело
        box(buf, m.peek().getPositionMatrix(), -4*u, 0, -2*u, 8*u, 12*u, 4*u, r, gr, b, alpha);

        // Левая рука (с анимацией)
        m.push();
        m.translate(-6*u, 2*u, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
        // Поднятие руки если держит предмет
        if (!g.mainHand.isEmpty()) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f));
        m.translate(6*u, -2*u, 0);
        box(buf, m.peek().getPositionMatrix(), -8*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
        m.pop();

        // Правая рука
        m.push();
        m.translate(6*u, 2*u, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        if (!g.offHand.isEmpty()) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f));
        m.translate(-6*u, -2*u, 0);
        box(buf, m.peek().getPositionMatrix(), 4*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
        m.pop();

        // Левая нога
        m.push();
        m.translate(-2*u, 12*u, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
        m.translate(2*u, -12*u, 0);
        box(buf, m.peek().getPositionMatrix(), -4*u, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
        m.pop();

        // Правая нога
        m.push();
        m.translate(2*u, 12*u, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
        m.translate(-2*u, -12*u, 0);
        box(buf, m.peek().getPositionMatrix(), 0, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
        m.pop();

        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Предметы в руках (рендерим через DrawContext поверх)
        renderHandItems(m, cam, g, alpha);

        m.pop();
    }

    @Native
    private void renderHandItems(MatrixStack m, Vec3d cam, Ghost g, float alpha) {
        // Рисуем иконки предметов в руках как 2D billboard
        if (g.mainHand.isEmpty() && g.offHand.isEmpty()) return;

        float u = 1f / 16f;

        // Основная рука — правая сторона (x = +6u)
        if (!g.mainHand.isEmpty()) {
            m.push();
            m.translate(8*u, 4*u, -3*u);
            m.multiply(mc.gameRenderer.getCamera().getRotation());
            m.scale(0.4f, 0.4f, 0.4f);
            // Рисуем как цветной квад (упрощённо — без текстуры предмета)
            drawItemIndicator(m, alpha);
            m.pop();
        }

        // Оффхенд — левая сторона
        if (!g.offHand.isEmpty()) {
            m.push();
            m.translate(-12*u, 4*u, -3*u);
            m.multiply(mc.gameRenderer.getCamera().getRotation());
            m.scale(0.4f, 0.4f, 0.4f);
            drawItemIndicator(m, alpha);
            m.pop();
        }
    }

    @Native
    private void drawItemIndicator(MatrixStack m, float alpha) {
        ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        float r = c.getRed()/255f, g = c.getGreen()/255f, b = c.getBlue()/255f;
        float s = 0.5f;
        Matrix4f mat = m.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, -s, -s, 0).color(r, g, b, alpha * 0.8f);
        buf.vertex(mat,  s, -s, 0).color(r, g, b, alpha * 0.8f);
        buf.vertex(mat,  s,  s, 0).color(r, g, b, alpha * 0.8f);
        buf.vertex(mat, -s,  s, 0).color(r, g, b, alpha * 0.8f);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Native
    public void spawnGhostAt(Vec3d pos, float yaw, float pitch) {
        ghosts.add(new Ghost(pos, yaw, pitch, false, 0f, 0f,
                ItemStack.EMPTY, ItemStack.EMPTY, System.currentTimeMillis()));
    }

    @Native
    private Ghost ghostFromPlayer(PlayerEntity player, long time) {
        return new Ghost(
                player.getPos(),
                player.getBodyYaw(),
                player.getPitch(),
                player.isSneaking(),
                player.limbAnimator.getPos(),
                player.limbAnimator.getSpeed(),
                player.getMainHandStack().copy(),
                player.getOffHandStack().copy(),
                time
        );
    }

    @Native
    private void box(BufferBuilder b, Matrix4f m,
                     float x, float y, float z,
                     float sx, float sy, float sz,
                     float r, float g, float bl, float a) {
        float x2=x+sx, y2=y+sy, z2=z+sz;
        b.vertex(m,x, y, z2).color(r,g,bl,a); b.vertex(m,x2,y, z2).color(r,g,bl,a); b.vertex(m,x2,y2,z2).color(r,g,bl,a); b.vertex(m,x, y2,z2).color(r,g,bl,a);
        b.vertex(m,x2,y, z ).color(r,g,bl,a); b.vertex(m,x, y, z ).color(r,g,bl,a); b.vertex(m,x, y2,z ).color(r,g,bl,a); b.vertex(m,x2,y2,z ).color(r,g,bl,a);
        b.vertex(m,x, y, z ).color(r,g,bl,a); b.vertex(m,x, y, z2).color(r,g,bl,a); b.vertex(m,x, y2,z2).color(r,g,bl,a); b.vertex(m,x, y2,z ).color(r,g,bl,a);
        b.vertex(m,x2,y, z2).color(r,g,bl,a); b.vertex(m,x2,y, z ).color(r,g,bl,a); b.vertex(m,x2,y2,z ).color(r,g,bl,a); b.vertex(m,x2,y2,z2).color(r,g,bl,a);
        b.vertex(m,x, y2,z2).color(r,g,bl,a); b.vertex(m,x2,y2,z2).color(r,g,bl,a); b.vertex(m,x2,y2,z ).color(r,g,bl,a); b.vertex(m,x, y2,z ).color(r,g,bl,a);
        b.vertex(m,x, y, z ).color(r,g,bl,a); b.vertex(m,x2,y, z ).color(r,g,bl,a); b.vertex(m,x2,y, z2).color(r,g,bl,a); b.vertex(m,x, y, z2).color(r,g,bl,a);
    }

    @Native
    private float ease(float t) {
        return 1f - (float) Math.pow(1f - MathHelper.clamp(t, 0f, 1f), 3);
    }

    @Native
    @Override
    public void onDisable() {
        ghosts.clear();
        trailGhosts.clear();
        super.onDisable();
    }

    // ── Ghost record ──────────────────────────────────────────────────────────

    private static class Ghost {
        final Vec3d pos;
        final float yaw, pitch;
        final boolean sneak;
        final float limbAngle, limbSpeed;
        final ItemStack mainHand, offHand;
        final long time;

        Ghost(Vec3d pos, float yaw, float pitch, boolean sneak,
              float limbAngle, float limbSpeed,
              ItemStack mainHand, ItemStack offHand, long time) {
            this.pos = pos; this.yaw = yaw; this.pitch = pitch;
            this.sneak = sneak;
            this.limbAngle = limbAngle; this.limbSpeed = limbSpeed;
            this.mainHand = mainHand; this.offHand = offHand;
            this.time = time;
        }
    }
}
