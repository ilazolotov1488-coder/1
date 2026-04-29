package space.visuals.client.modules.impl.render;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import space.visuals.Zenith;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "Cosmetics", category = Category.RENDER, description = "Крылья и шляпа")
public final class Cosmetics extends Module {
    public static final Cosmetics INSTANCE = new Cosmetics();

    // ── Тип косметики ──────────────────────────────────────────────────────────
    private final ModeSetting type = new ModeSetting("Тип", "Крылья", "Шляпа", "Крылья + Шляпа");

    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int   DEFAULT_ALPHA  = 220;

    private static final WingPoint[] SHAPE = {
            new WingPoint(0.08f,  0.10f,  0.88f),
            new WingPoint(0.28f,  0.34f,  0.78f),
            new WingPoint(0.56f,  0.82f,  0.62f),
            new WingPoint(0.86f,  0.30f,  0.52f),
            new WingPoint(1.14f,  0.46f,  0.40f),
            new WingPoint(1.24f,  0.04f,  0.30f),
            new WingPoint(1.02f, -0.18f,  0.28f),
            new WingPoint(1.18f, -0.64f,  0.22f),
            new WingPoint(0.86f, -0.46f,  0.20f),
            new WingPoint(0.80f, -0.98f,  0.14f),
            new WingPoint(0.54f, -0.74f,  0.16f),
            new WingPoint(0.30f, -1.16f,  0.12f),
            new WingPoint(0.10f, -0.54f,  0.18f)
    };

    private final BooleanSetting self    = new BooleanSetting("На себя",    true);
    private final BooleanSetting players = new BooleanSetting("На игроков", false);
    private final NumberSetting  size    = new NumberSetting("Размер", 1.0f, 0.75f, 1.35f, 0.05f);

    private float   selfBodyYaw;
    private boolean selfBodyYawInitialized;

    @SuppressWarnings("unchecked")
    private Cosmetics() {
        // Регистрируем FeatureRenderer для шляпы через Fabric API
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER) {
                registrationHelper.register(new ChinaHatFeatureRenderer(
                        (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) entityRenderer));
            }
        });
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        // Крылья рендерим только в нужных режимах
        if (type.is("Шляпа")) return;

        MatrixStack stack     = event.getMatrix();
        float       tickDelta = event.getPartialTicks();
        Vec3d       camera    = mc.gameRenderer.getCamera().getPos();

        stack.push();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (self.isEnabled() && !mc.options.getPerspective().isFirstPerson()
                && mc.player.isAlive() && !hasElytra(mc.player)) {
            try { renderWings(stack, mc.player, tickDelta, camera); } catch (Exception ignored) {}
        }

        if (players.isEnabled()) {
            for (var entity : mc.world.getEntities()) {
                if (!(entity instanceof PlayerEntity player) || player == mc.player) continue;
                if (!player.isAlive() || hasElytra(player)) continue;
                try { renderWings(stack, player, tickDelta, camera); } catch (Exception ignored) {}
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,       GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        stack.pop();
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private void renderWings(MatrixStack stack, PlayerEntity player, float tickDelta, Vec3d camera) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - camera.x;
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - camera.y;
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - camera.z;

        float bodyYaw = resolveBodyYaw(player, tickDelta);
        float move    = MathHelper.clamp(player.limbAnimator.getSpeed(tickDelta), 0f, 1f);

        WingPose pose = resolvePose(player, tickDelta);
        if (pose == null) return;

        float flap      = (float) Math.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open      = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.getCurrent() * pose.scaleMultiplier;

        int baseColor    = resolveBaseColor();
        int glowColor    = resolveGlowColor(baseColor);
        int coreColor    = resolveCoreColor(baseColor);
        int outlineColor = baseColor;

        stack.push();
        stack.translate(x, y, z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        if (pose.preTranslateY != 0f || pose.preTranslateZ != 0f)
            stack.translate(0f, pose.preTranslateY, pose.preTranslateZ);
        if (pose.pitchRotation != 0f)
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        if (pose.rollRotation != 0f)
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        stack.translate(0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);

        renderWingSide(stack, -1f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        renderWingSide(stack,  1f, open, baseColor, glowColor, coreColor, outlineColor, pose);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open,
                                int baseColor, int glowColor, int coreColor, int outlineColor,
                                WingPose pose) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingLayer(stack, side, 1.22f, setAlpha(glowColor, (int)(DEFAULT_ALPHA * 0.22f)), setAlpha(glowColor, 0));
        drawWingLayer(stack, side, 0.84f, setAlpha(coreColor, (int)(DEFAULT_ALPHA * 0.26f)), setAlpha(coreColor, 0));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWingLayer(stack, side, 1.0f, setAlpha(baseColor, DEFAULT_ALPHA), setAlpha(baseColor, 10));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingOutline(stack, side, 1.0f, setAlpha(outlineColor, (int)(DEFAULT_ALPHA * 0.62f)));
        drawWingRibs(stack, side, 0.96f, setAlpha(glowColor, (int)(DEFAULT_ALPHA * 0.20f)));

        stack.pop();
    }

    private void drawWingLayer(MatrixStack stack, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint cur  = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            vertex(buffer, matrix, 0f, 0f, 0f, rootColor);
            vertex(buffer, matrix, side * cur.x  * scale, cur.y  * scale, 0f, applyPointAlpha(edgeColor, cur.alphaMul));
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0f, applyPointAlpha(edgeColor, next.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawWingOutline(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.lineWidth(1.35f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (WingPoint point : SHAPE)
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, color);
        vertex(buffer, matrix, side * SHAPE[0].x * scale, SHAPE[0].y * scale, 0f, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawWingRibs(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        int[] ribIndices = {2, 4, 7, 9, 11};
        RenderSystem.lineWidth(0.9f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            vertex(buffer, matrix, 0f, 0f, 0f, setAlpha(color, Math.max(8, (int)(alpha(color) * 0.75f))));
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, applyPointAlpha(color, point.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    // ── Color helpers ──────────────────────────────────────────────────────────

    private int resolveBaseColor() {
        // Используем цвет темы клиента
        var c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private int resolveGlowColor(int base) {
        return interpolateColor(base, 0xFFFFFFFF, 0.28f);
    }

    private int resolveCoreColor(int base) {
        return interpolateColor(base, 0xFFFFFFFF, 0.55f);
    }

    private static int interpolateColor(int from, int to, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int r = (int)(red(from)   + (red(to)   - red(from))   * t);
        int g = (int)(green(from) + (green(to) - green(from)) * t);
        int b = (int)(blue(from)  + (blue(to)  - blue(from))  * t);
        int a = (int)(alpha(from) + (alpha(to) - alpha(from)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int applyPointAlpha(int color, float multiplier) {
        return setAlpha(color, Math.max(0, Math.min(255, (int)(alpha(color) * multiplier))));
    }

    private static int setAlpha(int color, int a) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int c) { return (c >> 24) & 0xFF; }
    private static int red(int c)   { return (c >> 16) & 0xFF; }
    private static int green(int c) { return (c >>  8) & 0xFF; }
    private static int blue(int c)  { return  c        & 0xFF; }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z)
                .color(red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f);
    }

    // ── Body yaw ───────────────────────────────────────────────────────────────

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        if (player != mc.player) return target;
        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    // ── Pose ───────────────────────────────────────────────────────────────────

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        if (player.isGliding()) {
            float flightTicks    = (float) player.getGlidingTicks() + tickDelta;
            float flightProgress = MathHelper.clamp(flightTicks * flightTicks / 100f, 0f, 1f);
            float pitchRotation  = flightProgress * (-90f - pitch);
            return new WingPose(0.34f, 0.46f, 0f, 0f, pitchRotation, 0f,
                    0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.06f, -5f, -2f, 0.13f);
        }

        if (player.isTouchingWater()) return null;

        if (player.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f,
                    1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
        }

        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f,
                1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }

    // ── ChinaHat FeatureRenderer ───────────────────────────────────────────────

    private static final class ChinaHatFeatureRenderer
            extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

        private static final int   SEGMENTS = 60;
        private static final float PI2      = (float)(Math.PI * 2);

        ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) {
            super(ctx);
        }

        @Override
        public void render(MatrixStack ms, VertexConsumerProvider vcp, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            Cosmetics module = Cosmetics.INSTANCE;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!module.isEnabled()) return;
            if (type(module).equals("Крылья")) return; // только шляпа или оба
            if (mc.player == null || mc.world == null) return;

            boolean isSelf = (state.id == mc.player.getId());
            if (!isSelf) return;
            if (isSelf && mc.options.getPerspective().isFirstPerson()) return;

            ms.push();
            this.getContextModel().head.rotate(ms);

            float yOffset = -0.489f;
            var entity = mc.world.getEntityById(state.id);
            if (entity instanceof LivingEntity living && !living.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                yOffset -= 0.0625f;
            }
            ms.translate(0f, yOffset, 0f);

            if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw();

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            Matrix4f matrix = ms.peek().getPositionMatrix();
            float width      = 0.62f;
            float coneHeight = 0.3f;
            int centerColor  = hatColor(200);
            int edgeColor    = hatColor(80);
            int outlineColor = hatColor(255);

            // Внутренняя сторона
            RenderSystem.depthMask(false);
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                float a1 = i       * PI2 / SEGMENTS;
                float a2 = (i + 1) * PI2 / SEGMENTS;
                float x1 = -MathHelper.sin(a1) * width, z1 = MathHelper.cos(a1) * width;
                float x2 = -MathHelper.sin(a2) * width, z2 = MathHelper.cos(a2) * width;
                buf.vertex(matrix, 0, -coneHeight, 0).color(centerColor);
                buf.vertex(matrix, x2, 0, z2).color(edgeColor);
                buf.vertex(matrix, x1, 0, z1).color(edgeColor);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            // Внешняя сторона
            RenderSystem.depthMask(true);
            buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                float a1 = i       * PI2 / SEGMENTS;
                float a2 = (i + 1) * PI2 / SEGMENTS;
                float x1 = -MathHelper.sin(a1) * width, z1 = MathHelper.cos(a1) * width;
                float x2 = -MathHelper.sin(a2) * width, z2 = MathHelper.cos(a2) * width;
                buf.vertex(matrix, 0, -coneHeight, 0).color(centerColor);
                buf.vertex(matrix, x1, 0, z1).color(edgeColor);
                buf.vertex(matrix, x2, 0, z2).color(edgeColor);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            // Контур
            RenderSystem.lineWidth(2.0f);
            buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                float a1 = i       * PI2 / SEGMENTS;
                float a2 = (i + 1) * PI2 / SEGMENTS;
                float x1 = -MathHelper.sin(a1) * width, z1 = MathHelper.cos(a1) * width;
                float x2 = -MathHelper.sin(a2) * width, z2 = MathHelper.cos(a2) * width;
                buf.vertex(matrix, x1, 0, z1).color(outlineColor);
                buf.vertex(matrix, x2, 0, z2).color(outlineColor);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            ms.pop();
        }

        private static String type(Cosmetics m) { return m.type.get(); }

        private static int hatColor(int alpha) {
            var c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
            return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }
    }

    // ── Inner types ────────────────────────────────────────────────────────────

    private static final class WingPoint {
        final float x, y, alphaMul;
        WingPoint(float x, float y, float alphaMul) { this.x = x; this.y = y; this.alphaMul = alphaMul; }
    }

    private static final class WingPose {
        final float preTranslateY, preTranslateZ;
        final float anchorY, anchorZ;
        final float pitchRotation, rollRotation;
        final float openMultiplier, scaleMultiplier;
        final float motionSpreadBoost, flapAmplitude;
        final float sideOffset, sideYOffset, sideZOffset;
        final float sideRoll, sidePitch, flapSpeed;

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset,
                 float sideRoll, float sidePitch, float flapSpeed) {
            this(preTranslateY, preTranslateZ, anchorY, anchorZ, pitchRotation, rollRotation,
                    openMultiplier, scaleMultiplier, motionSpreadBoost, flapAmplitude,
                    sideOffset, 0f, sideZOffset, sideRoll, sidePitch, flapSpeed);
        }

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset,
                 float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY   = preTranslateY;
            this.preTranslateZ   = preTranslateZ;
            this.anchorY         = anchorY;
            this.anchorZ         = anchorZ;
            this.pitchRotation   = pitchRotation;
            this.rollRotation    = rollRotation;
            this.openMultiplier  = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude   = flapAmplitude;
            this.sideOffset      = sideOffset;
            this.sideYOffset     = sideYOffset;
            this.sideZOffset     = sideZOffset;
            this.sideRoll        = sideRoll;
            this.sidePitch       = sidePitch;
            this.flapSpeed       = flapSpeed;
        }
    }
}
