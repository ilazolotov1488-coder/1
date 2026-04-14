package space.visuals.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.client.modules.impl.misc.AHHelper;
import space.visuals.client.modules.impl.render.AnimationModule;
import space.visuals.utility.game.server.AutoBuyUtil;
import space.visuals.utility.mixin.accessors.HandledScreenAccessor;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Unique @Mutable private boolean isAuc;
    @Unique @Mutable private Slot lowSumSlotId = null;
    @Unique @Mutable private Slot lowAllSumSlotId = null;

    @Unique private final Animation openAnim = new Animation(250, 0, Easing.QUAD_OUT);
    @Unique private boolean animInit = false;
    @Unique private boolean animPushed = false;

    @Shadow public abstract ScreenHandler getScreenHandler();
    @Shadow @Final protected ScreenHandler handler;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        animInit = false;
        animPushed = false;
    }

    // Анимируем только содержимое инвентаря — после renderBackground (блюр остаётся на весь экран)
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            shift = At.Shift.AFTER))
    private void onRenderPre(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!AnimationModule.INSTANCE.isEnabled() || !AnimationModule.INSTANCE.animateInventory.isEnabled()) return;
        if (!animInit) { openAnim.reset(0); animInit = true; }
        float scale = openAnim.update(1);
        if (scale >= 1f) { animPushed = false; return; }

        HandledScreenAccessor acc = (HandledScreenAccessor)(Object)this;
        float cx = acc.getX() + backgroundWidth  / 2f;
        float cy = acc.getY() + backgroundHeight / 2f;

        MatrixStack ms = context.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-cx, -cy, 0);
        animPushed = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!animPushed) return;
        context.getMatrices().pop();
        animPushed = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickScreen(CallbackInfo ci) {
        if (!isAuc && AHHelper.INSTANCE.isEnabled()) isAuc = AutoBuyUtil.isAuction(this.handler);
        if (isAuc && AHHelper.INSTANCE.isEnabled()) {
            int lowSum = Integer.MAX_VALUE, allSum = Integer.MAX_VALUE;
            for (int i = 0; i < 44; i++) {
                Slot slot = this.getScreenHandler().slots.get(i);
                if (slot.getStack().isEmpty()) continue;
                int sum = AutoBuyUtil.getPrice(slot.getStack());
                if (sum < lowSum) { lowSumSlotId = slot; lowSum = sum; }
                if (sum / slot.getStack().getCount() < allSum) { allSum = sum / slot.getStack().getCount(); lowAllSumSlotId = slot; }
            }
        }
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("HEAD"))
    private void onDrawSlotInject(DrawContext context, Slot slot, CallbackInfo ci) {
        if (AHHelper.INSTANCE.isEnabled()) {
            if (slot == lowSumSlotId) AHHelper.INSTANCE.renderCheat(context, slot);
            else if (slot == lowAllSumSlotId) AHHelper.INSTANCE.renderGood(context, slot);
        }
    }
}
