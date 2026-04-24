package space.visuals.client.modules.impl.render.motionblur;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import space.visuals.Zenith;
import space.visuals.client.modules.impl.render.MotionBlur;

public class MotionBlurShader {
    private final PostEffectShader shader;
    private long lastNano = System.nanoTime();
    private float currentFPS = 0.0f;

    private final Matrix4f tempPrevModelView = new Matrix4f();
    private final Matrix4f tempPrevProjection = new Matrix4f();
    private final Matrix4f tempProjInverse = new Matrix4f();
    private final Matrix4f tempMvInverse = new Matrix4f();

    public MotionBlurShader() {
        shader = new PostEffectShader(
                Identifier.of("space", "motion_blur"),
                s -> s.setBlendFactor(MotionBlur.INSTANCE.getStrength())
        );
    }

    public void applyBeforeHands() {
        long now = System.nanoTime();
        float delta = (now - lastNano) / 1_000_000_000.0f;
        lastNano = now;
        if (delta > 0 && delta < 1.0f) currentFPS = 1.0f / delta;

        if (!MotionBlur.INSTANCE.isEnabled()) return;
        if (MotionBlur.INSTANCE.getStrength() == 0) return;

        apply();
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();

        MonitorInfoProvider.updateDisplayInfo();
        int refreshRate = MonitorInfoProvider.getRefreshRate();

        float strength = MotionBlur.INSTANCE.getStrength();
        if (MotionBlur.INSTANCE.isUseRRC()) {
            float ratio = (refreshRate > 0) ? currentFPS / refreshRate : 1.0f;
            if (ratio < 1.0f) ratio = 1.0f;
            strength = strength * ratio;
        }

        int samples = getSamples(currentFPS);

        shader.setBlendFactor(strength);
        shader.setViewRes(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
        shader.setMotionBlurSamples(samples);
        shader.setHalfSamples(samples / 2);
        shader.setInverseSamples(1.0f / samples);
        shader.setBlurAlgorithm(1);
        shader.setHandDepthThreshold(0.56f);
        shader.render(0.0f);
    }

    private int getSamples(float fps) {
        int base = switch (MotionBlur.INSTANCE.getQuality()) {
            case 0 -> 8;
            case 1 -> 12;
            case 2 -> 16;
            case 3 -> 24;
            default -> 12;
        };
        if (fps < 30) return Math.max(6, base / 2);
        if (fps < 60) return Math.max(8, (int)(base * 0.75f));
        if (fps > 144) return (int)(base * 1.25f);
        return base;
    }

    public void setFrameData(Matrix4f modelView, Matrix4f prevModelView,
                             Matrix4f projection, Matrix4f prevProjection,
                             Vector3f cameraPos, Vector3f prevCameraPos) {
        shader.setMvInverse(tempMvInverse.set(modelView).invert());
        shader.setProjInverse(tempProjInverse.set(projection).invert());
        shader.setPrevModelView(tempPrevModelView.set(prevModelView));
        shader.setPrevProjection(tempPrevProjection.set(prevProjection));
        shader.setCameraPos(cameraPos.x, cameraPos.y, cameraPos.z);
        shader.setPrevCameraPos(prevCameraPos.x, prevCameraPos.y, prevCameraPos.z);
    }

    public void reload() {
        shader.reload();
    }
}
