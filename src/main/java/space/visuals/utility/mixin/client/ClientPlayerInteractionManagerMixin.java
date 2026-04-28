package space.visuals.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.base.events.impl.other.EventClickSlot;
import space.visuals.base.events.impl.player.EventAttack;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    public void attackEntityPre(PlayerEntity player, Entity target, CallbackInfo ci) {
        EventManager.call(new EventAttack(target, EventAttack.Action.PRE));
    }

    @Inject(method = "attackEntity", at = @At("RETURN"))
    public void attackEntityPost(PlayerEntity player, Entity target, CallbackInfo ci) {
        EventManager.call(new EventAttack(target, EventAttack.Action.POST));
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
        EventClickSlot event = new EventClickSlot(syncId,slotId,button,actionType);
        EventManager.call(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
    }

}
