package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import space.visuals.Zenith;
import space.visuals.base.events.impl.entity.EventEntityColor;
import space.visuals.base.events.impl.render.EventHudRender;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.CustomRenderTarget;

@ModuleAnnotation(name = "ShaderESP", category = Category.RENDER, description = "Подсвечивает сущности шейдером")
public final class ShaderESP extends Module {

    public static final ShaderESP INSTANCE = new ShaderESP();

    // Флаг — сейчас рендерим маску, чтобы EventEntityColor знал что делать
    public static boolean renderingMask = false;

    private final BooleanSetting players  = new BooleanSetting("Игроки", true);
    private final BooleanSetting mobs     = new BooleanSetting("Мобы", false);
    private final BooleanSetting self     = new BooleanSetting("Себя", false);
    private final BooleanSetting outline  = new BooleanSetting("Обводка", true);
    private final BooleanSetting fill     = new BooleanSetting("Заливка", false);
    private final BooleanSetting glow     = new BooleanSetting("Свечение", true);
    private final ModeSetting    colorMode = new ModeSetting("Цвет", "Клиентский", "Кастом");
    private final ColorSetting   color    = new ColorSetting("Цвет", new ColorRGBA(100, 200, 255));
    private final NumberSetting  glowSize = new NumberSetting("Размер свечения", 4f, 1f, 12f, 0.5f);
    private final NumberSetting  outlineW = new NumberSetting("Толщина обводки", 2f, 1f, 6f, 0.5f);

    // Буферы
    private CustomRenderTarget entityTarget;  // маска сущностей
    private CustomRenderTarget blurTarget1;   // промежуточный blur
    private CustomRenderTarget blurTarget2;   // промежуточный blur

    private boolean hasEntities = false;

    private ShaderESP() {}

    @Override
    public void onDisable() {
        super.onDisable();
        hasEntities = false;
        renderingMask = false;
        releaseTargets();
    }

    // ── Шаг 1: рендерим маску сущностей в отдельный буфер ────────────────────

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        ensureTargets();

        entityTarget.setup(true);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        float td = event.getPartialTicks();
        Camera camera = mc.gameRenderer.getCamera();
        VertexConsumerProvider.Immediate buffers = mc.getBufferBuilders().getEntityVertexConsumers();

        hasEntities = false;
        renderingMask = true;

        try {
            for (Entity entity : mc.world.getEntities()) {
                if (!shouldRender(entity)) continue;
                if (entity.distanceTo(mc.player) > 128f) continue;

                mc.getEntityRenderDispatcher().render(
                        entity, 0, 0, 0, td,
                        event.getMatrix(), buffers,
                        mc.getEntityRenderDispatcher().getLight(entity, td)
                );
                hasEntities = true;
            }
            buffers.draw();
        } finally {
            renderingMask = false;
        }

        entityTarget.stop();
        RenderSystem.disableBlend();
    }

    // ── Шаг 2: применяем эффект и рисуем на экран ────────────────────────────

    @EventTarget
    public void onHudRender(EventHudRender event) {
        if (!hasEntities || entityTarget == null) return;

        int baseColor = resolveColor();
        float w = mw.getScaledWidth();
        float h = mw.getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        if (glow.isEnabled()) {
            // Blur маски → рисуем как glow
            applyBlur(entityTarget, blurTarget1, blurTarget2, glowSize.getCurrent());
            mc.getFramebuffer().beginWrite(false);
            drawColoredTexture(blurTarget2.getColorAttachment(), baseColor, w, h, true);
        }

        if (fill.isEnabled()) {
            mc.getFramebuffer().beginWrite(false);
            drawColoredTexture(entityTarget.getColorAttachment(), baseColor, w, h, true);
        }

        if (outline.isEnabled()) {
            // Тонкий blur для обводки
            applyBlur(entityTarget, blurTarget1, blurTarget2, outlineW.getCurrent());
            mc.getFramebuffer().beginWrite(false);
            // Рисуем blur минус оригинал = только контур
            drawOutline(blurTarget2.getColorAttachment(), entityTarget.getColorAttachment(), baseColor, w, h);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        mc.getFramebuffer().beginWrite(true);
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private boolean shouldRender(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (player == mc.player) return self.isEnabled();
            return players.isEnabled();
        }
        if (entity instanceof MobEntity) return mobs.isEnabled();
        return false;
    }

    private int resolveColor() {
        if (colorMode.is("Кастом")) return color.getColor().getRGB();
        return Zenith.getInstance().getThemeManager().getClientColor(0).getRGB();
    }

    /**
     * Простой Kawase-like blur: несколько проходов размытия
     */
    private void applyBlur(CustomRenderTarget src, CustomRenderTarget tmp, CustomRenderTarget dst, float radius) {
        int passes = Math.max(1, (int)(radius / 2f));

        // Первый проход: src → tmp
        tmp.setup(true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, src.getColorAttachment());
        drawBlurPass(tmp, radius * 0.5f);
        tmp.stop();

        // Второй проход: tmp → dst
        dst.setup(true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, tmp.getColorAttachment());
        drawBlurPass(dst, radius);
        dst.stop();

        mc.getFramebuffer().beginWrite(false);
    }

    private void drawBlurPass(CustomRenderTarget target, float radius) {
        float w = mw.getWidth();
        float h = mw.getHeight();
        float dx = radius / w;
        float dy = radius / h;

        // Рисуем 4 смещённых копии для имитации blur
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                drawQuadUV(w, h, dx * i, dy * j, 0.25f);
            }
        }
        // Центральный проход с полной яркостью
        drawQuadUV(w, h, 0, 0, 1f);

        RenderSystem.defaultBlendFunc();
    }

    private void drawColoredTexture(int texId, int color, float w, float h, boolean flip) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texId);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 200;

        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        if (flip) {
            buf.vertex(0, h, 0).texture(0, 0).color(r, g, b, a);
            buf.vertex(w, h, 0).texture(1, 0).color(r, g, b, a);
            buf.vertex(w, 0, 0).texture(1, 1).color(r, g, b, a);
            buf.vertex(0, 0, 0).texture(0, 1).color(r, g, b, a);
        } else {
            buf.vertex(0, h, 0).texture(0, 1).color(r, g, b, a);
            buf.vertex(w, h, 0).texture(1, 1).color(r, g, b, a);
            buf.vertex(w, 0, 0).texture(1, 0).color(r, g, b, a);
            buf.vertex(0, 0, 0).texture(0, 0).color(r, g, b, a);
        }
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    /**
     * Обводка = blur - оригинал (рисуем blur поверх, потом вычитаем оригинал через DST_OUT)
     */
    private void drawOutline(int blurTex, int maskTex, int color, float w, float h) {
        // Рисуем blur
        drawColoredTexture(blurTex, color, w, h, true);

        // Вычитаем оригинальную маску чтобы получить только контур
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, maskTex);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(0, h, 0).texture(0, 0).color(255, 255, 255, 255);
        buf.vertex(w, h, 0).texture(1, 0).color(255, 255, 255, 255);
        buf.vertex(w, 0, 0).texture(1, 1).color(255, 255, 255, 255);
        buf.vertex(0, 0, 0).texture(0, 1).color(255, 255, 255, 255);
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.defaultBlendFunc();
    }

    private void drawQuadUV(float w, float h, float offU, float offV, float alpha) {
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int a = (int)(255 * alpha);
        buf.vertex(0, h, 0).texture(offU, offV).color(255, 255, 255, a);
        buf.vertex(w, h, 0).texture(1 + offU, offV).color(255, 255, 255, a);
        buf.vertex(w, 0, 0).texture(1 + offU, 1 + offV).color(255, 255, 255, a);
        buf.vertex(0, 0, 0).texture(offU, 1 + offV).color(255, 255, 255, a);
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private void ensureTargets() {
        if (entityTarget == null) entityTarget = new CustomRenderTarget(true);
        if (blurTarget1  == null) blurTarget1  = new CustomRenderTarget(false).setLinear();
        if (blurTarget2  == null) blurTarget2  = new CustomRenderTarget(false).setLinear();
    }

    private void releaseTargets() {
        if (entityTarget != null) { entityTarget.delete(); entityTarget = null; }
        if (blurTarget1  != null) { blurTarget1.delete();  blurTarget1  = null; }
        if (blurTarget2  != null) { blurTarget2.delete();  blurTarget2  = null; }
    }
}
