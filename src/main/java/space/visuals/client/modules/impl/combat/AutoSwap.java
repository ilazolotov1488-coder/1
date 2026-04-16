package space.visuals.client.modules.impl.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;

import java.util.Comparator;

@ModuleAnnotation(name = "AutoSwap", category = Category.COMBAT, description = "Автоматический свап предметов")
public final class AutoSwap extends Module {
    public static final AutoSwap INSTANCE = new AutoSwap();

    private final ModeSetting itemType = new ModeSetting("Предмет", "Щит", "Геплы", "Тотем", "Шар");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Щит", "Геплы", "Тотем", "Шар");
    private final KeySetting keyToSwap = new KeySetting("Кнопка", -1);
    // Только зачарованный тотем — работает для обоих слотов если выбран Тотем
    private final BooleanSetting enchantedTotem = new BooleanSetting("Только зачарованный тотем", false);

    private boolean startSwap = false;
    private int swapTick = 0;
    private int lockedSlotId = -1;
    private Item lockedItem = null;

    private AutoSwap() {}

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return;
        if (!event.is(keyToSwap.getKeyCode())) return;

        Item currentOffhand = mc.player.getOffHandStack().getItem();
        Item item1 = getItemByType(itemType.get());
        Item item2 = getItemByType(swapType.get());

        // Определяем целевой предмет
        Item targetItem = (currentOffhand == item1) ? item2 : item1;

        Slot target = findSlot(targetItem);
        if (target == null) return;

        lockedSlotId = target.id;
        lockedItem = targetItem; // строго тот тип что выбран
        startSwap = true;
        swapTick = 0;
    }

    /**
     * Ищет слот строго по типу предмета.
     * Если включён "Только зачарованный тотем" и цель — тотем — ищет только зачарованный.
     */
    private Slot findSlot(Item targetItem) {
        // Только стандартный инвентарь + хотбар (9-44), без оффхенда и серверных слотов
        java.util.function.Predicate<Slot> baseFilter = s ->
                s.id >= 9 && s.id <= 44
                && s.getStack().getItem() == targetItem; // строгое совпадение по типу

        if (enchantedTotem.isEnabled() && targetItem == Items.TOTEM_OF_UNDYING) {
            // Ищем только зачарованный тотем
            Slot enchanted = PlayerInventoryUtil.getSlot(
                    targetItem,
                    Comparator.comparing(s -> {
                        var enc = s.getStack().get(DataComponentTypes.ENCHANTMENTS);
                        return enc != null && !enc.isEmpty();
                    }),
                    s -> baseFilter.test(s)
                            && s.getStack().get(DataComponentTypes.ENCHANTMENTS) != null
                            && !s.getStack().get(DataComponentTypes.ENCHANTMENTS).isEmpty()
            );
            return enchanted; // null если нет зачарованного — свап не происходит
        }

        // Обычный поиск: предпочитаем зачарованный
        return PlayerInventoryUtil.getSlot(
                targetItem,
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                baseFilter
        );
    }

    public boolean isWPressed() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
    }
    public boolean isAPressed() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
    }
    public boolean isDPressed() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
    }
    public boolean isSPressed() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
    }
    public boolean isJumpPressed() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (!startSwap || lockedSlotId == -1) return;

        // Не добавляем задачу если очередь занята
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        if (swapTick < 2) {
            // Тики остановки — добавляем задачу остановки
            swapTick++;
            PlayerInventoryComponent.addTask(() -> {
                mc.options.jumpKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.backKey.setPressed(false);
            });
        } else {
            // Тик свапа
            Slot slot = mc.player.currentScreenHandler.slots.stream()
                    .filter(s -> s.id == lockedSlotId && s.getStack().getItem() == lockedItem)
                    .findFirst().orElse(null);

            startSwap = false;
            swapTick = 0;
            lockedSlotId = -1;
            lockedItem = null;

            if (slot != null) {
                ItemStack swappedStack = slot.getStack().copy();
                PlayerInventoryComponent.addTask(() -> {
                    PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, false);
                    PlayerInventoryUtil.closeScreen(true);
                    // Восстанавливаем движение
                    if (isWPressed()) mc.options.forwardKey.setPressed(true);
                    if (isAPressed()) mc.options.leftKey.setPressed(true);
                    if (isDPressed()) mc.options.rightKey.setPressed(true);
                    if (isSPressed()) mc.options.backKey.setPressed(true);
                    if (isJumpPressed()) mc.options.jumpKey.setPressed(true);
                });
                if (!swappedStack.isEmpty()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(swappedStack);
                }
            }
        }
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар" -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }
}
