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
            // Используем остаток от деления чтобы избежать переполнения float
            float hue = ((System.currentTimeMillis() % 2000L) / 2000f + hueOffset) % 1f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
            return new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
        }
        if (colorMode.get().equals("Кастом")) {
            return customColor.getColor();
        }
        return space.visuals.Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
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

        // Quad strip (лента)
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        for (int i = 0; i < total; i++) {
            float t = (float) i / total;
            float hueOff = colorMode.get().equals("Радуга") ? t * 0.3f : 0f;
            ColorRGBA c = getTrailColor(hueOff);
            float r = c.getRed()   / 255f;
            float g = c.getGreen() / 255f;
            float b = c.getBlue()  / 255f;
            float rB = Math.min(r + 0.3f * blinkFactor, 1f);
            float gB = Math.min(g + 0.3f * blinkFactor, 1f);
            float bB = Math.min(b + 0.3f * blinkFactor, 1f);
            float alpha = t * 0.7f;
            Vec3d pos = points.get(i).pos.subtract(cam);
            buf.vertex(m, (float)pos.x, (float)(pos.y + mc.player.getHeight()), (float)pos.z).color(rB, gB, bB, alpha);
            buf.vertex(m, (float)pos.x, (float)pos.y, (float)pos.z).color(rB, gB, bB, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Верхняя и нижняя линии
        renderLine(ms, cam, true,  blinkFactor);
        renderLine(ms, cam, false, blinkFactor);

        ms.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderLine(MatrixStack ms, Vec3d cam, boolean top, float blinkFactor) {
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        int total = points.size();
        for (int i = 0; i < total; i++) {
            float t = (float) i / total;
            float hueOff = colorMode.get().equals("Радуга") ? t * 0.3f : 0f;
            ColorRGBA c = getTrailColor(hueOff);
            float r = c.getRed()   / 255f;
            float g = c.getGreen() / 255f;
            float b = c.getBlue()  / 255f;
            float rB = Math.min(r + 0.3f * blinkFactor, 1f);
            float gB = Math.min(g + 0.3f * blinkFactor, 1f);
            float bB = Math.min(b + 0.3f * blinkFactor, 1f);
            float alpha = Math.min(t * 1.5f, 1f);
            Vec3d pos = points.get(i).pos.subtract(cam);
            float yOff = top ? (float) mc.player.getHeight() : 0f;
            buf.vertex(m, (float)pos.x, (float)pos.y + yOff, (float)pos.z)
               .color(MathHelper.lerp(blinkFactor, r, rB),
                      MathHelper.lerp(blinkFactor, g, gB),
                      MathHelper.lerp(blinkFactor, b, bB),
                      alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
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
