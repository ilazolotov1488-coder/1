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
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "Trails", category = Category.RENDER, description = "Создаёт плавную линию ходьбы")
public final class Trails extends Module {
    public static final Trails INSTANCE = new Trails();

    private final NumberSetting trailLength = new NumberSetting("Длина (сек)", 2f, 0.5f, 5f, 0.5f);

    private final List<Point> points = new ArrayList<>();
    private static final float BLINK_SPEED = 0.002f;

    private Trails() {}

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
        ColorRGBA theme = space.visuals.Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        ms.push();

        float blinkFactor = (float)(Math.sin(System.currentTimeMillis() * BLINK_SPEED) * 0.5 + 0.5);
        float r = theme.getRed()   / 255f;
        float g = theme.getGreen() / 255f;
        float b = theme.getBlue()  / 255f;
        float rB = Math.min(r + 0.3f * blinkFactor, 1f);
        float gB = Math.min(g + 0.3f * blinkFactor, 1f);
        float bB = Math.min(b + 0.3f * blinkFactor, 1f);

        // Quad strip (лента)
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        int total = points.size();
        for (int i = 0; i < total; i++) {
            float alpha = (float) i / total * 0.7f;
            Vec3d pos = points.get(i).pos.subtract(cam);
            buf.vertex(m, (float)pos.x, (float)(pos.y + mc.player.getHeight()), (float)pos.z).color(rB, gB, bB, alpha);
            buf.vertex(m, (float)pos.x, (float)pos.y, (float)pos.z).color(rB, gB, bB, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Верхняя линия
        renderLine(ms, cam, true, r, g, b, rB, gB, bB);
        // Нижняя линия
        renderLine(ms, cam, false, r, g, b, rB, gB, bB);

        ms.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderLine(MatrixStack ms, Vec3d cam, boolean top,
                            float r, float g, float b, float rB, float gB, float bB) {
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        int total = points.size();
        float blinkFactor = (float)(Math.sin(System.currentTimeMillis() * BLINK_SPEED) * 0.5 + 0.5);
        for (int i = 0; i < total; i++) {
            float alpha = Math.min((float) i / total * 1.5f, 1f);
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
