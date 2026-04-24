package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleAnnotation(name = "TargetESP2", category = Category.RENDER, description = "Визуальный эффект на текущей цели (polyak)")
public final class TargetESP2 extends Module {
    public static final TargetESP2 INSTANCE = new TargetESP2();

    private static final Identifier GLOW_TEXTURE = Zenith.id("textures/bloom.png");

    private final ModeSetting mode = new ModeSetting("Режим",
            "Ghost", "Crystals", "Crystals2", "Cubes", "Cubes2", "Prizraki", "Kolco");

    private final NumberSetting ghostSpeed   = new NumberSetting("Скорость призраков", 1.0f, 0.5f, 2.0f, 0.1f);
    private final NumberSetting prizrakiSize = new NumberSetting("Размер призраков",   0.4f, 0.1f, 1.0f, 0.05f);

    // Cubes2 settings (наши кубы из TargetESP)
    private final NumberSetting cubes2Speed  = new NumberSetting("Скорость (Cubes2)",  0.5f, 0.1f, 5.0f, 0.1f, () -> mode.is("Cubes2"));
    private final NumberSetting cubes2Size   = new NumberSetting("Размер (Cubes2)",    1.5f, 0.5f, 3.0f, 0.1f, () -> mode.is("Cubes2"));
    private final NumberSetting cubes2Count  = new NumberSetting("Кол-во (Cubes2)",   20f,  1f,  50f,  1f,   () -> mode.is("Cubes2"));

    private final NumberSetting  kolcoSpeed       = new NumberSetting("Скорость кольца",    0.01f, 0.005f, 0.3f, 0.005f);
    private final BooleanSetting kolcoRainbow      = new BooleanSetting("Радуга кольца",      true);
    private final BooleanSetting kolcoDamageRed    = new BooleanSetting("Красный при уроне",  true);

    // Animations
    private final Animation animation    = new Animation(3000L, 0f, Easing.EXPO_OUT);
    private final Animation crystalsAnim = new Animation(350L, 0f, Easing.EXPO_OUT);

    private Entity lastTarget       = null;
    private Entity crystalsLastTarget = null;
    private long   targetLostTime   = 0L;

    // Ghost
    private float ghostAnimTime      = 0f;
    private long  lastGhostTimestamp = 0L;

    // Prizraki
    private long lastPrizrakTime = System.currentTimeMillis();

    // Kolco
    private long  lastKolcoUpdateTime  = 0L;
    private double kolcoStep           = 0;
    private float  kolcoAnimProgress   = 0f;
    private long   lastDamageTime      = 0;
    private float  damageFlashIntensity = 0f;
    private static final long DAMAGE_FLASH_DURATION = 500;

    // Cubes
    private final ArrayList<CubeParticle> particles = new ArrayList<>();
    private static final int   PARTICLES_PER_SPAWN = 1;
    private static final float SPAWN_INTERVAL      = 0.017f;
    private float spawnAccumulator = 0f;
    private long  lastCubeTime     = 0L;

    private TargetESP2() {}

    // ── Target detection ──────────────────────────────────────────────────────

    private LivingEntity getTarget() {
        if (mc.player == null || mc.world == null) return null;
        var hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return null;
        var entity = ((EntityHitResult) hit).getEntity();
        if (!(entity instanceof LivingEntity living)) return null;
        if (living == mc.player || !living.isAlive()) return null;
        if (living instanceof ArmorStandEntity) return null;
        if (mc.player.distanceTo(living) > 6.0) return null;
        return living;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices  = event.getMatrix();
        float       tickDelta = event.getPartialTicks();
        Camera      camera    = mc.gameRenderer.getCamera();

        LivingEntity target = getTarget();

        if (target != null) {
            lastTarget = target;
            animation.update(true);
            crystalsAnim.update(true);
            targetLostTime = 0L;
        } else {
            if (targetLostTime == 0L) targetLostTime = System.currentTimeMillis();
            boolean delayPassed = System.currentTimeMillis() - targetLostTime >= 3000L;
            if (delayPassed) {
                animation.update(false);
                crystalsAnim.update(false);
                if (animation.getValue() == 0f) lastTarget = null;
            }
        }

        float partialTicks = tickDelta;

        if (target != null && lastTarget != null) {
            float animVal = animation.getValue();
            switch (mode.get()) {
                case "Ghost"     -> renderGhost(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                case "Crystals"  -> {
                    crystalsLastTarget = lastTarget;
                    float anim = crystalsAnim.getValue();
                    if (anim > 0.001f) renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, false, anim, tickDelta);
                }
                case "Crystals2" -> {
                    crystalsLastTarget = lastTarget;
                    float anim = crystalsAnim.getValue();
                    if (anim > 0.001f) renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, true, anim, tickDelta);
                }
                case "Cubes"     -> renderCubes(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                case "Cubes2"    -> renderCubes2(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                case "Prizraki"  -> renderPrizraki(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                case "Kolco"     -> {
                    kolcoAnimProgress = Math.min(1f, kolcoAnimProgress + 0.1f);
                    if (lastTarget instanceof LivingEntity living && living.hurtTime > 0) lastDamageTime = System.currentTimeMillis();
                    long now = System.currentTimeMillis();
                    if (lastKolcoUpdateTime == 0L) lastKolcoUpdateTime = now;
                    float dt = Math.min((now - lastKolcoUpdateTime) / 1000f, 0.1f);
                    lastKolcoUpdateTime = now;
                    kolcoStep += kolcoSpeed.getCurrent() * dt * 60.0;
                    renderKolco(matrices, camera, (LivingEntity) lastTarget, tickDelta, kolcoAnimProgress * animVal);
                }
            }
        } else {
            // Плавное исчезновение эффектов — 3 секунды задержки, потом анимация
            float animVal = animation.getValue();
            float crystalAnim = crystalsAnim.getValue();

            if (animVal > 0f && lastTarget != null) {
                switch (mode.get()) {
                    case "Ghost"    -> renderGhost(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                    case "Prizraki" -> renderPrizraki(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                    case "Cubes2"   -> renderCubes2(matrices, camera, (LivingEntity) lastTarget, tickDelta, animVal);
                    case "Crystals" -> {
                        if (crystalAnim > 0.001f && crystalsLastTarget != null)
                            renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, false, crystalAnim, tickDelta);
                    }
                    case "Crystals2" -> {
                        if (crystalAnim > 0.001f && crystalsLastTarget != null)
                            renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, true, crystalAnim, tickDelta);
                    }
                    case "Kolco" -> {
                        float kolcoVal = kolcoAnimProgress * animVal;
                        if (kolcoVal > 0 && lastTarget != null) {
                            long now = System.currentTimeMillis();
                            if (lastKolcoUpdateTime == 0L) lastKolcoUpdateTime = now;
                            float dt = Math.min((now - lastKolcoUpdateTime) / 1000f, 0.1f);
                            lastKolcoUpdateTime = now;
                            kolcoStep += kolcoSpeed.getCurrent() * dt * 60.0;
                            renderKolco(matrices, camera, (LivingEntity) lastTarget, tickDelta, kolcoVal);
                        }
                    }
                }
            } else {
                lastGhostTimestamp = 0L;
                ghostAnimTime      = 0f;
                lastCubeTime       = 0L;
                spawnAccumulator   = 0f;
                kolcoAnimProgress  = 0f;
                crystalsLastTarget = null;
                lastKolcoUpdateTime = 0L;
            }

            if (mode.is("Cubes") && !particles.isEmpty())
                renderCubes(matrices, camera, null, tickDelta, animVal);
        }
    }
    // -- Ghost -----------------------------------------------------------------

    private void renderGhost(MatrixStack matrices, Camera camera, LivingEntity target, float pt, float alphaMul) {
        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw(), camPitch = camera.getPitch();
        double tx = MathHelper.lerp(pt, target.prevX, target.getX());
        double ty = MathHelper.lerp(pt, target.prevY, target.getY());
        double tz = MathHelper.lerp(pt, target.prevZ, target.getZ());
        double x = tx - camPos.x, y = ty - camPos.y, z = tz - camPos.z;
        long now = System.currentTimeMillis();
        if (lastGhostTimestamp == 0) lastGhostTimestamp = now;
        ghostAnimTime += (4f * (now - lastGhostTimestamp) / 600f) * ghostSpeed.getCurrent();
        lastGhostTimestamp = now;
        float[] rgb = themeRgb();
        float r = rgb[0], g = rgb[1], b = rgb[2];
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean has = false;
        for (int ring = 0; ring < 9; ring += 3) {
            for (int si = 0; si < 25; si++) {
                float ao = si * 0.1f, ro = (float) Math.pow(ring, 2.0);
                double sx = x + 1.3f * 0.6f * MathHelper.sin(ghostAnimTime + ao + ro);
                double sy = y + 0.4f + 1.3f * 0.3f * MathHelper.sin(ghostAnimTime + si * 0.2f) + 0.2f * ring;
                double sz = z + 1.3f * 0.6f * MathHelper.cos(ghostAnimTime + ao - ro);
                float progress = (float) si / 24f;
                float scale = 0.5f * (0.2f + progress * 0.8f);
                float alpha = (1.0f - progress * 0.9f) * alphaMul;
                if (scale < 0.01f || alpha < 0.01f) continue;
                float cr = Math.min(1f, r * 1.5f), cg = Math.min(1f, g * 1.5f), cb = Math.min(1f, b * 1.5f);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 4.0f, r, g, b, alpha * 0.15f, camYaw, camPitch);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 2.0f, r, g, b, alpha * 0.35f, camYaw, camPitch);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 0.8f, cr, cg, cb, alpha, camYaw, camPitch);
                has = true;
            }
        }
        if (has) BufferRenderer.drawWithGlobalProgram(builder.end());
        restoreRenderState();
    }

    private void renderPrizraki(MatrixStack matrices, Camera camera, LivingEntity target, float pt, float alphaMul) {
        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw(), camPitch = camera.getPitch();
        double tx = MathHelper.lerp(pt, target.prevX, target.getX());
        double ty = MathHelper.lerp(pt, target.prevY, target.getY()) + 0.38 + target.getHeight() / 2.0;
        double tz = MathHelper.lerp(pt, target.prevZ, target.getZ());
        double rx = tx - camPos.x + 0.2, ry = ty - camPos.y, rz = tz - camPos.z;
        double radius = 0.4 + target.getWidth() / 2.0;
        float speed = 30f / ghostSpeed.getCurrent(), size = prizrakiSize.getCurrent();
        int length = 34; long now = System.currentTimeMillis();
        float[] rgb = themeRgb(); float r = rgb[0], g = rgb[1], b = rgb[2];
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int trail = 0; trail < 3; trail++) {
            for (int i = 0; i < length; i++) {
                double angle = 0.05f * (now - lastPrizrakTime - (i * 6.0)) / speed;
                double s = Math.sin(angle * Math.PI) * radius, c = Math.cos(angle * Math.PI) * radius;
                double o = (trail == 0) ? Math.cos(angle * Math.PI) * radius : Math.sin(angle * Math.PI) * radius;
                float t = i / (float)(length - 1), curSize = size * (1.0f - t * 0.5f), alpha = (1.0f - t * 0.9f) * alphaMul;
                double px = rx + (trail == 1 ? -s : s), py = ry + o, pz = rz + (trail == 2 ? c : -c);
                float cr = Math.min(1f, r * 1.5f), cg = Math.min(1f, g * 1.5f), cb = Math.min(1f, b * 1.5f);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.6f, r, g, b, alpha * 0.15f, camYaw, camPitch);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.35f, r, g, b, alpha * 0.35f, camYaw, camPitch);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.15f, cr, cg, cb, alpha, camYaw, camPitch);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end()); restoreRenderState();
    }

    private void renderKolco(MatrixStack matrices, Camera camera, LivingEntity target, float pt, float animProgress) {
        if (animProgress <= 0f || target == null) return;
        Vec3d camPos = camera.getPos(); float camYaw = camera.getYaw(), camPitch = camera.getPitch();
        double tx = MathHelper.lerp(pt, target.prevX, target.getX());
        double ty = MathHelper.lerp(pt, target.prevY, target.getY());
        double tz = MathHelper.lerp(pt, target.prevZ, target.getZ());
        double rx = tx - camPos.x, ry = ty - camPos.y, rz = tz - camPos.z;
        float entityWidth = target.getWidth() * 0.9f, entityHeight = target.getHeight();
        float eased = easeOutCubic(animProgress);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR); RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean has = false; double golovkaY = absSin(kolcoStep) * entityHeight; int totalPoints = 72;
        long now = System.currentTimeMillis();
        for (int i = 0; i < totalPoints; i++) {
            double angle = 2 * Math.PI * i / totalPoints;
            float xOff = (float)(Math.cos(angle) * entityWidth), zOff = (float)(Math.sin(angle) * entityWidth);
            int baseColor = getKolcoColor(i * (360 / totalPoints), now);
            float cr = ((baseColor >> 16) & 0xFF) / 255f, cg = ((baseColor >> 8) & 0xFF) / 255f, cb = (baseColor & 0xFF) / 255f;
            float ss = eased * 0.12f; if (ss < 0.01f) continue;
            putBloomQuad(builder, matrices, rx+xOff, ry+golovkaY, rz+zOff, ss*4f, cr, cg, cb, eased*0.15f, camYaw, camPitch);
            putBloomQuad(builder, matrices, rx+xOff, ry+golovkaY, rz+zOff, ss*2f, cr, cg, cb, eased*0.35f, camYaw, camPitch);
            putBloomQuad(builder, matrices, rx+xOff, ry+golovkaY, rz+zOff, ss*0.8f, Math.min(1f,cr*1.5f), Math.min(1f,cg*1.5f), Math.min(1f,cb*1.5f), eased, camYaw, camPitch);
            has = true;
        }
        if (has) BufferRenderer.drawWithGlobalProgram(builder.end()); restoreRenderState();
    }

    private double absSin(double input) { return Math.abs(1 + Math.sin(input)) / 2.0; }

    private int getKolcoColor(int offsetAngle, long now) {
        int color;
        if (kolcoRainbow.isEnabled()) {
            float timeFactor = (now % 3000) / 3000.0f;
            int hue = (int)(timeFactor * 360) + offsetAngle;
            color = Color.HSBtoRGB((hue % 360) / 360f, 0.7f, 1.0f);
            color = (255 << 24) | (color & 0x00FFFFFF);
        } else {
            ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
            color = (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }
        return applyDamageFlash(color);
    }

    private int applyDamageFlash(int color) {
        if (!kolcoDamageRed.isEnabled()) return color;
        long timeSince = System.currentTimeMillis() - lastDamageTime;
        float tgt = 0f;
        if (timeSince < DAMAGE_FLASH_DURATION) tgt = 1.0f - easeOutCubic((float) timeSince / DAMAGE_FLASH_DURATION);
        damageFlashIntensity = MathHelper.lerp(0.1f, damageFlashIntensity, tgt);
        if (damageFlashIntensity < 0.05f) return color;
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        return (a << 24) | ((int) MathHelper.lerp(damageFlashIntensity, r, 255) << 16)
                | ((int) MathHelper.lerp(damageFlashIntensity, g, 50) << 8)
                | (int) MathHelper.lerp(damageFlashIntensity, b, 50);
    }

    private void renderCrystals(MatrixStack matrices, Camera camera, LivingEntity target, boolean sharp, float anim, float pt) {
        if (target == null || mc.player == null) return;
        float eased = easeOutCubic(anim), time = (mc.player.age + pt) * 6.0f;
        Vec3d camPos = camera.getPos();
        double tx = MathHelper.lerp(pt, target.prevX, target.getX());
        double ty = MathHelper.lerp(pt, target.prevY, target.getY());
        double tz = MathHelper.lerp(pt, target.prevZ, target.getZ());
        float camYaw = camera.getYaw(), camPitch = camera.getPitch();
        float entityHeight = target.getHeight(), halfWidth = target.getWidth() * 0.5f;
        float[] rgb = themeRgb();
        float r = Math.min(1f, rgb[0] * 1.3f), g = Math.min(1f, rgb[1] * 1.3f), b = Math.min(1f, rgb[2] * 1.3f);
        matrices.push();
        matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);
        int crystalCount = 18;
        float pelvisY = entityHeight * 0.35f, torsoY = entityHeight * 0.55f, neckY = entityHeight * 0.74f;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR); RenderSystem.enableBlend(); RenderSystem.disableCull();
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean hasCrystals = false;
        for (int i = 0; i < crystalCount; i++) {
            float s1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float s2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float s3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angle = time + i * (360f / crystalCount) + s1 * 12f;
            float radius = halfWidth + 0.25f + s3 * 0.15f;
            float cx = radius * (float) Math.cos(Math.toRadians(angle));
            float cz = radius * (float) Math.sin(Math.toRadians(angle));
            float cy = s2 * entityHeight;
            float scale = 0.18f * eased; if (scale < 0.001f) continue;
            float lookY = getCrystalLookY(cy, entityHeight, pelvisY, torsoY, neckY);
            float dx = -cx, dy = lookY - cy, dz = -cz;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx));
            float pitch = (float) Math.toDegrees(Math.atan2(dy, (float) Math.sqrt(dx*dx + dz*dz)));
            drawCrystalShape(buf, matrices, cx, cy, cz, scale, yaw, pitch, (int)(r*255), (int)(g*255), (int)(b*255), (int)(200*anim), sharp);
            hasCrystals = true;
        }
        if (hasCrystals) BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR); RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder glowBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasGlow = false;
        for (int i = 0; i < crystalCount; i++) {
            float s1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float s2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float s3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angle = time + i * (360f / crystalCount) + s1 * 12f;
            float radius = halfWidth + 0.25f + s3 * 0.15f;
            float cx = radius * (float) Math.cos(Math.toRadians(angle));
            float cz = radius * (float) Math.sin(Math.toRadians(angle));
            float cy = s2 * entityHeight;
            float scale = 0.18f * eased;
            if (anim * 0.15f > 0.001f && scale > 0.0001f) {
                putBloomQuad(glowBuf, matrices, cx, cy, cz, scale * 5.5f, r, g, b, anim * 0.15f, camYaw, camPitch);
                putBloomQuad(glowBuf, matrices, cx, cy, cz, scale * 3.5f, r, g, b, anim * 0.25f, camYaw, camPitch);
                hasGlow = true;
            }
        }
        if (hasGlow) BufferRenderer.drawWithGlobalProgram(glowBuf.end());
        matrices.pop();
        RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend();
    }

    private float getCrystalLookY(float cy, float h, float pelvis, float torso, float neck) {
        float n = cy / h;
        return n < 0.33f ? pelvis : n < 0.6f ? torso : neck;
    }

    private void drawCrystalShape(BufferBuilder buf, MatrixStack ms, float x, float y, float z,
                                   float scale, float yaw, float pitch, int r, int g, int b, int a, boolean sharp) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch));
        ms.scale(scale, scale, scale);
        Matrix4f m = ms.peek().getPositionMatrix();
        float w = sharp ? 0.35f : 0.7f, h = sharp ? 1.2f : 1.0f;
        tri(buf, m, h,0,0, 0,w,0, 0,0,w, r,g,b,a); tri(buf, m, h,0,0, 0,0,w, 0,-w,0, r,g,b,a);
        tri(buf, m, h,0,0, 0,-w,0, 0,0,-w, r,g,b,a); tri(buf, m, h,0,0, 0,0,-w, 0,w,0, r,g,b,a);
        tri(buf, m, -h,0,0, 0,w,0, 0,0,w, r,g,b,a); tri(buf, m, -h,0,0, 0,0,w, 0,-w,0, r,g,b,a);
        tri(buf, m, -h,0,0, 0,-w,0, 0,0,-w, r,g,b,a); tri(buf, m, -h,0,0, 0,0,-w, 0,w,0, r,g,b,a);
        ms.pop();
    }

    private void tri(BufferBuilder buf, Matrix4f m, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3, int r,int g,int b,int a) {
        buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x2,y2,z2).color(r,g,b,a); buf.vertex(m,x3,y3,z3).color(r,g,b,a);
    }

    private void renderCubes(MatrixStack matrices, Camera camera, LivingEntity target, float pt, float alphaMul) {
        long now = System.currentTimeMillis();
        if (lastCubeTime == 0L) lastCubeTime = now;
        float dt = Math.min((now - lastCubeTime) / 1000.0f, 0.1f);
        lastCubeTime = now;
        if (target != null) {
            spawnAccumulator += dt;
            while (spawnAccumulator >= SPAWN_INTERVAL) {
                spawnAccumulator -= SPAWN_INTERVAL;
                for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
                    double rand = ThreadLocalRandom.current().nextDouble() * 360;
                    double px = Math.cos(Math.toRadians(rand)) * 0.7;
                    double py = ThreadLocalRandom.current().nextDouble() * 0.16 + 0.04;
                    double pz = Math.sin(Math.toRadians(rand)) * 0.7;
                    particles.add(new CubeParticle(target, px, py, pz));
                }
            }
        }
        float camYaw = camera.getYaw(), camPitch = camera.getPitch();
        Iterator<CubeParticle> it = particles.iterator();
        ArrayList<CubeParticle> toRender = new ArrayList<>();
        while (it.hasNext()) {
            CubeParticle p = it.next();
            p.update(dt);
            if (now - p.time > 1000L) it.remove();
            else toRender.add(p);
        }
        int color = themeColorInt();
        for (CubeParticle p : toRender) p.renderCube(matrices, camera, color, pt, alphaMul);
        if (!toRender.isEmpty()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.disableCull(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false);
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            boolean hasBloom = false;
            for (CubeParticle p : toRender) if (p.renderBloom(builder, matrices, camera, color, camYaw, camPitch, pt, alphaMul)) hasBloom = true;
            if (hasBloom) BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.depthMask(true); RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.enableCull();
        }
    }

    // ── Cubes2 (наши вращающиеся кубы из TargetESP) ──────────────────────────

    private void renderCubes2(MatrixStack ms, Camera camera, LivingEntity target, float pt, float alphaMul) {
        Vec3d camPos = camera.getPos();
        Vec3d targetPos = new Vec3d(
            MathHelper.lerp(pt, target.prevX, target.getX()),
            MathHelper.lerp(pt, target.prevY, target.getY()),
            MathHelper.lerp(pt, target.prevZ, target.getZ())
        );
        Vec3d renderPos = targetPos.subtract(camPos);

        float cubeScaleFactor = cubes2Size.getCurrent() * 0.12f;
        float time = ((float) mc.player.age + pt) * cubes2Speed.getCurrent();
        float entityHeight = target.getHeight();
        float entityWidth  = target.getWidth();
        float halfWidth    = entityWidth * 0.5f;
        int   cubeCount    = (int) cubes2Count.getCurrent();

        ms.push();
        ms.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int color = themeColorInt();

        for (int i = 0; i < cubeCount; i++) {
            float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;
            float angleOffset = i * (360.0f / cubeCount) + seed1 * 12.0f;
            float angle  = time + angleOffset;
            float radius = halfWidth + 0.25f + seed3 * 0.15f;
            float x = radius * (float) Math.cos(Math.toRadians(angle));
            float z = radius * (float) Math.sin(Math.toRadians(angle));
            float bobbing = (float) Math.sin(time * 0.05f + i * 0.3f) * 0.1f;
            float y = seed2 * entityHeight * 1.05f + bobbing;
            drawCubes2Cube(buf, ms, x, y, z, cubeScaleFactor, time * 4.0f, time * 3.0f, color, 0.5f * alphaMul);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawCubes2Cube(BufferBuilder buf, MatrixStack ms, float x, float y, float z,
                                 float size, float rotY, float rotX, int color, float alpha) {
        if (size <= 0f) return;
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        Matrix4f m = ms.peek().getPositionMatrix();
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int a = (int)(255f * alpha);
        float s = size / 2f;
        addCubes2Quad(buf, m, -s,-s, s,  s,-s, s,  s, s, s, -s, s, s, r, g, b, a);
        addCubes2Quad(buf, m, -s,-s,-s, -s, s,-s,  s, s,-s,  s,-s,-s, (int)(r*.9),(int)(g*.9),(int)(b*.9), a);
        addCubes2Quad(buf, m, -s,-s,-s, -s,-s, s, -s, s, s, -s, s,-s, (int)(r*.8),(int)(g*.8),(int)(b*.8), a);
        addCubes2Quad(buf, m,  s,-s,-s,  s, s,-s,  s, s, s,  s,-s, s, (int)(r*.8),(int)(g*.8),(int)(b*.8), a);
        addCubes2Quad(buf, m, -s, s,-s, -s, s, s,  s, s, s,  s, s,-s, Math.min(255,(int)(r*1.1)),Math.min(255,(int)(g*1.1)),Math.min(255,(int)(b*1.1)), a);
        addCubes2Quad(buf, m, -s,-s,-s,  s,-s,-s,  s,-s, s, -s,-s, s, (int)(r*.7),(int)(g*.7),(int)(b*.7), a);
        ms.pop();
    }

    private void addCubes2Quad(BufferBuilder buf, Matrix4f m,
                                float x1,float y1,float z1, float x2,float y2,float z2,
                                float x3,float y3,float z3, float x4,float y4,float z4,
                                int r, int g, int b, int a) {
        buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x2,y2,z2).color(r,g,b,a); buf.vertex(m,x3,y3,z3).color(r,g,b,a);
        buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x3,y3,z3).color(r,g,b,a); buf.vertex(m,x4,y4,z4).color(r,g,b,a);
    }

    // -- Helpers ---------------------------------------------------------------

    private void putBloomQuad(BufferBuilder builder, MatrixStack ms, double x, double y, double z,
                               float scale, float r, float g, float b, float a, float camYaw, float camPitch) {
        if (a <= 0.001f || scale <= 0.0001f) return;
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
        ms.scale(scale, scale, scale);
        Matrix4f m = ms.peek().getPositionMatrix();
        int ri = (int)(r*255), gi = (int)(g*255), bi = (int)(b*255), ai = (int)(a*255);
        builder.vertex(m, -0.5f,  0.5f, 0).texture(0f, 1f).color(ri, gi, bi, ai);
        builder.vertex(m,  0.5f,  0.5f, 0).texture(1f, 1f).color(ri, gi, bi, ai);
        builder.vertex(m,  0.5f, -0.5f, 0).texture(1f, 0f).color(ri, gi, bi, ai);
        builder.vertex(m, -0.5f, -0.5f, 0).texture(0f, 0f).color(ri, gi, bi, ai);
        ms.pop();
    }

    private static float easeOutCubic(float t) { float u = 1f - t; return 1f - u * u * u; }

    private void restoreRenderState() {
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend();
        RenderSystem.enableCull(); RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private float[] themeRgb() {
        ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        return new float[]{ c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f };
    }

    private int themeColorInt() {
        ColorRGBA c = Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    // -- Inner classes ---------------------------------------------------------

    public static class CubeParticle {
        double x, y, z; long time; LivingEntity entity;
        CubeParticle(LivingEntity entity, double x, double y, double z) {
            this.entity = entity; this.x = x; this.y = y; this.z = z; this.time = System.currentTimeMillis();
        }
        void update(float dt) { this.y += (ThreadLocalRandom.current().nextDouble() * 0.03 + 0.01) * (dt * 60); }

        void renderCube(MatrixStack ms, Camera camera, int colorInt, float pt, float alphaMul) {
            if (entity == null) return;
            double alive = System.currentTimeMillis() - time;
            float life = Math.min(1f, (float) alive / 1000f);
            float alpha = (life > 0.8f ? 1f - (life - 0.8f) * 5f : (alive < 200 ? (float) alive / 200f : 1f)) * alphaMul;
            if (alpha <= 0.001f) return;
            Vec3d cam = camera.getPos();
            double ex = MathHelper.lerp(pt, entity.prevX, entity.getX());
            double ey = MathHelper.lerp(pt, entity.prevY, entity.getY());
            double ez = MathHelper.lerp(pt, entity.prevZ, entity.getZ());
            float scale = 0.12f;
            int color = setAlpha(colorInt, (int)(alpha * 255));
            ms.push();
            ms.translate(ex - cam.x + x, ey - cam.y + y, ez - cam.z + z);
            double rot = alive / 10.0;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rot));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rot));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rot));
            ms.scale(scale, scale, scale);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.enableBlend(); RenderSystem.enableDepthTest(); RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawBox(ms, color);
            RenderSystem.depthMask(true); RenderSystem.disableBlend(); RenderSystem.enableCull();
            ms.pop();
        }

        boolean renderBloom(BufferBuilder builder, MatrixStack ms, Camera camera, int colorInt,
                            float camYaw, float camPitch, float pt, float alphaMul) {
            if (entity == null) return false;
            double alive = System.currentTimeMillis() - time;
            float life = Math.min(1f, (float) alive / 1000f);
            float alpha = (life > 0.8f ? 1f - (life - 0.8f) * 5f : (alive < 200 ? (float) alive / 200f : 1f)) * alphaMul;
            if (alpha <= 0.001f) return false;
            float r = ((colorInt >> 16) & 0xFF) / 255f, g = ((colorInt >> 8) & 0xFF) / 255f, b = (colorInt & 0xFF) / 255f;
            Vec3d cam = camera.getPos();
            double ex = MathHelper.lerp(pt, entity.prevX, entity.getX());
            double ey = MathHelper.lerp(pt, entity.prevY, entity.getY());
            double ez = MathHelper.lerp(pt, entity.prevZ, entity.getZ());
            ms.push();
            ms.translate(ex - cam.x + x, ey - cam.y + y, ez - cam.z + z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
            ms.scale(0.9f, 0.9f, 0.9f);
            Matrix4f m = ms.peek().getPositionMatrix();
            int ai = (int)(alpha * 0.12f * 255), ri = (int)(r*255), gi = (int)(g*255), bi = (int)(b*255);
            builder.vertex(m, -0.5f,  0.5f, 0).texture(0f, 1f).color(ri, gi, bi, ai);
            builder.vertex(m,  0.5f,  0.5f, 0).texture(1f, 1f).color(ri, gi, bi, ai);
            builder.vertex(m,  0.5f, -0.5f, 0).texture(1f, 0f).color(ri, gi, bi, ai);
            builder.vertex(m, -0.5f, -0.5f, 0).texture(0f, 0f).color(ri, gi, bi, ai);
            ms.pop(); return true;
        }

        private static void drawBox(MatrixStack ms, int color) {
            float min = -0.5f, max = 0.5f;
            float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f, a = ((color >> 24) & 0xFF) / 255f;
            float lineA = a / 4f, fillA = a / 12f;
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            Matrix4f m = ms.peek().getPositionMatrix();
            addLine(buf, m, min,min,min, max,min,min, r,g,b,lineA); addLine(buf, m, min,max,min, max,max,min, r,g,b,lineA);
            addLine(buf, m, min,min,max, max,min,max, r,g,b,lineA); addLine(buf, m, min,max,max, max,max,max, r,g,b,lineA);
            addLine(buf, m, min,min,min, min,max,min, r,g,b,lineA); addLine(buf, m, max,min,min, max,max,min, r,g,b,lineA);
            addLine(buf, m, min,min,max, min,max,max, r,g,b,lineA); addLine(buf, m, max,min,max, max,max,max, r,g,b,lineA);
            addLine(buf, m, min,min,min, min,min,max, r,g,b,lineA); addLine(buf, m, max,min,min, max,min,max, r,g,b,lineA);
            addLine(buf, m, min,max,min, min,max,max, r,g,b,lineA); addLine(buf, m, max,max,min, max,max,max, r,g,b,lineA);
            BufferRenderer.drawWithGlobalProgram(buf.end());
            BufferBuilder fb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            addFace(fb, m, min,min,min, max,min,max, r,g,b,fillA); addFace(fb, m, min,max,min, max,max,max, r,g,b,fillA);
            addFace(fb, m, min,min,min, max,max,min, r,g,b,fillA); addFace(fb, m, min,min,max, max,max,max, r,g,b,fillA);
            addFace(fb, m, min,min,min, min,max,max, r,g,b,fillA); addFace(fb, m, max,min,min, max,max,max, r,g,b,fillA);
            BufferRenderer.drawWithGlobalProgram(fb.end());
        }

        private static void addLine(BufferBuilder buf, Matrix4f m, float x1,float y1,float z1, float x2,float y2,float z2, float r,float g,float b,float a) {
            buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x2,y2,z2).color(r,g,b,a);
        }

        private static void addFace(BufferBuilder buf, Matrix4f m, float x1,float y1,float z1, float x2,float y2,float z2, float r,float g,float b,float a) {
            buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x2,y1,z1).color(r,g,b,a);
            buf.vertex(m,x2,y1,z2).color(r,g,b,a); buf.vertex(m,x1,y1,z2).color(r,g,b,a);
            buf.vertex(m,x1,y2,z1).color(r,g,b,a); buf.vertex(m,x1,y2,z2).color(r,g,b,a);
            buf.vertex(m,x2,y2,z2).color(r,g,b,a); buf.vertex(m,x2,y2,z1).color(r,g,b,a);
            buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x1,y2,z1).color(r,g,b,a);
            buf.vertex(m,x2,y2,z1).color(r,g,b,a); buf.vertex(m,x2,y1,z1).color(r,g,b,a);
            buf.vertex(m,x1,y1,z2).color(r,g,b,a); buf.vertex(m,x2,y1,z2).color(r,g,b,a);
            buf.vertex(m,x2,y2,z2).color(r,g,b,a); buf.vertex(m,x1,y2,z2).color(r,g,b,a);
            buf.vertex(m,x1,y1,z1).color(r,g,b,a); buf.vertex(m,x1,y1,z2).color(r,g,b,a);
            buf.vertex(m,x1,y2,z2).color(r,g,b,a); buf.vertex(m,x1,y2,z1).color(r,g,b,a);
            buf.vertex(m,x2,y1,z1).color(r,g,b,a); buf.vertex(m,x2,y2,z1).color(r,g,b,a);
            buf.vertex(m,x2,y2,z2).color(r,g,b,a); buf.vertex(m,x2,y1,z2).color(r,g,b,a);
        }

        private static int setAlpha(int color, int a) {
            return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
        }
    }
}
