package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.KeySetting;

import java.util.List;

@ModuleAnnotation(name = "AutoSwap", category = Category.COMBAT, description = "Автоматический свап предметов")
public final class AutoSwap extends Module {
    public static final AutoSwap INSTANCE = new AutoSwap();

    private final ModeSetting itemType = new ModeSetting("Предмет", "Тотем", "Шар");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Шар", "Тотем");
    private final KeySetting keyToSwap = new KeySetting("Кнопка", -1);

    // Состояние (как в оригинале)
    private int step = 0;   // 0=idle, 1=открыть+ждать, 2=своп+ждать, 3=закрыть
    private int aka  = 0;   // счётчик задержки
    private int lk   = -1;  // server-side id слота
    private boolean auj = false; // был ли инвентарь открыт до свапа
    private ItemStack pendingStack = ItemStack.EMPTY; // для уведомления

    private AutoSwap() {}

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return;
        if (!event.is(keyToSwap.getKeyCode())) return;
        startSwap();
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (step == 0 || mc.player == null) return;
        if (aka > 0) { aka--; return; }

        switch (step) {
            case 1 -> {
                // Открываем инвентарь если не открыт, затем переходим к свапу
                if (!auj) mc.setScreen(new InventoryScreen(mc.player));
                p(0); // delay=0 — сразу следующий тик
            }
            case 2 -> {
                // Свап: clickSlot(syncId, lk, 40, SWAP)
                doSwap(lk);
                p(0); // delay=0 — сразу следующий тик
            }
            case 3 -> {
                // Закрываем инвентарь
                if (!auj && mc.currentScreen instanceof InventoryScreen) {
                    mc.currentScreen.close();
                }
                // Уведомление
                if (!pendingStack.isEmpty()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(pendingStack);
                    pendingStack = ItemStack.EMPTY;
                }
                reset();
            }
        }
    }

    private void p(int i) {
        step++;
        aka = i;
    }

    private void doSwap(int slotId) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slotId, 40, SlotActionType.SWAP, mc.player
        );
    }

    private void startSwap() {
        if (mc.player == null || step != 0) return;

        Item item1 = getItemByType(itemType.get());
        Item item2 = getItemByType(swapType.get());
        Item offhand = mc.player.getOffHandStack().getItem();
        Item target = (offhand == item1) ? item2 : item1;

        int slotId = findSlot(target);
        if (slotId == -1) return;

        // Сохраняем стак для уведомления
        for (Slot s : mc.player.currentScreenHandler.slots) {
            if (s.id == slotId) {
                pendingStack = s.getStack().copy();
                break;
            }
        }

        lk  = slotId;
        auj = mc.currentScreen instanceof InventoryScreen;
        step = 1;
        aka  = 0;
    }

    // Хотбар (36-44) приоритетнее инвентаря (9-35) — как в оригинале
    private int findSlot(Item item) {
        if (mc.player == null) return -1;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        for (Slot s : slots) {
            if (s.id >= 36 && s.id <= 44 && s.getStack().getItem() == item) return s.id;
        }
        for (Slot s : slots) {
            if (s.id >= 9 && s.id <= 35 && s.getStack().getItem() == item) return s.id;
        }
        return -1;
    }

    private void reset() {
        step = 0;
        aka  = 0;
        lk   = -1;
        auj  = false;
    }

    @Override
    public void onDisable() {
        if (!auj && mc.currentScreen instanceof InventoryScreen) mc.currentScreen.close();
        reset();
        pendingStack = ItemStack.EMPTY;
        super.onDisable();
    }

    private Item getItemByType(String type) {
        return switch (type) {
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Шар"   -> Items.PLAYER_HEAD;
            default      -> Items.AIR;
        };
    }
}
