package space.visuals.utility.mixin.client.render;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.Zenith;
import space.visuals.base.events.impl.entity.EventEntityColor;
import space.visuals.client.modules.impl.render.CustomModelType;
import space.visuals.client.modules.impl.render.CustomModels;
import space.visuals.utility.interfaces.IMinecraft;
import space.visuals.utility.render.entity.CustomModelsRenderer;
import space.visuals.utility.render.entity.RenderStateEntityCache;
import space.visuals.utility.render.level.Render3DUtil;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {

    // Сохраняем entity в кэш при обновлении render state — нужно для CustomModels
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void storeRenderStateEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
        RenderStateEntityCache.put(state, entity);
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    public float changeYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getYaw(),Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    public float changeHeadYaw(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player)&&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getYaw(),Zenith.getInstance().getRotationManager().getCurrentRotation().getYaw());
        }
        return oldValue;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    public float changePitch(float oldValue, LivingEntity entity) {
        if (entity.equals(mc.player) &&!Zenith.getInstance().getRotationManager().isSetRotation()) {
            return   MathHelper.lerpAngleDegrees(Render3DUtil.getTickDelta(),Zenith.getInstance().getRotationManager().getPreviousRotation().getPitch(),Zenith.getInstance().getRotationManager().getCurrentRotation().getPitch());

        }
        return oldValue;
    }
    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        // CustomModels: подменяем текстуру на текстуру кастомной модели
        CustomModels customModels = CustomModels.INSTANCE;
        if (customModels.isEnabled()) {
            LivingEntity entity = RenderStateEntityCache.get(state);
            CustomModelType type;
            if (entity != null && customModels.shouldApplyTo(entity) && (type = customModels.getSelectedType()) != null) {
                // Возвращаем слой с текстурой кастомной модели вместо скина игрока
                return showOutline
                        ? RenderLayer.getOutline(type.getTexture())
                        : RenderLayer.getEntityTranslucent(type.getTexture());
            }
        }

        if (!translucent && state.width == 0.6F) {
            EventEntityColor event = new EventEntityColor(-1);
            EventManager.call(event);
            if (event.isCancelled()) translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        EventEntityColor event = new EventEntityColor(l);
        if (renderState.invisibleToPlayer) EventManager.call(event);

        // CustomModels: заменяем модель если нужно
        CustomModels customModels = CustomModels.INSTANCE;
        if (customModels.isEnabled()) {
            LivingEntity entity = RenderStateEntityCache.get(renderState);
            CustomModelType type;
            if (entity != null && customModels.shouldApplyTo(entity) && (type = customModels.getSelectedType()) != null
                    && CustomModelsRenderer.render(type, instance, matrixStack, vertexConsumer, i, j, event.getColor())) {
                return;
            }
        }

        instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
    }

}
