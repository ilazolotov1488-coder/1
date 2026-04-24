package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "Trails", category = Category.RENDER, description = "Создаёт плавную линию ходьбы")
public final class Trails extends Module {
    public static final Trails INSTANCE = new Trails();

    private final NumberSetting trailLength = new NumberSetting("Длина (сек)", 2f, 0.5f, 5f, 0.5f);

    // Режим цвета
    private final ModeSetting colorMode = new ModeSetting("Цвет", "Тема", "Кастом", "Радуга");

    // Кастомный цвет (виден только в режиме Кастом)
    private final ColorSetting customColor = new ColorSetting("Кастом цвет",
            new ColorRGBA(108, 99, 210, 255),
            () -> colorMode.get().equals("Кастом"));

    private final List<Point> points = new ArrayList<>();
    private static final float BLINK_SPEED = 0.002f;

    private Trails() {}

    private ColorRGBA getTrailColor(float hueOffset) {
        if (colorMode.get().equals("Радуга")) {
            float hue = ((System.currentTimeMillis() % 2000L) / 2000f + hueOffset) % 1f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
            return new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
        }
        if (colorMode.get().equals("Кастом")) {
            return customColor.getColor();
        }
        return space.visuals.Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
    }

    // Кэш цвета темы — обновляем раз в кадр, не создаём объект на каждый вертекс
    private ColorRGBA cachedThemeColor = null;
    private long cachedThemeColorFrame = -1;

    private ColorRGBA getTrailColorCached(float hueOffset) {
        if (colorMode.get().equals("Радуга")) {
            float hue = ((System.currentTimeMillis() % 2000L) / 2000f + hueOffset) % 1f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
            return new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
        }
        if (colorMode.get().equals("Кастом")) return customColor.getColor();
        long frame = mc.getRenderTickCounter() != null ? System.currentTimeMillis() / 16 : 0;
        if (cachedThemeColor == null || frame != cachedThemeColorFrame) {
            cachedThemeColor = space.visuals.Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
            cachedThemeColorFrame = frame;
        }
        return cachedThemeColor;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        long now = System.currentTimeMillis();
        long maxAge = (long)(trailLength.getCurrent() * 1000f);
        points.removeIf(p -> (now - p.time) > maxAge);

        float pt = event.getPartialTicks();
        Vec3d playerPos = new Vec3d(
                MathHelper.lerp(pt, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(pt, mc.player.prevY, mc.player.getY()),
                MathHelper.lerp(pt, mc.player.prevZ, mc.player.getZ())
        );
        points.add(new Point(playerPos));

        if (points.size() < 2) return;

        render(event.getMatrix());
    }

    private void render(MatrixStack ms) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        ms.push();

        float blinkFactor = (float)(Math.sin(System.currentTimeMillis() * BLINK_SPEED) * 0.5 + 0.5);
        int total = points.size();
        boolean isRainbow = colorMode.get().equals("Радуга");

        // Предвычисляем цвета один раз для всех точек
        float[] rs = new float[total], gs = new float[total], bs = new float[total];
        for (int i = 0; i < total; i++) {
            float t = (float) i / total;
            float hueOff = isRainbow ? t * 0.3f : 0f;
            ColorRGBA c = getTrailColorCached(hueOff);
            rs[i] = Math.min(c.getRed()   / 255f + 0.3f * blinkFactor, 1f);
            gs[i] = Math.min(c.getGreen() / 255f + 0.3f * blinkFactor, 1f);
            bs[i] = Math.min(c.getBlue()  / 255f + 0.3f * blinkFactor, 1f);
        }

        Matrix4f m = ms.peek().getPositionMatrix();
        float playerH = (float) mc.player.getHeight();

        // Quad strip (лента)
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < total; i++) {
            float alpha = (float) i / total * 0.7f;
            Vec3d pos = points.get(i).pos.subtract(cam);
            buf.vertex(m, (float)pos.x, (float)(pos.y + playerH), (float)pos.z).color(rs[i], gs[i], bs[i], alpha);
            buf.vertex(m, (float)pos.x, (float)pos.y,             (float)pos.z).color(rs[i], gs[i], bs[i], alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Верхняя линия
        buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < total; i++) {
            float alpha = Math.min((float) i / total * 1.5f, 1f);
            Vec3d pos = points.get(i).pos.subtract(cam);
            buf.vertex(m, (float)pos.x, (float)pos.y + playerH, (float)pos.z).color(rs[i], gs[i], bs[i], alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Нижняя линия
        buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < total; i++) {
            float alpha = Math.min((float) i / total * 1.5f, 1f);
            Vec3d pos = points.get(i).pos.subtract(cam);
            buf.vertex(m, (float)pos.x, (float)pos.y, (float)pos.z).color(rs[i], gs[i], bs[i], alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        ms.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Override
    public void onDisable() {
        points.clear();
        super.onDisable();
    }

    private static class Point {
        final Vec3d pos;
        final long time;
        Point(Vec3d pos) { this.pos = pos; this.time = System.currentTimeMillis(); }
    }
}
