package space.visuals.utility.mixin.minecraft.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.EntityESP;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    private static final float CUSTOM_TAG_DISTANCE_SQ = 30f * 30f; // 900

    @Inject(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void render(PlayerEntityRenderState state, Text text, MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (!EntityESP.INSTANCE.isEnabled() || !EntityESP.INSTANCE.isRenderName()) return;

        // Ближе 30 блоков — скрываем ванильный, рисуем свой кастомный
        if (state.squaredDistanceToCamera <= CUSTOM_TAG_DISTANCE_SQ) {
            ci.cancel();
        }
        // Дальше 30 блоков — ванильный нейм тег остаётся
    }
}
