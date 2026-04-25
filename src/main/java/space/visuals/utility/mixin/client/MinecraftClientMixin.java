package space.visuals.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventSetScreen;
import space.visuals.base.events.impl.other.EventWindowResize;
import space.visuals.client.modules.impl.misc.FakePlayer;

import static space.visuals.utility.interfaces.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract Window getWindow();

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!FakePlayer.INSTANCE.isEnabled()) return;
        var fake = FakePlayer.INSTANCE.getFakePlayer();
        if (fake == null || mc.player == null) return;
        var eyePos = mc.player.getEyePos();
        var lookVec = mc.player.getRotationVec(1.0f);
        double reach = mc.player.getBlockInteractionRange() + 2.0;
        var end = eyePos.add(lookVec.multiply(reach));
        var box = fake.getBoundingBox().expand(0.3);
        var hit = box.raycast(eyePos, end);
        if (hit.isPresent()) {
            FakePlayer.INSTANCE.onAttack();
            // Анимация удара рукой
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateTargetedEntity", at = @At("RETURN"))
    private void onUpdateTargetedEntity(CallbackInfo ci) {
        if (!FakePlayer.INSTANCE.isEnabled()) return;
        var fake = FakePlayer.INSTANCE.getFakePlayer();
        if (fake == null || mc.player == null) return;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) return;
        var eyePos = mc.player.getEyePos();
        var lookVec = mc.player.getRotationVec(1.0f);
        double reach = mc.player.getBlockInteractionRange() + 2.0;
        var end = eyePos.add(lookVec.multiply(reach));
        var box = fake.getBoundingBox().expand(0.3);
        var hit = box.raycast(eyePos, end);
        if (hit.isPresent()) {
            mc.crosshairTarget = new EntityHitResult(fake, hit.get());
            mc.targetedEntity = fake; // для TargetHUD
        } else if (mc.targetedEntity == fake) {
            mc.targetedEntity = null;
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient$1;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/RunArgs;)V"))
    public void init(RunArgs args, CallbackInfo ci) {
        // init() is now called via ZenithInitializer (ClientModInitializer)
    }
    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {

    }
    @ModifyVariable(
            method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Screen mixin$modifySetScreenArg(Screen original) {

        EventSetScreen event = new EventSetScreen(original);

        EventManager.call(event);
        return event.getScreen();
    }
}
