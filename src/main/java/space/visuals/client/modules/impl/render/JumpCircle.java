package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.player.EventJump;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.theme.Theme;
import space.visuals.base.theme.ThemeManager;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleAnnotation(name = "JumpCircle", category = Category.RENDER, description = "Рисует круг при прыжке.")
public final class JumpCircle extends Module {
    public static final JumpCircle INSTANCE = new JumpCircle();

    private static final Identifier TEXTURE_DEFAULT  = Zenith.id("hud/jump_circle.png");
    private static final Identifier TEXTURE_JUMPS_V1 = Zenith.id("hud/circle.png");
    private static final Identifier TEXTURE_JUMPS_V2 = Zenith.id("hud/circle_2.png");
    private static final Identifier TEXTURE_JUMPS_V3 = Zenith.id("hud/circle_bold.png");
    private static final Identifier TEXTURE_JUMPS_V4 = Zenith.id("hud/hens.png");
    private static final Identifier TEXTURE_VURST    = Zenith.id("hud/vurstvisual.png");

    private final ModeSetting mode = new ModeSetting("Режим", "Обычный", "Рокстар", "Пульс", "Тонкие", "Широкие", "Ромбированный", "Вурст Визуал");
    private final ModeSetting.Value modeClassic    = mode.getValues().get(0);
    private final ModeSetting.Value modeRockstar   = mode.getValues().get(1);
    private final ModeSetting.Value modeJumpsV1    = mode.getValues().get(2);
    private final ModeSetting.Value modeJumpsV2    = mode.getValues().get(3);
    private final ModeSetting.Value modeJumpsV3    = mode.getValues().get(4);
    private final ModeSetting.Value modeJumpsV4    = mode.getValues().get(5);
    private final ModeSetting.Value modeVurstVisual = mode.getValues().get(6);

    private final NumberSetting radius   = new NumberSetting("Радиус",    1.0f, 0.01f, 2.0f,    0.01f, this::isCircleTextureMode);
    private final NumberSetting expand   = new NumberSetting("Расширение",1.0f, 0.2f,  6.0f,    0.1f,  this::isCircleTextureMode);
    private final NumberSetting lifetime = new NumberSetting("Время жизни",2.0f,0.5f,  10.0f,   0.1f,  this::isCircleTextureMode);
    private final BooleanSetting customTheme = new BooleanSetting("Кастомный цвет", false, this::isCircleTextureMode);
    private final ColorSetting primaryColor   = new ColorSetting("Цвет 1", Theme.DARK.getColor(),       () -> isCircleTextureMode() && customTheme.isEnabled(), Theme.DARK::getColor);
    private final ColorSetting secondaryColor = new ColorSetting("Цвет 2", Theme.DARK.getSecondColor(), () -> isCircleTextureMode() && customTheme.isEnabled(), Theme.DARK::getSecondColor);

    private final NumberSetting rockstarRadius = new NumberSetting("Рокстар радиус", 4.0f, 2.0f, 8.0f,    0.1f,  () -> mode.is(modeRockstar));
    private final NumberSetting rockstarSpeed  = new NumberSetting("Рокстар скорость",800f,300f, 2000.0f, 10.0f, () -> mode.is(modeRockstar));
    private final ColorSetting rockstarColor1  = new ColorSetting("Рокстар цвет 1", new ColorRGBA(100,200,255,255), () -> mode.is(modeRockstar));
    private final ColorSetting rockstarColor2  = new ColorSetting("Рокстар цвет 2", new ColorRGBA(255,100,200,255), () -> mode.is(modeRockstar));

    private final List<Circle> circles = new ArrayList<>();
    private final List<WaveEffect> waveEffects = new ArrayList<>();
    private final ThemeManager themeManager = Zenith.getInstance().getThemeManager();
    private Object lastWorld;

    private JumpCircle() {}

    @Override
    public void onEnable() {
        circles.clear(); waveEffects.clear();
        lastWorld = mc.world;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        circles.clear(); waveEffects.clear();
        lastWorld = null;
        super.onDisable();
    }

    @EventTarget
    public void onJump(EventJump event) {
        if (mc.player == null || mc.world == null) return;
        if (mode.is(modeRockstar)) {
            waveEffects.add(new WaveEffect(mc.player.getBlockPos().down(), System.currentTimeMillis()));
        } else {
            circles.add(new Circle(mc.player.getPos().add(0, 0.05, 0)));
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        if (lastWorld != mc.world) { circles.clear(); waveEffects.clear(); lastWorld = mc.world; }
        if (mode.is(modeRockstar)) { renderRockstar(); return; }
        renderClassic(event);
    }

    private void renderClassic(EventRender3D event) {
        if (circles.isEmpty()) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, getCurrentTexture());
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        MatrixStack matrices = event.getMatrix();
        Vec3d camPos = mc.getEntityRenderDispatcher().camera.getPos();
        long now = System.currentTimeMillis();
        long lifeMs = Math.max(1, (long)(lifetime.getCurrent() * 1000f));
        Iterator<Circle> it = circles.iterator();
        while (it.hasNext()) {
            Circle circle = it.next();
            long age = now - circle.spawnTime;
            if (age >= lifeMs) { it.remove(); continue; }
            if (isOccluded(camPos, circle.position.add(0, 0.1, 0))) continue;
            float progress = MathHelper.clamp((float) age / lifeMs, 0f, 1f);
            float fade = 1f - progress;
            float eased = Easing.CIRC_OUT.ease(progress, 0f, 1f, 1f);
            float diameter = radius.getCurrent() + expand.getCurrent() * eased;
            renderClassicCircle(matrices, camPos, circle.position, diameter, fade, age);
        }
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderClassicCircle(MatrixStack matrices, Vec3d camPos, Vec3d pos, float diameter, float alpha, long age) {
        float half = diameter / 2f;
        int alphaInt = clamp255(255f * alpha);
        int primary   = getPrimaryThemeColor().withAlpha(alphaInt).getRGB();
        int secondary = getSecondaryThemeColor().withAlpha(alphaInt).getRGB();
        float phase = (float)(age % 1600L) / 1600f;
        int topLeft     = lerpColor(primary, secondary, animatedBlend(phase));
        int topRight    = lerpColor(primary, secondary, animatedBlend(phase + 0.25f));
        int bottomRight = lerpColor(primary, secondary, animatedBlend(phase + 0.5f));
        int bottomLeft  = lerpColor(primary, secondary, animatedBlend(phase + 0.75f));
        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -half, 0f, -half).texture(0f, 0f).color(topLeft);
        buffer.vertex(matrix,  half, 0f, -half).texture(1f, 0f).color(topRight);
        buffer.vertex(matrix,  half, 0f,  half).texture(1f, 1f).color(bottomRight);
        buffer.vertex(matrix, -half, 0f,  half).texture(0f, 1f).color(bottomLeft);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    private void renderRockstar() {
        if (waveEffects.isEmpty() || mc.world == null) return;
        long now = System.currentTimeMillis();
        Iterator<WaveEffect> it = waveEffects.iterator();
        while (it.hasNext()) {
            WaveEffect wave = it.next();
            if (wave.isExpired(now)) { it.remove(); continue; }
            wave.render(now);
        }
    }

    private ColorRGBA getPrimaryThemeColor() {
        return customTheme.isEnabled() ? primaryColor.getColor() : themeManager.getCurrentTheme().getColor();
    }
    private ColorRGBA getSecondaryThemeColor() {
        return customTheme.isEnabled() ? secondaryColor.getColor() : themeManager.getCurrentTheme().getSecondColor();
    }

    private boolean isCircleTextureMode() { return !mode.is(modeRockstar); }

    private Identifier getCurrentTexture() {
        if (mode.is(modeJumpsV1)) return TEXTURE_JUMPS_V1;
        if (mode.is(modeJumpsV2)) return TEXTURE_JUMPS_V2;
        if (mode.is(modeJumpsV3)) return TEXTURE_JUMPS_V3;
        if (mode.is(modeJumpsV4)) return TEXTURE_JUMPS_V4;
        if (mode.is(modeVurstVisual)) return TEXTURE_VURST;
        return TEXTURE_DEFAULT;
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static float animatedBlend(float phase) {
        float n = phase - (float) Math.floor(phase);
        return (float)((Math.sin(n * Math.PI * 2.0) + 1.0) * 0.5);
    }
    private static int clamp255(float v) { return Math.round(MathHelper.clamp(v, 0f, 255f)); }
    private static int withAlpha(int color, int alpha) { return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF); }
    private static int lerpColor(int c1, int c2, float t) {
        float f = MathHelper.clamp(t, 0f, 1f);
        int a1=c1>>>24&0xFF, r1=c1>>>16&0xFF, g1=c1>>>8&0xFF, b1=c1&0xFF;
        int a2=c2>>>24&0xFF, r2=c2>>>16&0xFF, g2=c2>>>8&0xFF, b2=c2&0xFF;
        return (Math.round(a1+(a2-a1)*f)<<24)|(Math.round(r1+(r2-r1)*f)<<16)|(Math.round(g1+(g2-g1)*f)<<8)|Math.round(b1+(b2-b1)*f);
    }
    private static float easeOutCubic(float x) { return 1f - (float)Math.pow(1f - x, 3); }
    private static float easeInCubic(float x) { return x * x * x; }

    // ==================== INNER CLASSES ====================
    private final class WaveEffect {
        private static final int MAX_PER_FRAME = 400;
        private static final float WAVE_WIDTH = 2.5f;
        private final BlockPos centerPos;
        private final long startTime;
        private final long duration;
        private final int maxRadius;

        WaveEffect(BlockPos centerPos, long startTime) {
            this.centerPos = centerPos;
            this.startTime = startTime;
            this.duration = (long) rockstarSpeed.getCurrent();
            this.maxRadius = Math.max(1, (int) Math.ceil(rockstarRadius.getCurrent()));
        }

        boolean isExpired(long now) { return now - startTime > duration; }

        void render(long now) {
            if (mc.world == null) return;
            long elapsed = now - startTime;
            float progress = MathHelper.clamp((float) elapsed / duration, 0f, 1f);
            float currentRadius = easeOutCubic(progress) * maxRadius;
            float globalAlpha;
            float fadeInDuration = 0.15f, fadeOutStart = 0.75f;
            if (progress < fadeInDuration) globalAlpha = progress / fadeInDuration;
            else if (progress >= fadeOutStart) globalAlpha = 1f - easeInCubic((progress - fadeOutStart) / (1f - fadeOutStart));
            else globalAlpha = 1f;

            int rendered = 0;
            int colorA = rockstarColor1.getColor().getRGB();
            int colorB = rockstarColor2.getColor().getRGB();

            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    if (rendered >= MAX_PER_FRAME) return;
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist > currentRadius + 0.5f || dist < currentRadius - WAVE_WIDTH) continue;
                    BlockPos renderPos = findSurface(centerPos.add(x, 0, z));
                    if (renderPos == null) continue;
                    BlockState state = mc.world.getBlockState(renderPos);
                    VoxelShape shape = state.getOutlineShape((BlockView) mc.world, renderPos);
                    if (shape.isEmpty()) continue;
                    rendered++;
                    float waveProgress = (float)(dist / Math.max(1, maxRadius));
                    float localAlpha = 1f - Math.abs((float) dist - currentRadius) / WAVE_WIDTH;
                    localAlpha = MathHelper.clamp(localAlpha, 0f, 1f);
                    float pulse = (float) Math.sin(progress * (float) Math.PI * 4f - waveProgress * 2f);
                    localAlpha *= 0.5f + ((pulse + 1f) / 2f) * 0.5f;
                    localAlpha *= globalAlpha;
                    if (localAlpha <= 0.02f) continue;
                    float smoothT = (float)(Math.sin((waveProgress - 0.5f) * Math.PI) * 0.5 + 0.5);
                    int gradientColor = lerpColor(colorA, colorB, smoothT);
                    int finalColor = withAlpha(gradientColor, (int)(localAlpha * 180f));
                    try {
                        Render3DUtil.drawShapeAlternative(renderPos, shape, finalColor, 1.5f, true, true);
                    } catch (Exception ignored) {}
                }
            }
        }

        private BlockPos findSurface(BlockPos pos) {
            if (mc.world == null) return null;
            for (int y = 2; y >= -4; y--) {
                BlockPos p = pos.add(0, y, 0);
                if (mc.world.getBlockState(p).isAir() || !mc.world.getBlockState(p.up()).isAir()) continue;
                return p;
            }
            return null;
        }
    }

    private static final class Circle {
        final Vec3d position;
        final long spawnTime;
        Circle(Vec3d position) { this.position = position; this.spawnTime = System.currentTimeMillis(); }
    }
}
