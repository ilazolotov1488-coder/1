package space.visuals.utility.mixin.client;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.MotionBlur;

@Mixin(value = GameRenderer.class, priority = 1100)
public class MotionBlurGameRendererMixin {

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V",
            shift = At.Shift.BEFORE
        ),
        require = 0
    )
    private void beforeRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!MotionBlur.INSTANCE.isEnabled()) return;
        if (MotionBlur.INSTANCE.getShader() == null) return;
        MotionBlur.INSTANCE.getShader().applyBeforeHands();
    }
}
