package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.math.Timer;
import space.visuals.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(name = "TargetESP", category = Category.RENDER, description = "Помечает активную цель")
public final class TargetESP extends Module {
    public static final TargetESP INSTANCE = new TargetESP();

    private final ModeSetting mode = new ModeSetting("Режим", "Souls", "Ромб", "Кольцо");
    private final ColorSetting colorTarget = new ColorSetting("Цвет", new ColorRGBA(100, 180, 255));
    private final BooleanSetting changeColorOnDamage = new BooleanSetting("Цвет при уроне", true);
    private final NumberSetting speed = new NumberSetting("Скорость", 0.5f, 0.1f, 5.0f, 0.1f);
    private final NumberSetting size = new NumberSetting("Размер", 1.5f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting particleCount = new NumberSetting("Кол-во частиц", 20f, 1f, 50f, 1f);
    private final NumberSetting particleThickness = new NumberSetting("Толщина частиц", 1.0f, 0.1f, 2.0f, 0.1f);

    private final Animation animation = new Animation(300L, 0.0F, Easing.BOTH_CUBIC);
    private final Animation moving = new Animation(70L, 0.0F, Easing.LINEAR);
    private final Animation hurtAnimation = new Animation(200L, 0.0F, Easing.BOTH_CUBIC);
    private final Animation alphaAnim = new Animation(300L, 0.0F, Easing.LINEAR);
    private final Animation sizeAnim  = new Animation(300L, 0.0F, Easing.LINEAR);

    private LivingEntity prevTarget;
    private final Timer targetLostTimer = new Timer();
    private double rotationPhase = 0;
    private long lastRotationUpdateMs = 0;
    private double ringProgress = 0;

    private TargetESP() {}

    private ColorRGBA getTargetColor() {
        float hitProgress = hurtAnimation.getValue();
        ColorRGBA baseColor = colorTarget.getColor();
        if (changeColorOnDamage.isEnabled() && hitProgress > 0.0F) {
            return baseColor.mix(new ColorRGBA(255, 0, 0), hitProgress);
        }
        return baseColor;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        LivingEntity target = null;
        var hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            var ehr = (net.minecraft.util.hit.EntityHitResult) hit;
            var entity = ehr.getEntity();
            if (entity instanceof LivingEntity living && living != mc.player && !living.isInvisible()) {
                if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
                    if (player.isSpectator()) return;
                }
                if (mc.player.distanceTo(living) <= 3.0) {
                    target = living;
                }
            }
        }

        if (target != null) {
            prevTarget = target;
            targetLostTimer.reset();
        }

        boolean shouldShow = target != null || !targetLostTimer.finished(1000L);

        animation.setEasing(Easing.FIGMA_EASE_IN_OUT);
        animation.update(shouldShow);
        alphaAnim.update(shouldShow ? 1f : 0f);
        sizeAnim.update(shouldShow ? 1f : 0f);

        float speedMultiplier = speed.getCurrent();
        moving.update(moving.getValue() + 10.0F * speedMultiplier + 50.0F * speedMultiplier);

        if (target != null && prevTarget == target) {
            hurtAnimation.update(target.hurtTime > 0);
        } else {
            hurtAnimation.update(false);
        }

        if (prevTarget != null && animation.getValue() != 0.0F) {
            MatrixStack ms = event.getMatrix();
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.enableDepthTest();
            if (mc.world.raycast(new RaycastContext(
                    mc.gameRenderer.getCamera().getPos(),
                    prevTarget.getEyePos(),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player)).getType() != HitResult.Type.MISS) {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);

            switch (mode.get()) {
                case "Ромб"   -> drawRhombus(ms, prevTarget);
                case "Кольцо" -> drawRing(ms, prevTarget);
                default       -> drawGhosts(ms, prevTarget);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
            ms.pop();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Vec3d getRenderPos(LivingEntity target) {
        float td = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
        return new Vec3d(
            MathHelper.lerp(td, target.prevX, target.getX()),
            MathHelper.lerp(td, target.prevY, target.getY()),
            MathHelper.lerp(td, target.prevZ, target.getZ())
        );
    }

    private void prepareMatrices(MatrixStack ms, Vec3d pos) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
    }

    private static void buildBuffer(BufferBuilder builder) {
        BuiltBuffer built = builder.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private void drawImage(MatrixStack ms, BufferBuilder buf, Identifier tex,
                           float x, float y, float w, float h, ColorRGBA color) {
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Camera cam = mc.gameRenderer.getCamera();
        ms.push();
        ms.multiply(cam.getRotation());
        Matrix4f m = ms.peek().getPositionMatrix();
        buf.vertex(m, x,     y,     0).texture(0, 1).color(color.getRGB());
        buf.vertex(m, x + w, y,     0).texture(1, 1).color(color.getRGB());
        buf.vertex(m, x + w, y + h, 0).texture(1, 0).color(color.getRGB());
        buf.vertex(m, x,     y + h, 0).texture(0, 0).color(color.getRGB());
        ms.pop();
    }

    // ── Rhombus (Ромб) ───────────────────────────────────────────────────────

    private void drawRhombus(MatrixStack ms, LivingEntity target) {
        Camera cam = mc.gameRenderer.getCamera();

        double deltaTime = lastRotationUpdateMs == 0 ? 0 : (System.currentTimeMillis() - lastRotationUpdateMs) / 1000.0;
        lastRotationUpdateMs = System.currentTimeMillis();
        rotationPhase += 2.0 * deltaTime;

        float displayedSize = size.getCurrent() * 0.6f * alphaAnim.getValue();
        float halfSize = displayedSize / 2f;

        float hurtFactor = changeColorOnDamage.isEnabled() && target.hurtTime > 0
                ? MathHelper.clamp((float) target.hurtTime / Math.max(1, target.maxHurtTime), 0f, 1f) : 0f;
        ColorRGBA color = colorTarget.getColor().mix(new ColorRGBA(255, 0, 0), hurtFactor)
                .withAlpha((int)(alphaAnim.getValue() * 180));

        ms.push();
        // позиционируем на середину тела цели
        prepareMatrices(ms, getRenderPos(target));
        ms.translate(0, target.getHeight() * 0.5, 0);
        // billboard — поворачиваем к камере
        ms.multiply(cam.getRotation());
        // вращение ромба
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(Math.sin(rotationPhase) * 180)));

        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, Zenith.id("textures/target.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);

        Matrix4f m = ms.peek().getPositionMatrix();
        int c = color.getRGB();
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, -halfSize,  halfSize, 0).texture(0, 1).color(c);
        buf.vertex(m,  halfSize,  halfSize, 0).texture(1, 1).color(c);
        buf.vertex(m,  halfSize, -halfSize, 0).texture(1, 0).color(c);
        buf.vertex(m, -halfSize, -halfSize, 0).texture(0, 0).color(c);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableDepthTest();
        ms.pop();
    }

    // ── Ring (Кольцо) ────────────────────────────────────────────────────────

    private void drawRing(MatrixStack ms, LivingEntity target) {
        float radius = target.getWidth() * 0.8F * size.getCurrent();
        float entityHeight = target.getHeight();

        double duration = 2000.0;
        double elapsedMillis = System.currentTimeMillis() % duration;
        // MONOTON логика: 0→1 первую половину, 1→0 вторую
        double progress = elapsedMillis / (duration / 2);
        progress = elapsedMillis > duration / 2 ? progress - 1 : 1 - progress;
        // ease in-out quad
        progress = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;

        // след: смещение вниз в первой половине, вверх во второй
        double heightOffset = (entityHeight / 2)
                * (progress > 0.5 ? 1 - progress : progress)
                * (elapsedMillis > duration / 2 ? -1 : 1);

        ColorRGBA base = getTargetColor();
        ColorRGBA dark = base.mix(new ColorRGBA(0, 0, 0), 0.5f);

        ms.push();
        prepareMatrices(ms, getRenderPos(target));

        RenderSystem.lineWidth(particleThickness.getCurrent() * 1.5f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
        RenderSystem.disableDepthTest();

        Matrix4f m = ms.peek().getPositionMatrix();
        float ringY = (float)(entityHeight * progress);

        // лента: для каждого угла — две вершины (основная + хвост с alpha=0)
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int deg = 0; deg <= 360; deg++) {
            double rad = Math.toRadians(deg);
            float cx = (float)(Math.cos(rad) * radius);
            float cz = (float)(Math.sin(rad) * radius);

            // градиент по углу как в MONOTON
            float t2 = deg / 360.0f;
            ColorRGBA grad = base.mix(dark, t2);

            // основная вершина кольца
            buf.vertex(m, cx, ringY, cz).color(grad.getRGB());
        }
        buildBuffer(buf);

        // хвост — второй проход с прозрачностью
        buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int deg = 0; deg <= 360; deg++) {
            double rad = Math.toRadians(deg);
            float cx = (float)(Math.cos(rad) * radius);
            float cz = (float)(Math.sin(rad) * radius);

            float t2 = deg / 360.0f;
            ColorRGBA grad = base.mix(dark, t2);

            // вершина хвоста — смещена по Y, прозрачная
            buf.vertex(m, cx, (float)(ringY + heightOffset), cz)
               .color(grad.withAlpha(0).getRGB());
        }
        buildBuffer(buf);

        RenderSystem.enableDepthTest();
        ms.pop();
    }

    // ── Ghosts (Souls) ───────────────────────────────────────────────────────

    private void drawGhosts(MatrixStack ms, LivingEntity target) {
        Camera cam = mc.gameRenderer.getCamera();
        ColorRGBA color = getTargetColor();
        float width = prevTarget.getWidth() * 0.8F * size.getCurrent();
        Identifier bloom = Zenith.id("textures/bloom.png");
        RenderSystem.setShaderTexture(0, bloom);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        ms.push();
        prepareMatrices(ms, getRenderPos(prevTarget));
        int step = 2, wormTick = 0, wormCD = 0;
        for (int i = 0; i < 360; i += step) {
            // base particle size driven by thickness slider
            float sz    = (0.03F + 0.001F * wormTick) * particleThickness.getCurrent();
            float bigSz = (0.12F + 0.002F * wormTick) * particleThickness.getCurrent();
            if (wormCD > 0) {
                wormCD -= step;
            } else {
                wormTick += step;
                if (wormTick > 50) { wormCD = 100; wormTick = 0; }
                else {
                    float sin = (float)(Math.sin(Math.toRadians(i + moving.getValue())) * width);
                    float cos = (float)(Math.cos(Math.toRadians(i + moving.getValue())) * width);
                    float yy  = prevTarget.getHeight() * 0.5F
                              + prevTarget.getHeight() * 0.3F
                              * (float)Math.sin(Math.toRadians(i / 2.0 + moving.getValue() / 5.0));
                    ms.push();
                    ms.translate(sin, yy, cos);
                    ms.multiply(cam.getRotation());
                    Matrix4f m = ms.peek().getPositionMatrix();
                    ColorRGBA bc = color.withAlpha(color.getAlpha() * animation.getValue() * 0.05F);
                    buf.vertex(m, -bigSz/2, -bigSz/2, 0).texture(0,1).color(bc.getRGB());
                    buf.vertex(m,  bigSz/2, -bigSz/2, 0).texture(1,1).color(bc.getRGB());
                    buf.vertex(m,  bigSz/2,  bigSz/2, 0).texture(1,0).color(bc.getRGB());
                    buf.vertex(m, -bigSz/2,  bigSz/2, 0).texture(0,0).color(bc.getRGB());
                    ColorRGBA cc = color.withAlpha(color.getAlpha() * animation.getValue());
                    buf.vertex(m, -sz/2, -sz/2, 0).texture(0,1).color(cc.getRGB());
                    buf.vertex(m,  sz/2, -sz/2, 0).texture(1,1).color(cc.getRGB());
                    buf.vertex(m,  sz/2,  sz/2, 0).texture(1,0).color(cc.getRGB());
                    buf.vertex(m, -sz/2,  sz/2, 0).texture(0,0).color(cc.getRGB());
                    ms.pop();
                }
            }
        }
        buildBuffer(buf);
        ms.pop();
    }

    private float easeOutCubic(float x) { return 1.0F - (float)Math.pow(1.0F - x, 3); }
}
