package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
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
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(name = "BlockOverlay", category = Category.RENDER, description = "Подсвечивает блок на который смотришь")
public final class BlockOverlay extends Module {

    public static final BlockOverlay INSTANCE = new BlockOverlay();

    private final ModeSetting mode = new ModeSetting("Режим", "Default", "Space");
    private final BooleanSetting syncWithTheme = new BooleanSetting("Синхронизировать с темой", true,
            () -> mode.is("Default"));

    private static final Identifier END_SKY_TEXTURE    = Identifier.ofVanilla("textures/environment/end_sky.png");
    private static final Identifier END_PORTAL_TEXTURE = Identifier.ofVanilla("textures/entity/end_portal.png");

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

        if (mode.is("Space")) {
            renderSpace(matrices, box);
        } else {
            renderDefault(matrices, box);
        }
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

    // ── Space ─────────────────────────────────────────────────────────────────

    private void renderSpace(MatrixStack matrices, Box box) {
        long now = System.currentTimeMillis();
        float time = now * 0.00012f;
        float pulse = (float)(Math.sin(now * 0.0018) * 0.5 + 0.5);

        float glowR = 18f + 16f * pulse;
        float glowG = 24f + 14f * pulse;
        float glowB = 54f + 44f * pulse;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bg = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderFilledBox(matrices, bg, box, 1f/255f, 2f/255f, 6f/255f, 92f/255f);
        renderFilledBox(matrices, bg, box, glowR/255f, glowG/255f, glowB/255f, (25f + pulse * 18f)/255f);
        BufferRenderer.drawWithGlobalProgram(bg.end());

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        renderCosmosTextureLayer(matrices, box, time, END_SKY_TEXTURE, 8, 0.78f, 0.24f, 0.42f, 0.56f, 1.0f, 0.40f);
        renderCosmosTextureLayer(matrices, box, time * 1.35f, END_PORTAL_TEXTURE, 10, 0.62f, 0.47f, 0.80f, 0.88f, 1.0f, 0.34f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder stars = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderVolumeStars(matrices, stars, box, time);
        BufferRenderer.drawWithGlobalProgram(stars.end());

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        float outlineAlpha = (120f + 110f * pulse) / 255f;
        BufferBuilder outline = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        renderOutlinedBox(matrices, outline, box, (130f + pulse * 40f)/255f, (80f + pulse * 30f)/255f, 1f, outlineAlpha);
        BufferRenderer.drawWithGlobalProgram(outline.end());

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
        // 6 faces
        quad(buf,m, x0,y0,z0, x1,y0,z0, x1,y1,z0, x0,y1,z0, r,g,b,a); // -Z
        quad(buf,m, x1,y0,z1, x0,y0,z1, x0,y1,z1, x1,y1,z1, r,g,b,a); // +Z
        quad(buf,m, x0,y0,z1, x0,y0,z0, x0,y1,z0, x0,y1,z1, r,g,b,a); // -X
        quad(buf,m, x1,y0,z0, x1,y0,z1, x1,y1,z1, x1,y1,z0, r,g,b,a); // +X
        quad(buf,m, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0, r,g,b,a); // -Y
        quad(buf,m, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, r,g,b,a); // +Y
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

    // ── Space helpers ─────────────────────────────────────────────────────────

    private static float fract(float v) { return v - (float)Math.floor(v); }
    private static float lerp(float a, float b, float t) { return a + (b-a)*t; }
    private static float clamp(float v, float mn, float mx) { return Math.max(mn, Math.min(mx, v)); }
    private static float hash(int x, int y, int z) {
        int h = x*374761393 + y*668265263 + z*982451653;
        h = (h^(h>>13))*1274126177; h ^= h>>16;
        return (h & 0x7fffffff) / (float)Integer.MAX_VALUE;
    }

    private void renderCosmosTextureLayer(MatrixStack ms, Box box, float time, Identifier tex,
                                          int depthLayers, float uvScale, float uvSpeed,
                                          float tR, float tG, float tB, float baseAlpha) {
        float x0=(float)box.minX, x1=(float)box.maxX;
        float y0=(float)box.minY, y1=(float)box.maxY;
        float z0=(float)box.minZ, z1=(float)box.maxZ;
        float cx=(x0+x1)*.5f, cy=(y0+y1)*.5f, cz=(z0+z1)*.5f;
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int d = 0; d < depthLayers; d++) {
            float t = (d+1f)/(depthLayers+1f);
            float inset = t*0.38f;
            float lx0=lerp(x0,cx,inset), lx1=lerp(x1,cx,inset);
            float ly0=lerp(y0,cy,inset), ly1=lerp(y1,cy,inset);
            float lz0=lerp(z0,cz,inset), lz1=lerp(z1,cz,inset);
            float alpha = baseAlpha*(1f-t*0.74f);
            for (int face = 0; face < 6; face++) {
                float shift = time*uvSpeed*(1f+face*0.11f);
                float u0 = fract(hash(face,d,7)+shift)*(1f-uvScale);
                float v0 = fract(hash(face,d,13)-shift*0.73f)*(1f-uvScale);
                float u1=u0+uvScale, v1=v0+uvScale;
                addFaceQuadTex(buf, mat, face, lx0,lx1,ly0,ly1,lz0,lz1, u0,u1,v0,v1, tR,tG,tB,alpha);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderVolumeStars(MatrixStack ms, BufferBuilder buf, Box box, float time) {
        double w=box.maxX-box.minX, h=box.maxY-box.minY, d=box.maxZ-box.minZ;
        double minDim = Math.min(w, Math.min(h, d));
        Matrix4f mat = ms.peek().getPositionMatrix();
        for (int i = 0; i < 110; i++) {
            float drift = time*(0.20f+hash(i,7,11)*0.55f);
            double px = box.minX + w*fract(hash(i,23,37)+drift*0.82f);
            double py = box.minY + h*fract(hash(i,53,73)+drift*0.57f);
            double pz = box.minZ + d*fract(hash(i,89,101)+drift*0.69f);
            float twinkle = 0.35f+0.65f*(float)Math.sin(time*14f+hash(i,131,173)*30f);
            double size = minDim*(0.0045+hash(i,149,181)*0.010);
            float tint = hash(i,191,211);
            float r=lerp(170f,255f,tint)/255f, g=lerp(190f,255f,tint)/255f, b=lerp(220f,255f,tint)/255f;
            float a=(22f+80f*twinkle)/255f;
            float sx0=(float)(px-size), sx1=(float)(px+size);
            float sy0=(float)(py-size), sy1=(float)(py+size);
            float sz0=(float)(pz-size), sz1=(float)(pz+size);
            quad(buf,mat, sx0,sy0,sz0, sx1,sy0,sz0, sx1,sy1,sz0, sx0,sy1,sz0, r,g,b,a);
            quad(buf,mat, sx0,sy0,sz1, sx1,sy0,sz1, sx1,sy1,sz1, sx0,sy1,sz1, r,g,b,a);
        }
    }

    private static void addFaceQuadTex(BufferBuilder buf, Matrix4f mat, int face,
                                       float x0, float x1, float y0, float y1, float z0, float z1,
                                       float u0, float u1, float v0, float v1,
                                       float r, float g, float b, float a) {
        switch (face) {
            case 0 -> { float z=z0+.0008f; buf.vertex(mat,x0,y0,z).texture(u0,v1).color(r,g,b,a); buf.vertex(mat,x1,y0,z).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x1,y1,z).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x0,y1,z).texture(u0,v0).color(r,g,b,a); }
            case 1 -> { float z=z1-.0008f; buf.vertex(mat,x1,y0,z).texture(u0,v1).color(r,g,b,a); buf.vertex(mat,x0,y0,z).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x0,y1,z).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x1,y1,z).texture(u0,v0).color(r,g,b,a); }
            case 2 -> { float x=x0+.0008f; buf.vertex(mat,x,y0,z1).texture(u0,v1).color(r,g,b,a); buf.vertex(mat,x,y0,z0).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x,y1,z0).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x,y1,z1).texture(u0,v0).color(r,g,b,a); }
            case 3 -> { float x=x1-.0008f; buf.vertex(mat,x,y0,z0).texture(u0,v1).color(r,g,b,a); buf.vertex(mat,x,y0,z1).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x,y1,z1).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x,y1,z0).texture(u0,v0).color(r,g,b,a); }
            case 4 -> { float y=y0+.0008f; buf.vertex(mat,x0,y,z0).texture(u0,v0).color(r,g,b,a); buf.vertex(mat,x1,y,z0).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x1,y,z1).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x0,y,z1).texture(u0,v1).color(r,g,b,a); }
            default -> { float y=y1-.0008f; buf.vertex(mat,x0,y,z1).texture(u0,v1).color(r,g,b,a); buf.vertex(mat,x1,y,z1).texture(u1,v1).color(r,g,b,a); buf.vertex(mat,x1,y,z0).texture(u1,v0).color(r,g,b,a); buf.vertex(mat,x0,y,z0).texture(u0,v0).color(r,g,b,a); }
        }
    }
}
