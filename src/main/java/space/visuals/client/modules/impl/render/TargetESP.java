package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
import org.joml.Vector3f;
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

    private final ModeSetting mode = new ModeSetting("Режим", "Кристалы", "Souls", "Ромб", "Кольцо", "Кубы");
    private final ModeSetting crystalDirection = new ModeSetting("Направление", () -> mode.is("Кристалы"), "Вертикальный", "Горизонтальный", "Рандомный");

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
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
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
                case "Ромб"     -> drawRhombus(ms, prevTarget);
                case "Кольцо"   -> drawRing(ms, prevTarget);
                case "Кубы"     -> drawCubes(ms, prevTarget);
                case "Кристалы" -> drawCrystals(ms, prevTarget);
                default         -> drawGhosts(ms, prevTarget);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
            ms.pop();
        }
    }

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

    // Rhombus
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
        prepareMatrices(ms, getRenderPos(target));
        ms.translate(0, target.getHeight() * 0.5, 0);
        ms.multiply(cam.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(Math.sin(rotationPhase) * 180)));
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, Zenith.id("textures/target.png"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
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

    // Ring
    private void drawRing(MatrixStack ms, LivingEntity target) {
        float radius = target.getWidth() * 0.8F * size.getCurrent();
        float entityHeight = target.getHeight();
        double duration = 2000.0;
        double elapsedMillis = System.currentTimeMillis() % duration;
        double progress = elapsedMillis / (duration / 2);
        progress = elapsedMillis > duration / 2 ? progress - 1 : 1 - progress;
        progress = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;
        double heightOffset = (entityHeight / 2)
                * (progress > 0.5 ? 1 - progress : progress)
                * (elapsedMillis > duration / 2 ? -1 : 1);
        ColorRGBA base = getTargetColor();
        ColorRGBA dark = base.mix(new ColorRGBA(0, 0, 0), 0.5f);
        ms.push();
        prepareMatrices(ms, getRenderPos(target));
        RenderSystem.lineWidth(particleThickness.getCurrent() * 1.5f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        Matrix4f m = ms.peek().getPositionMatrix();
        float ringY = (float)(entityHeight * progress);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int deg = 0; deg <= 360; deg++) {
            double rad = Math.toRadians(deg);
            float cx = (float)(Math.cos(rad) * radius);
            float cz = (float)(Math.sin(rad) * radius);
            float t2 = deg / 360.0f;
            ColorRGBA grad = base.mix(dark, t2);
            buf.vertex(m, cx, ringY, cz).color(grad.getRGB());
        }
        buildBuffer(buf);
        buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int deg = 0; deg <= 360; deg++) {
            double rad = Math.toRadians(deg);
            float cx = (float)(Math.cos(rad) * radius);
            float cz = (float)(Math.sin(rad) * radius);
            float t2 = deg / 360.0f;
            ColorRGBA grad = base.mix(dark, t2);
            buf.vertex(m, cx, (float)(ringY + heightOffset), cz).color(grad.withAlpha(0).getRGB());
        }
        buildBuffer(buf);
        RenderSystem.enableDepthTest();
        ms.pop();
    }

    // Ghosts / Souls
    private void drawGhosts(MatrixStack ms, LivingEntity target) {
        Camera cam = mc.gameRenderer.getCamera();
        ColorRGBA color = getTargetColor();
        float width = target.getWidth() * 0.8F * size.getCurrent();
        Identifier bloom = Zenith.id("textures/bloom.png");
        RenderSystem.setShaderTexture(0, bloom);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        ms.push();
        prepareMatrices(ms, getRenderPos(target));
        int step = 2, wormTick = 0, wormCD = 0;
        for (int i = 0; i < 360; i += step) {
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
                    float yy  = target.getHeight() * 0.5F
                              + target.getHeight() * 0.3F
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


    private void drawCube(BufferBuilder buf, MatrixStack ms, float x, float y, float z, 
                          float size, float rotY, float rotX, int color, float alpha) {
        if (size <= 0f) return;
        
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        
        Matrix4f m = ms.peek().getPositionMatrix();
        
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int)(255f * alpha);
        
        float s = size / 2f;
        
        // Передняя грань (яркая)
        addCubeQuad(buf, m, -s, -s, s, s, -s, s, s, s, s, -s, s, s, r, g, b, a);
        
        // Задняя грань (темнее)
        addCubeQuad(buf, m, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s, 
                    (int)(r*0.9), (int)(g*0.9), (int)(b*0.9), a);
        
        // Левая грань
        addCubeQuad(buf, m, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, 
                    (int)(r*0.8), (int)(g*0.8), (int)(b*0.8), a);
        
        // Правая грань
        addCubeQuad(buf, m, s, -s, -s, s, s, -s, s, s, s, s, -s, s, 
                    (int)(r*0.8), (int)(g*0.8), (int)(b*0.8), a);
        
        // Верхняя грань (светлее)
        addCubeQuad(buf, m, -s, s, -s, -s, s, s, s, s, s, s, s, -s, 
                    Math.min(255, (int)(r*1.1)), Math.min(255, (int)(g*1.1)), Math.min(255, (int)(b*1.1)), a);
        
        // Нижняя грань (темнее)
        addCubeQuad(buf, m, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, 
                    (int)(r*0.7), (int)(g*0.7), (int)(b*0.7), a);
        
        ms.pop();
    }

    private void addCubeQuad(BufferBuilder buf, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int r, int g, int b, int a) {
        // Первый треугольник
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buf.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        
        // Второй треугольник
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buf.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        buf.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    private int getThemeColorAngle(int offsetAngle, long now) {
        float timeFactor = (float)(now % 3000L) / 3000f;
        int angle = ((int)(timeFactor * 360f) + offsetAngle) % 360;
        return Zenith.getInstance().getThemeManager().getClientColor(angle).getRGB();
    }

    private int applyAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int)(255f * alpha), 0, 255);
        return (a << 24) | (color & 0xFFFFFF);
    }

    // ── Crystals ──────────────────────────────────────────────────────────────

    private void drawCrystals(MatrixStack ms, LivingEntity target) {
        float alphaValue = animation.getValue();
        if (alphaValue <= 0f) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d targetPos = new Vec3d(
            MathHelper.lerp(tickDelta, target.prevX, target.getX()),
            MathHelper.lerp(tickDelta, target.prevY, target.getY()),
            MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
        );
        Vec3d renderPos = targetPos.subtract(camera.getPos());

        float time = ((float) mc.player.age + tickDelta) * speed.getCurrent();
        float entityHeight = target.getHeight();
        float entityWidth = target.getWidth();
        float halfWidth = entityWidth * 0.5f;
        int crystalCount = (int) particleCount.getCurrent();
        float baseCrystalScale = 0.05f * size.getCurrent();
        float bloomScale = baseCrystalScale * 13.0f * particleThickness.getCurrent();

        ms.push();
        ms.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        ColorRGBA baseColor = getTargetColor();
        int color = baseColor.getRGB();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, Zenith.id("textures/bloom.png"));

        BufferBuilder glowBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < crystalCount; i++) {
            float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angleOffset = i * (360f / crystalCount) + seed1 * 12f;
            float angle = time + angleOffset;
            float radius = halfWidth + 0.25f + seed3 * 0.15f;
            float x = radius * (float) Math.cos(Math.toRadians(angle));
            float z = radius * (float) Math.sin(Math.toRadians(angle));
            float y = seed2 * entityHeight;
            drawCrystalBillboard(glowBuf, ms, camera, x, y, z, bloomScale * alphaValue, applyAlpha(color, alphaValue * 0.4f));
        }
        buildBuffer(glowBuf);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder crystalBuf = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < crystalCount; i++) {
            float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angleOffset = i * (360f / crystalCount) + seed1 * 12f;
            float angle = time + angleOffset;
            float radius = halfWidth + 0.25f + seed3 * 0.15f;
            float x = radius * (float) Math.cos(Math.toRadians(angle));
            float z = radius * (float) Math.sin(Math.toRadians(angle));
            float y = seed2 * entityHeight;
            drawEssenceCrystalMesh(crystalBuf, ms, x, y, z, baseCrystalScale * alphaValue, angle, color, alphaValue * 0.8f, i);
        }
        BufferRenderer.drawWithGlobalProgram(crystalBuf.end());

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawCubes(MatrixStack ms, LivingEntity target) {
        float alphaValue = animation.getValue();
        if (alphaValue <= 0f) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d targetPos = new Vec3d(
            MathHelper.lerp(tickDelta, target.prevX, target.getX()),
            MathHelper.lerp(tickDelta, target.prevY, target.getY()),
            MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
        );
        Vec3d renderPos = targetPos.subtract(camera.getPos());

        float cubeScaleFactor = size.getCurrent() * 0.12f;
        float time = ((float) mc.player.age + tickDelta) * speed.getCurrent();
        float entityHeight = target.getHeight();
        float entityWidth = target.getWidth();
        float halfWidth = entityWidth * 0.5f;
        int cubeCount = (int) particleCount.getCurrent();

        ms.push();
        ms.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < cubeCount; i++) {
            float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angleOffset = i * (360.0f / cubeCount) + seed1 * 12.0f;
            float angle = time + angleOffset;
            float radius = halfWidth + 0.25f + seed3 * 0.15f;
            float x = radius * (float) Math.cos(Math.toRadians(angle));
            float z = radius * (float) Math.sin(Math.toRadians(angle));
            float bobbing = (float) Math.sin(time * 0.05f + i * 0.3f) * 0.1f;
            float y = seed2 * entityHeight * 1.05f + bobbing;
            int color = getTargetColor().getRGB();
            drawCube(buf, ms, x, y, z, cubeScaleFactor, time * 4.0f, time * 3.0f, color, alphaValue * 0.5f);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawCrystalBillboard(BufferBuilder buf, MatrixStack ms, Camera camera, float x, float y, float z, float size, int color) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(camera.getRotation());
        Matrix4f m = ms.peek().getPositionMatrix();
        float s = size / 2f;
        buf.vertex(m, -s, -s, 0).texture(0, 1).color(color);
        buf.vertex(m,  s, -s, 0).texture(1, 1).color(color);
        buf.vertex(m,  s,  s, 0).texture(1, 0).color(color);
        buf.vertex(m, -s,  s, 0).texture(0, 0).color(color);
        ms.pop();
    }

    private void drawEssenceCrystalMesh(BufferBuilder buffer, MatrixStack matrices,
                                        float x, float y, float z, float scale,
                                        float yaw, int color, float alpha, int crystalIndex) {
        matrices.push();
        matrices.translate(x, y, z);
        
        // Пульсация
        float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
        matrices.scale(scale * pulsation, scale * pulsation, scale * pulsation);
        
        // Вращение кристалла
        float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * 0.5f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw + selfRotation));
        
        // Направление кристалла
        String direction = crystalDirection.get();
        if (direction.equals("Горизонтальный")) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        } else if (direction.equals("Рандомный")) {
            float seed1 = (float) Math.sin(crystalIndex * 2.7f + 0.5f);
            float seed2 = (float) Math.cos(crystalIndex * 3.3f + 0.9f);
            float seed3 = (float) Math.sin(crystalIndex * 1.9f + 1.3f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(seed1 * 45f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(seed2 * 45f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(seed3 * 45f));
        }
        
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f m = entry.getPositionMatrix();
        
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int)(255 * alpha * 0.8f);
        
        // Два ромба (октаэдр без призмы): только верхняя и нижняя пирамиды
        // Вершины экватора (8 точек на уровне y=0)
        float s = 1.0f;
        float h_pyramid = 1.8f; // высота каждой пирамиды
        int numSides = 8;
        
        java.util.List<Vec3d> equatorVertices = new java.util.ArrayList<>();
        for (int i = 0; i < numSides; i++) {
            float angle = (float) (2 * Math.PI * i / numSides);
            float vx = (float) (s * Math.cos(angle));
            float vz = (float) (s * Math.sin(angle));
            equatorVertices.add(new Vec3d(vx, 0, vz));
        }
        
        Vec3d vTop    = new Vec3d(0,  h_pyramid, 0);
        Vec3d vBottom = new Vec3d(0, -h_pyramid, 0);
        
        // Верхняя пирамида (светлее)
        int rL = Math.min(255, (int)(r * 1.3f));
        int gL = Math.min(255, (int)(g * 1.3f));
        int bL = Math.min(255, (int)(b * 1.3f));
        for (int i = 0; i < numSides; i++) {
            Vec3d v1 = equatorVertices.get(i);
            Vec3d v2 = equatorVertices.get((i + 1) % numSides);
            buffer.vertex(m, (float)vTop.x, (float)vTop.y, (float)vTop.z).color(rL, gL, bL, a);
            buffer.vertex(m, (float)v1.x,   (float)v1.y,   (float)v1.z  ).color(rL, gL, bL, a);
            buffer.vertex(m, (float)v2.x,   (float)v2.y,   (float)v2.z  ).color(rL, gL, bL, a);
        }
        
        // Нижняя пирамида (темнее)
        int rD = (int)(r * 0.6f);
        int gD = (int)(g * 0.6f);
        int bD = (int)(b * 0.6f);
        for (int i = 0; i < numSides; i++) {
            Vec3d v1 = equatorVertices.get(i);
            Vec3d v2 = equatorVertices.get((i + 1) % numSides);
            buffer.vertex(m, (float)vBottom.x, (float)vBottom.y, (float)vBottom.z).color(rD, gD, bD, a);
            buffer.vertex(m, (float)v2.x,      (float)v2.y,      (float)v2.z     ).color(rD, gD, bD, a);
            buffer.vertex(m, (float)v1.x,      (float)v1.y,      (float)v1.z     ).color(rD, gD, bD, a);
        }
        
        matrices.pop();
    }
}