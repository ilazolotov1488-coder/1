package space.visuals.client.screens.menu.settings.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import space.visuals.client.modules.api.setting.impl.ItemSlotSetting;

/**
 * Менеджер выбора предмета из инвентаря для ItemSlotSetting.
 * Когда активен — перехватывает следующий клик по слоту инвентаря
 * и сохраняет предмет в настройку, не отправляя пакет на сервер.
 */
public final class ItemSlotPickerManager {

    public static final ItemSlotPickerManager INSTANCE = new ItemSlotPickerManager();

    private ItemSlotSetting pendingSetting = null;

    private ItemSlotPickerManager() {}

    /** Начать ожидание выбора предмета для данной настройки */
    public void startPicking(ItemSlotSetting setting) {
        this.pendingSetting = setting;
    }

    /** Активен ли режим выбора */
    public boolean isPicking() {
        return pendingSetting != null;
    }

    /**
     * Вызывается из HandledScreenMixin при клике по слоту.
     * Возвращает true если клик был перехвачен (нужно отменить стандартную обработку).
     */
    public boolean onClickSlot(Slot slot, SlotActionType actionType) {
        if (pendingSetting == null) return false;
        if (slot == null || slot.getStack().isEmpty()) {
            // Клик по пустому слоту — сбрасываем предмет
            pendingSetting.setStack(net.minecraft.item.ItemStack.EMPTY);
        } else {
            pendingSetting.setStack(slot.getStack());
        }
        pendingSetting = null;

        // Закрываем инвентарь и возвращаемся в меню
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.currentScreen != null) mc.currentScreen.close();
        });

        return true;
    }

    /** Отменить ожидание (например при закрытии инвентаря без выбора) */
    public void cancel() {
        pendingSetting = null;
    }
}
