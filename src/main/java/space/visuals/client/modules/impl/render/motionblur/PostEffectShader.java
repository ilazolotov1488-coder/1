package space.visuals.client.modules.impl.render.motionblur;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import space.visuals.utility.mixin.accessors.PostEffectProcessorPassesAccessor;
import space.visuals.utility.mixin.accessors.ShaderLoaderCacheAccessor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Обёртка над PostEffectProcessor для 1.21.4.
 * Использует GlUniform через ShaderProgram.
 */
public class PostEffectShader {
    private final Identifier location;
    private final Consumer<PostEffectShader> initCallback;
    private PostEffectProcessor processor;
    private boolean initialized = false;
    private boolean errored = false;

    private final Matrix4f mvInverse = new Matrix4f();
    private final Matrix4f projInverse = new Matrix4f();
    private final Matrix4f prevModelView = new Matrix4f();
    private final Matrix4f prevProjection = new Matrix4f();
    private float cameraPosX, cameraPosY, cameraPosZ;
    private float prevCameraPosX, prevCameraPosY, prevCameraPosZ;
    private float viewResX = 1.0f, viewResY = 1.0f;
    private float blendFactor = 0.5f;
    private float inverseSamples = 1.0f;
    private float handDepthThreshold = 0.56f;
    private int motionBlurSamples = 8;
    private int halfSamples = 4;
    private int blurAlgorithm = 1;

    public PostEffectShader(Identifier location, Consumer<PostEffectShader> initCallback) {
        this.location = location;
        this.initCallback = initCallback;
    }

    private void ensureInitialized() {
        if (initialized || errored) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ShaderLoader shaderLoader = client.getShaderLoader();
            ShaderLoader.Cache cache = ((ShaderLoaderCacheAccessor) shaderLoader).getCache();
            this.processor = cache.getOrLoadProcessor(location, DefaultFramebufferSet.MAIN_ONLY);
            this.initialized = true;
            this.initCallback.accept(this);
        } catch (Exception e) {
            this.errored = true;
        }
    }

    public void render(float tickDelta) {
        ensureInitialized();
        if (processor == null) return;

        // Устанавливаем uniforms через setUniforms
        processor.setUniforms("BlendFactor", blendFactor);
        processor.setUniforms("inverseSamples", inverseSamples);
        processor.setUniforms("handDepthThreshold", handDepthThreshold);

        // Для матриц и векторов используем GlUniform напрямую через ShaderProgram
        setMatrixAndVectorUniforms();

        MinecraftClient client = MinecraftClient.getInstance();
        processor.render(client.getFramebuffer(), net.minecraft.client.util.ObjectAllocator.TRIVIAL);
    }

    private void setMatrixAndVectorUniforms() {
        if (processor == null) return;
        List<PostEffectPass> passes = ((PostEffectProcessorPassesAccessor) processor).getPasses();
        if (passes.isEmpty()) return;

        PostEffectPass pass = passes.get(0);
        ShaderProgram program = pass.getProgram();
        if (program == null) return;

        setMat4(program, "mvInverse", mvInverse);
        setMat4(program, "projInverse", projInverse);
        setMat4(program, "prevModelView", prevModelView);
        setMat4(program, "prevProjection", prevProjection);
        setVec3(program, "cameraPos", cameraPosX, cameraPosY, cameraPosZ);
        setVec3(program, "prevCameraPos", prevCameraPosX, prevCameraPosY, prevCameraPosZ);
        setVec2(program, "view_res", viewResX, viewResY);
        setInt(program, "motionBlurSamples", motionBlurSamples);
        setInt(program, "halfSamples", halfSamples);
        setInt(program, "blurAlgorithm", blurAlgorithm);
    }

    private void setMat4(ShaderProgram program, String name, Matrix4f mat) {
        GlUniform u = program.getUniform(name);
        if (u != null) u.set(mat);
    }

    private void setVec3(ShaderProgram program, String name, float x, float y, float z) {
        GlUniform u = program.getUniform(name);
        if (u != null) u.set(x, y, z);
    }

    private void setVec2(ShaderProgram program, String name, float x, float y) {
        GlUniform u = program.getUniform(name);
        if (u != null) u.set(x, y);
    }

    private void setInt(ShaderProgram program, String name, int value) {
        GlUniform u = program.getUniform(name);
        if (u != null) u.set(value);
    }

    public void setBlendFactor(float v)          { this.blendFactor = v; }
    public void setViewRes(float x, float y)     { this.viewResX = x; this.viewResY = y; }
    public void setMotionBlurSamples(int v)      { this.motionBlurSamples = v; }
    public void setHalfSamples(int v)            { this.halfSamples = v; }
    public void setInverseSamples(float v)       { this.inverseSamples = v; }
    public void setBlurAlgorithm(int v)          { this.blurAlgorithm = v; }
    public void setHandDepthThreshold(float v)   { this.handDepthThreshold = v; }
    public void setMvInverse(Matrix4f m)         { this.mvInverse.set(m); }
    public void setProjInverse(Matrix4f m)       { this.projInverse.set(m); }
    public void setPrevModelView(Matrix4f m)     { this.prevModelView.set(m); }
    public void setPrevProjection(Matrix4f m)    { this.prevProjection.set(m); }
    public void setCameraPos(float x, float y, float z)     { this.cameraPosX = x; this.cameraPosY = y; this.cameraPosZ = z; }
    public void setPrevCameraPos(float x, float y, float z) { this.prevCameraPosX = x; this.prevCameraPosY = y; this.prevCameraPosZ = z; }

    public void reload() {
        this.processor = null;
        this.initialized = false;
        this.errored = false;
    }
}
