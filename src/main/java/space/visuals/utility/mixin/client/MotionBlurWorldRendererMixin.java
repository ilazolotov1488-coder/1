package space.visuals.utility.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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

    @Inject(
        method = "render",
        at = @At("HEAD"),
        require = 0
    )
    private void mb_onRenderHead(CallbackInfo ci) {
        if (!MotionBlur.INSTANCE.isEnabled() || MotionBlur.INSTANCE.getShader() == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        RenderTickCounter tickCounter = mc.getRenderTickCounter();
        
        float td = tickCounter.getTickDelta(true);
        GameRenderer gr = mc.gameRenderer;
        float fov = ((GameRendererFovAccessor) gr).invokeGetFov(camera, td, true);

        // Получаем матрицы из RenderSystem
        Matrix4f positionMatrix = RenderSystem.getModelViewMatrix();

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

    @Inject(
        method = "render",
        at = @At("RETURN"),
        require = 0
    )
    private void mb_onRenderReturn(CallbackInfo ci) {
        if (!MotionBlur.INSTANCE.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        RenderTickCounter tickCounter = mc.getRenderTickCounter();

        mb_prevModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        GameRenderer gr = mc.gameRenderer;
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
