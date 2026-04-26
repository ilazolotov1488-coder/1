package space.visuals.utility.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.ShaderHands;
import space.visuals.client.modules.impl.render.SwingAnimation;
import space.visuals.client.modules.impl.render.ViewModel;
import space.visuals.utility.render.item.ShaderHandsRenderState;

import static space.visuals.utility.interfaces.IMinecraft.mc;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);


//    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"), cancellable = true)
//    private void onRenderItemHook(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
//        if (!(item.isEmpty()) && !(item.getItem() instanceof FilledMapItem) ) {
//            SwingAnimation swingAnimation = SwingAnimation.INSTANCE;
//            if (swingAnimation.isEnabled() || ViewModel.INSTANCE.isEnabled()) { //мы не можем гарантировать что наш рендер предмета работать правильно с учетом
//                ci.cancel();
//                swingAnimation.renderFirstPersonItem(player, tickDelta, pitch, hand
//                        , swingProgress, item, equipProgress, matrices, vertexConsumers, light);
//            }
//        }
//    }


    @Inject(
            method = "renderFirstPersonItem",
            at = @At(value = "HEAD"))
    private void beginShaderHandsTint(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ShaderHands sh = ShaderHands.INSTANCE;
        if (!sh.shouldShaderHands() || item.isEmpty() || item.contains(DataComponentTypes.MAP_ID)) return;
        float brightness = sh.getHandBrightness();
        float r = Math.min(1f, sh.getHandColor().getRed()   / 255f * brightness);
        float g = Math.min(1f, sh.getHandColor().getGreen() / 255f * brightness);
        float b = Math.min(1f, sh.getHandColor().getBlue()  / 255f * brightness);
        ShaderHandsRenderState.begin(r, g, b, sh.getHandCombinedAlpha());
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(value = "RETURN"))
    private void endShaderHandsTint(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ShaderHands sh = ShaderHands.INSTANCE;
        if (sh.shouldShaderHands() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID))
            ShaderHandsRenderState.end();
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 0
            )
    )
    public void injectBeforeRenderCrossBowItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandScale(matrices, arm);
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 1
            )
    )
    public void injectBeforeRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandScale(matrices, arm);
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandPosition(matrices, arm);
        }
    }


    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        SwingAnimation swingAnimation = SwingAnimation.INSTANCE;
        if (swingAnimation.isEnabled()) {
            if (arm == Arm.RIGHT) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            this.swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }

    //будем мечтать что фрик найдет середину для скейла правильного
//    @Inject(method = "renderArmHoldingItem",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",shift = At.Shift.AFTER,ordinal = 1))
//    public void injectRenderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci){
//        ViewModel viewModel = ViewModel.INSTANCE;
//        if(MinecraftClient.getInstance().player!=null &&viewModel.isEnabled() ) {
//            if(MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof FilledMapItem){
//                return;
//            }
//            viewModel.applyHandPosition(matrices,arm);
//            viewModel.applyHandScale(matrices,arm);
//
//
//        }
//
//    }
}
