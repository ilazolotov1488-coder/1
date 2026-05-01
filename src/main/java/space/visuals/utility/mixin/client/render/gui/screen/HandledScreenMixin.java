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
import space.visuals.client.screens.menu.settings.impl.ItemSlotPickerManager;
import space.visuals.client.modules.impl.misc.AHHelper;
import space.visuals.client.modules.impl.render.AnimationModule;
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

    /** Если Animation включён — пропускаем тёмный оверлей (method_52752), серый фон инвентаря остаётся */
    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "renderBackground",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_465;method_52752(Lnet/minecraft/class_332;)V"))
    private void skipDarkOverlay(net.minecraft.client.gui.screen.ingame.HandledScreen instance, DrawContext context) {
        if (!AnimationModule.INSTANCE.isEnabled() || !AnimationModule.INSTANCE.animateInventory.isEnabled()) {
            // Анимация выключена — вызываем оригинал через reflection
            try {
                java.lang.reflect.Method m = net.minecraft.client.gui.screen.ingame.HandledScreen.class
                    .getDeclaredMethod("method_52752", DrawContext.class);
                m.setAccessible(true);
                m.invoke(instance, context);
            } catch (Exception ignored) {}
        }
        // Анимация включена — пропускаем тёмный оверлей
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        animInit = false;
        animPushed = false;
    }

    /** Если Animation включён — отменяем только блюр инвентаря, фон остаётся */

    /**
     * Анимируем весь render инвентаря из центра.
     * Блюр рисуется через InGameHudMixin отдельно и не попадает в эту матрицу.
     */
    @Inject(method = "render", at = @At("HEAD"))
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
        if (!isAuc && AHHelper.INSTANCE.isEnabled()) isAuc = isAuction(this.handler);
        if (isAuc && AHHelper.INSTANCE.isEnabled()) {
            int lowSum = Integer.MAX_VALUE, allSum = Integer.MAX_VALUE;
            for (int i = 0; i < 44; i++) {
                Slot slot = this.getScreenHandler().slots.get(i);
                if (slot.getStack().isEmpty()) continue;
                int sum = getSlotPrice(slot.getStack());
                if (sum < lowSum) { lowSumSlotId = slot; lowSum = sum; }
                if (sum / slot.getStack().getCount() < allSum) { allSum = sum / slot.getStack().getCount(); lowAllSumSlotId = slot; }
            }
        }
    }

    private boolean isAuction(net.minecraft.screen.ScreenHandler h) {
        return h.slots.size() == 90 && h.getSlot(49).getStack().getItem() == net.minecraft.item.Items.NETHER_STAR;
    }

    private int getSlotPrice(net.minecraft.item.ItemStack stack) {
        try {
            net.minecraft.component.type.NbtComponent customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            if (customData == null) return Integer.MAX_VALUE;
            String nbt = customData.getNbt().toString();
            java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("\\$\\s*([0-9][\\d,]*)").matcher(nbt);
            if (m1.find()) return Integer.parseInt(m1.group(1).replace(",", ""));
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")").matcher(nbt);
            if (m2.find()) return Integer.parseInt(m2.group(1).replaceAll(" ", ""));
        } catch (Exception ignored) {}
        return Integer.MAX_VALUE;
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("HEAD"))
    private void onDrawSlotInject(DrawContext context, Slot slot, CallbackInfo ci) {
        if (AHHelper.INSTANCE.isEnabled()) {
            if (slot == lowSumSlotId) AHHelper.INSTANCE.renderCheat(context, slot);
            else if (slot == lowAllSumSlotId) AHHelper.INSTANCE.renderGood(context, slot);
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSlotClick(Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, CallbackInfo ci) {
        if (!ItemSlotPickerManager.INSTANCE.isPicking()) return;
        boolean handled = ItemSlotPickerManager.INSTANCE.onClickSlot(slot, actionType);
        if (handled) ci.cancel();
    }
}
