package space.visuals.client.modules.impl.combat;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.utility.render.level.Render3DUtil;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(name = "ItemRadius", category = Category.COMBAT, description = "Показывает радиус действия предметов")
public final class ItemRadius extends Module {

    public static final ItemRadius INSTANCE = new ItemRadius();

    private final BooleanSetting deska     = new BooleanSetting("Дезка", true);
    private final BooleanSetting yavka     = new BooleanSetting("Явка", true);
    private final BooleanSetting fireCharge = new BooleanSetting("Огненный Заряд", true);
    private final BooleanSetting godAura   = new BooleanSetting("Божья Аура", true);
    private final BooleanSetting trapka    = new BooleanSetting("Трапка", true);
    private final BooleanSetting plast     = new BooleanSetting("Пласт", true);

    // color transition state
    private int   currentFillColor    = 0x55005500;
    private int   currentOutlineColor = 0xFF005500;
    private int   targetFillColor     = 0x55005500;
    private int   targetOutlineColor  = 0xFF005500;
    private float transitionTimer     = 0.0f;
    private boolean lastPlayersInRadius = false;
    private static final float TRANSITION_DURATION = 0.5f;

    private ItemRadius() {}

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null) return;
        try {
            var main = mc.player.getMainHandStack();
            var off  = mc.player.getOffHandStack();
            Vec3d playerPos = mc.player.getPos();
            Vec3d centerPos = playerPos.add(0, -1.4, 0);
            float dt = Render3DUtil.getTickDelta();

            if (deska.isEnabled() && (main.getItem() == Items.ENDER_EYE || off.getItem() == Items.ENDER_EYE)) {
                boolean inRadius = checkPlayersInRadius(centerPos, 10.0);
                updateColors(inRadius, 0x55005500, 0xFF005500, 0x5500AA00, 0xFF00AA00, dt);
                renderRadius(event, 10.0f, currentFillColor, currentOutlineColor);
                return;
            }
            if (yavka.isEnabled() && (main.getItem() == Items.SUGAR || off.getItem() == Items.SUGAR)) {
                boolean inRadius = checkPlayersInRadius(centerPos, 10.0);
                updateColors(inRadius, 0x55999999, 0xFF999999, 0x55FFFFFF, 0xFFFFFFFF, dt);
                renderRadius(event, 10.0f, currentFillColor, currentOutlineColor);
                return;
            }
            if (fireCharge.isEnabled() && (main.getItem() == Items.FIRE_CHARGE || off.getItem() == Items.FIRE_CHARGE)) {
                boolean inRadius = checkPlayersInRadius(centerPos, 10.0);
                updateColors(inRadius, 0x55550000, 0xFF550000, 0x55AA0000, 0xFFAA0000, dt);
                renderRadius(event, 10.0f, currentFillColor, currentOutlineColor);
                return;
            }
            if (godAura.isEnabled() && (main.getItem() == Items.PHANTOM_MEMBRANE || off.getItem() == Items.PHANTOM_MEMBRANE)) {
                boolean inRadius = checkPlayersInRadius(centerPos, 2.0);
                updateColors(inRadius, 0x55009999, 0xFF009999, 0x5500FFFF, 0xFF00FFFF, dt);
                renderRadius(event, 2.0f, currentFillColor, currentOutlineColor);
                return;
            }
            if (trapka.isEnabled() && (main.getItem() == Items.NETHERITE_SCRAP || off.getItem() == Items.NETHERITE_SCRAP)) {
                renderCube(event, 0x558B4513, 0xFF8B4513);
                return;
            }
            if (plast.isEnabled() && (main.getItem() == Items.DRIED_KELP || off.getItem() == Items.DRIED_KELP)) {
                renderPlane(event, 0x55333333, 0xFF333333);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateColors(boolean inRadius, int baseFill, int baseOutline, int lightFill, int lightOutline, float dt) {
        if (inRadius != lastPlayersInRadius) { transitionTimer = 0.0f; lastPlayersInRadius = inRadius; }
        targetFillColor    = inRadius ? lightFill    : baseFill;
        targetOutlineColor = inRadius ? lightOutline : baseOutline;
        transitionTimer = Math.min(transitionTimer + dt / TRANSITION_DURATION, 1.0f);
        currentFillColor    = lerpColor(currentFillColor,    targetFillColor,    transitionTimer);
        currentOutlineColor = lerpColor(currentOutlineColor, targetOutlineColor, transitionTimer);
    }

    private int lerpColor(int s, int e, float t) {
        int sA=(s>>24)&0xFF, sR=(s>>16)&0xFF, sG=(s>>8)&0xFF, sB=s&0xFF;
        int eA=(e>>24)&0xFF, eR=(e>>16)&0xFF, eG=(e>>8)&0xFF, eB=e&0xFF;
        return (((int)(sA+(eA-sA)*t))<<24)|(((int)(sR+(eR-sR)*t))<<16)|(((int)(sG+(eG-sG)*t))<<8)|((int)(sB+(eB-sB)*t));
    }

    private boolean checkPlayersInRadius(Vec3d center, double radius) {
        if (mc.world == null || mc.player == null) return false;
        Box box = new Box(center.x-radius, center.y-radius, center.z-radius, center.x+radius, center.y+radius, center.z+radius);
        for (PlayerEntity p : mc.world.getEntitiesByClass(PlayerEntity.class, box, e -> e != mc.player)) {
            if (p.getPos().distanceTo(center) <= radius) return true;
        }
        return false;
    }

    private void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(3.0f);
    }

    private void teardownRender() {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void renderRadius(EventRender3D event, float radius, int fillColor, int outlineColor) {
        if (mc.player == null) return;
        MatrixStack matrices = event.getMatrix();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getPos().add(0, -1.4, 0);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        setupRender();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(matrices.peek().getPositionMatrix(), (float)pos.x, (float)(pos.y + mc.player.getHeight()), (float)pos.z).color(fillColor);
        for (int z = 0; z <= 360; z += 5) {
            float x  = (float)(pos.x + MathHelper.sin((float)Math.toRadians(z)) * radius);
            float zz = (float)(pos.z - MathHelper.cos((float)Math.toRadians(z)) * radius);
            buf.vertex(matrices.peek().getPositionMatrix(), x, (float)(pos.y + mc.player.getHeight()), zz).color(fillColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int z = 0; z <= 360; z += 5) {
            float x  = (float)(pos.x + MathHelper.sin((float)Math.toRadians(z)) * radius);
            float zz = (float)(pos.z - MathHelper.cos((float)Math.toRadians(z)) * radius);
            buf.vertex(matrices.peek().getPositionMatrix(), x, (float)(pos.y + mc.player.getHeight()), zz).color(outlineColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        teardownRender();
        matrices.pop();
    }

    private void renderCube(EventRender3D event, int fillColor, int outlineColor) {
        if (mc.player == null) return;
        MatrixStack matrices = event.getMatrix();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d pos = mc.player.getPos();
        double cx = Math.floor(pos.x) + 0.5 - cam.x;
        double cy = Math.floor(pos.y) + 0.5 + 1.625 - cam.y;
        double cz = Math.floor(pos.z) + 0.5 - cam.z;

        matrices.push();
        setupRender();

        float h = 2.0f, hw = 2.5f;
        float[][] v = {
            {(float)(cx-hw),(float)(cy-h),(float)(cz-hw)}, {(float)(cx+hw),(float)(cy-h),(float)(cz-hw)},
            {(float)(cx+hw),(float)(cy+h),(float)(cz-hw)}, {(float)(cx-hw),(float)(cy+h),(float)(cz-hw)},
            {(float)(cx-hw),(float)(cy-h),(float)(cz+hw)}, {(float)(cx+hw),(float)(cy-h),(float)(cz+hw)},
            {(float)(cx+hw),(float)(cy+h),(float)(cz+hw)}, {(float)(cx-hw),(float)(cy+h),(float)(cz+hw)}
        };
        int[][] faces = {{0,1,2,3},{5,4,7,6},{4,5,1,0},{3,2,6,7},{4,0,3,7},{1,5,6,2}};
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int[] f : faces) {
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[0]][0], v[f[0]][1], v[f[0]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[1]][0], v[f[1]][1], v[f[1]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[2]][0], v[f[2]][1], v[f[2]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[0]][0], v[f[0]][1], v[f[0]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[2]][0], v[f[2]][1], v[f[2]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[f[3]][0], v[f[3]][1], v[f[3]][2]).color(fillColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int[] e : edges) {
            buf.vertex(matrices.peek().getPositionMatrix(), v[e[0]][0], v[e[0]][1], v[e[0]][2]).color(outlineColor);
            buf.vertex(matrices.peek().getPositionMatrix(), v[e[1]][0], v[e[1]][1], v[e[1]][2]).color(outlineColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        teardownRender();
        matrices.pop();
    }

    private void renderPlane(EventRender3D event, int fillColor, int outlineColor) {
        if (mc.player == null || mc.world == null) return;
        MatrixStack matrices = event.getMatrix();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float dt = Render3DUtil.getTickDelta();
        float pitch = mc.player.getPitch(dt);
        float yaw   = mc.player.getYaw(dt);
        boolean lookingDown  = pitch > 45.0f;
        boolean lookingUp    = pitch < -45.0f;
        boolean lookingHoriz = !lookingDown && !lookingUp;

        Vec3d eyePos  = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(dt);
        Vec3d end     = eyePos.add(lookVec.multiply(4.0));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, end,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        float width = 4.0f, height = 4.0f, thickness = 1.5f;
        float hw = width / 2, hh = height / 2, ht = thickness / 2;

        double yawRad = Math.toRadians(yaw);
        double rightX =  Math.cos(yawRad);
        double rightZ =  Math.sin(yawRad);
        double fwdX = -Math.sin(yawRad);
        double fwdZ =  Math.cos(yawRad);

        Vec3d planePos;
        if (hit.getType() == HitResult.Type.BLOCK && hit.getPos().distanceTo(eyePos) <= 4.0) {
            Vec3d hp = hit.getPos();
            if (lookingDown) {
                planePos = new Vec3d(Math.floor(hp.x)+0.5, Math.floor(hp.y+1.0)-1.8+ht, Math.floor(hp.z)+0.5);
            } else if (lookingUp) {
                planePos = new Vec3d(Math.floor(hp.x)+0.5, Math.floor(hp.y)-ht+1.6, Math.floor(hp.z)+0.5);
            } else {
                double ox = hit.getSide().getOffsetX() != 0 ? hit.getSide().getOffsetX() * ht : 0;
                double oz = hit.getSide().getOffsetZ() != 0 ? hit.getSide().getOffsetZ() * ht : 0;
                planePos = new Vec3d(Math.floor(hp.x)+0.5+ox, Math.floor(hp.y)+0.5+1.6, Math.floor(hp.z)+0.5+oz);
            }
        } else {
            Vec3d p = eyePos.add(lookVec.multiply(4.0));
            double ay = Math.floor(p.y) + (lookingDown ? -1.8+ht : lookingUp ? -ht+1.6 : 0.5+1.6);
            planePos = new Vec3d(Math.floor(p.x)+0.5, ay, Math.floor(p.z)+0.5);
        }

        double cx = planePos.x - cam.x;
        double cy = planePos.y - cam.y;
        double cz = planePos.z - cam.z;

        float[][] verts;
        if (lookingHoriz) {
            verts = new float[][] {
                {(float)(cx - rightX*hw - ht*fwdX), (float)(cy - hh), (float)(cz - rightZ*hw - ht*fwdZ)},
                {(float)(cx + rightX*hw - ht*fwdX), (float)(cy - hh), (float)(cz + rightZ*hw - ht*fwdZ)},
                {(float)(cx + rightX*hw - ht*fwdX), (float)(cy + hh), (float)(cz + rightZ*hw - ht*fwdZ)},
                {(float)(cx - rightX*hw - ht*fwdX), (float)(cy + hh), (float)(cz - rightZ*hw - ht*fwdZ)},
                {(float)(cx - rightX*hw + ht*fwdX), (float)(cy - hh), (float)(cz - rightZ*hw + ht*fwdZ)},
                {(float)(cx + rightX*hw + ht*fwdX), (float)(cy - hh), (float)(cz + rightZ*hw + ht*fwdZ)},
                {(float)(cx + rightX*hw + ht*fwdX), (float)(cy + hh), (float)(cz + rightZ*hw + ht*fwdZ)},
                {(float)(cx - rightX*hw + ht*fwdX), (float)(cy + hh), (float)(cz - rightZ*hw + ht*fwdZ)}
            };
        } else {
            verts = new float[][] {
                {(float)(cx - rightX*hw - fwdX*hh), (float)(cy - ht), (float)(cz - rightZ*hw - fwdZ*hh)},
                {(float)(cx + rightX*hw - fwdX*hh), (float)(cy - ht), (float)(cz + rightZ*hw - fwdZ*hh)},
                {(float)(cx + rightX*hw - fwdX*hh), (float)(cy + ht), (float)(cz + rightZ*hw - fwdZ*hh)},
                {(float)(cx - rightX*hw - fwdX*hh), (float)(cy + ht), (float)(cz - rightZ*hw - fwdZ*hh)},
                {(float)(cx - rightX*hw + fwdX*hh), (float)(cy - ht), (float)(cz - rightZ*hw + fwdZ*hh)},
                {(float)(cx + rightX*hw + fwdX*hh), (float)(cy - ht), (float)(cz + rightZ*hw + fwdZ*hh)},
                {(float)(cx + rightX*hw + fwdX*hh), (float)(cy + ht), (float)(cz + rightZ*hw + fwdZ*hh)},
                {(float)(cx - rightX*hw + fwdX*hh), (float)(cy + ht), (float)(cz - rightZ*hw + fwdZ*hh)}
            };
        }
        int[][] faces = {{0,1,2,3},{5,4,7,6},{4,5,1,0},{3,2,6,7},{4,0,3,7},{1,5,6,2}};
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};

        matrices.push();
        setupRender();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int[] f : faces) {
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[0]][0], verts[f[0]][1], verts[f[0]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[1]][0], verts[f[1]][1], verts[f[1]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[2]][0], verts[f[2]][1], verts[f[2]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[0]][0], verts[f[0]][1], verts[f[0]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[2]][0], verts[f[2]][1], verts[f[2]][2]).color(fillColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[f[3]][0], verts[f[3]][1], verts[f[3]][2]).color(fillColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int[] e : edges) {
            buf.vertex(matrices.peek().getPositionMatrix(), verts[e[0]][0], verts[e[0]][1], verts[e[0]][2]).color(outlineColor);
            buf.vertex(matrices.peek().getPositionMatrix(), verts[e[1]][0], verts[e[1]][1], verts[e[1]][2]).color(outlineColor);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        teardownRender();
        matrices.pop();
    }
}
