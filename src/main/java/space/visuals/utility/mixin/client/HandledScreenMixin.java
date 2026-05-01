package space.visuals.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.base.events.impl.render.EventHandledScreen;
import space.visuals.client.modules.impl.combat.SwapPlus;
import space.visuals.client.screens.menu.settings.impl.ItemSlotPickerManager;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.call(new EventHandledScreen(context, focusedSlot, backgroundWidth, backgroundHeight));
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // ItemSlotPickerManager — выбор предмета для ItemSlotSetting
        if (ItemSlotPickerManager.INSTANCE.isPicking()) {
            ItemSlotPickerManager.INSTANCE.onClickSlot(slot, actionType);
            ci.cancel();
            return;
        }
        if (slot != null && SwapPlus.INSTANCE.isPendingPick()) {
            // Отменяем клик на сервер - только сохраняем предмет в колесо клиентски
            SwapPlus.INSTANCE.onClickSlot(slot.id, actionType);
            ci.cancel();
        }
    }
}
