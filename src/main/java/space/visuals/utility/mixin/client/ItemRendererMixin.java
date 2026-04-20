package space.visuals.utility.mixin.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import space.visuals.utility.render.item.ShaderHandsRenderState;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Redirect(
            method = "renderBakedItemQuads",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFII)V"))
    private static void applyShaderHandsTint(VertexConsumer instance, MatrixStack.Entry matrixEntry, BakedQuad quad,
                                              float red, float green, float blue, float alpha, int light, int overlay) {
        if (!ShaderHandsRenderState.isActive()) {
            instance.quad(matrixEntry, quad, red, green, blue, alpha, light, overlay);
            return;
        }
        instance.quad(matrixEntry, quad,
                ShaderHandsRenderState.tintRed(red),
                ShaderHandsRenderState.tintGreen(green),
                ShaderHandsRenderState.tintBlue(blue),
                ShaderHandsRenderState.tintAlpha(alpha),
                light, overlay);
    }
}
