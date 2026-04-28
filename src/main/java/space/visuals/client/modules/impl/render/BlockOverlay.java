package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(name = "BlockOverlay", category = Category.RENDER, description = "Подсвечивает блок на который смотришь")
public final class BlockOverlay extends Module {

    public static final BlockOverlay INSTANCE = new BlockOverlay();

    private final BooleanSetting syncWithTheme = new BooleanSetting("Синхронизировать с темой", true);

    private BlockOverlay() {}

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult result)) return;
        if (result.getType() != HitResult.Type.BLOCK) return;
        if (mc.world == null) return;

        BlockPos pos = result.getBlockPos();
        MatrixStack matrices = event.getMatrix();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        Box shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos).isEmpty()
                ? new Box(pos)
                : mc.world.getBlockState(pos).getOutlineShape(mc.world, pos).getBoundingBox().offset(pos);
        Box box = shape.offset(-cam.x, -cam.y, -cam.z);

        renderDefault(matrices, box);
    }

    // ── Default ───────────────────────────────────────────────────────────────

    private void renderDefault(MatrixStack matrices, Box box) {
        ColorRGBA col = syncWithTheme.isEnabled()
                ? Zenith.getInstance().getThemeManager().getCurrentTheme().getColor()
                : new ColorRGBA(151, 71, 255, 255);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder q = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderFilledBox(matrices, q, box, col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, 50f / 255f);
        BufferRenderer.drawWithGlobalProgram(q.end());

        BufferBuilder l = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        renderOutlinedBox(matrices, l, box, col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, 150f / 255f);
        BufferRenderer.drawWithGlobalProgram(l.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void renderFilledBox(MatrixStack ms, BufferBuilder buf, Box box, float r, float g, float b, float a) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x0=(float)box.minX, x1=(float)box.maxX;
        float y0=(float)box.minY, y1=(float)box.maxY;
        float z0=(float)box.minZ, z1=(float)box.maxZ;
        quad(buf,m, x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, r,g,b,a);
        quad(buf,m, x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, r,g,b,a);
        quad(buf,m, x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, r,g,b,a);
        quad(buf,m, x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, r,g,b,a);
        quad(buf,m, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, r,g,b,a);
        quad(buf,m, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, r,g,b,a);
    }

    private static void renderOutlinedBox(MatrixStack ms, BufferBuilder buf, Box box, float r, float g, float b, float a) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x0=(float)box.minX, x1=(float)box.maxX;
        float y0=(float)box.minY, y1=(float)box.maxY;
        float z0=(float)box.minZ, z1=(float)box.maxZ;
        line(buf,m,x0,y0,z0,x1,y0,z0,r,g,b,a); line(buf,m,x1,y0,z0,x1,y0,z1,r,g,b,a);
        line(buf,m,x1,y0,z1,x0,y0,z1,r,g,b,a); line(buf,m,x0,y0,z1,x0,y0,z0,r,g,b,a);
        line(buf,m,x0,y1,z0,x1,y1,z0,r,g,b,a); line(buf,m,x1,y1,z0,x1,y1,z1,r,g,b,a);
        line(buf,m,x1,y1,z1,x0,y1,z1,r,g,b,a); line(buf,m,x0,y1,z1,x0,y1,z0,r,g,b,a);
        line(buf,m,x0,y0,z0,x0,y1,z0,r,g,b,a); line(buf,m,x1,y0,z0,x1,y1,z0,r,g,b,a);
        line(buf,m,x1,y0,z1,x1,y1,z1,r,g,b,a); line(buf,m,x0,y0,z1,x0,y1,z1,r,g,b,a);
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3, float r,float g,float bl,float a) {
        b.vertex(m,x0,y0,z0).color(r,g,bl,a);
        b.vertex(m,x1,y1,z1).color(r,g,bl,a);
        b.vertex(m,x2,y2,z2).color(r,g,bl,a);
        b.vertex(m,x3,y3,z3).color(r,g,bl,a);
    }

    private static void line(BufferBuilder b, Matrix4f m, float x0,float y0,float z0, float x1,float y1,float z1, float r,float g,float bl,float a) {
        float nx=x1-x0, ny=y1-y0, nz=z1-z0;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz); if(len>0){nx/=len;ny/=len;nz/=len;}
        b.vertex(m,x0,y0,z0).color(r,g,bl,a).normal(nx,ny,nz);
        b.vertex(m,x1,y1,z1).color(r,g,bl,a).normal(nx,ny,nz);
    }
}
