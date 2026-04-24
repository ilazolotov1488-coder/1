package space.visuals.utility.mixin.client;

import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.MotionBlur;
import space.visuals.utility.mixin.accessors.GameRendererFovAccessor;

@Mixin(WorldRenderer.class)
public class MotionBlurWorldRendererMixin {

    @Unique private Matrix4f mb_prevModelView = new Matrix4f();
    @Unique private Matrix4f mb_prevProjection = new Matrix4f();
    @Unique private Vector3f mb_prevCameraPos = new Vector3f();

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void mb_onRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                  boolean renderBlockOutline, Camera camera,
                                  Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                  Object fog, Vector4f fogColor,
                                  boolean shouldRenderSky, CallbackInfo ci) {
        if (!MotionBlur.INSTANCE.isEnabled() || MotionBlur.INSTANCE.getShader() == null) return;

        float td = tickCounter.getTickDelta(true);
        GameRenderer gr = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
        float fov = ((GameRendererFovAccessor) gr).invokeGetFov(camera, td, true);

        MotionBlur.INSTANCE.getShader().setFrameData(
                positionMatrix, mb_prevModelView,
                gr.getBasicProjectionMatrix(fov), mb_prevProjection,
                new Vector3f(
                        (float)(camera.getPos().x % 30000f),
                        (float)(camera.getPos().y % 30000f),
                        (float)(camera.getPos().z % 30000f)
                ),
                mb_prevCameraPos
        );
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void mb_onRenderReturn(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                    boolean renderBlockOutline, Camera camera,
                                    Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                    Object fog, Vector4f fogColor,
                                    boolean shouldRenderSky, CallbackInfo ci) {
        if (!MotionBlur.INSTANCE.isEnabled()) return;

        mb_prevModelView = new Matrix4f(positionMatrix);
        GameRenderer gr = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
        float td = tickCounter.getTickDelta(true);
        float fov = ((GameRendererFovAccessor) gr).invokeGetFov(camera, td, true);
        mb_prevProjection = new Matrix4f(gr.getBasicProjectionMatrix(fov));
        mb_prevCameraPos = new Vector3f(
                (float)(camera.getPos().x % 30000f),
                (float)(camera.getPos().y % 30000f),
                (float)(camera.getPos().z % 30000f)
        );
    }
}
