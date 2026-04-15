package space.visuals.utility.mixin.client.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.CustomModels;
import space.visuals.utility.render.entity.RenderStateEntityCache;

/**
 * Скрывает броню когда активен CustomModels.
 */
@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideArmorForCustomModels(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                          int light, BipedEntityRenderState state,
                                          float limbAngle, float limbDistance, CallbackInfo ci) {
        CustomModels customModels = CustomModels.INSTANCE;
        if (!customModels.isEnabled()) return;

        LivingEntity entity = RenderStateEntityCache.get(state);
        if (entity != null && customModels.shouldApplyTo(entity)) {
            ci.cancel();
        }
    }
}