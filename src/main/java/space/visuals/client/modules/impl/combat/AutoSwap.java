package space.visuals.client.modules.impl.combat;

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
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(name = "AutoSwap", category = Category.COMBAT, description = "Автоматический свап предметов")
public final class AutoSwap extends Module {
    public static final AutoSwap INSTANCE = new AutoSwap();

    private final ModeSetting itemType = new ModeSetting("Предмет", "Щит", "Геплы", "Тотем", "Шар");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Щит", "Геплы", "Тотем", "Шар");
    private final KeySetting keyToSwap = new KeySetting("Кнопка", -1);

    boolean startSwap = false;
    int swapTick;

    private AutoSwap() {}

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return;
        if (event.is(keyToSwap.getKeyCode())) {
            startSwap = true;
        }
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
        if (!startSwap) return;

        Item currentOffhand = mc.player.getOffHandStack().getItem();
        Item item1 = getItemByType(itemType.get());
        Item item2 = getItemByType(swapType.get());

        // Определяем цель: если в оффхенде item1 — берём item2, иначе item1
        Item targetItem = (currentOffhand == item1) ? item2 : item1;

        // Поиск в порядке: хотбар (36-44), потом инвентарь строчками сверху вниз (9-35)
        // Только точное совпадение по типу предмета — никаких других предметов
        Slot validSlot = findNearestSlot(targetItem);

        // Если предмет не найден или уже в оффхенде — отменяем
        if (validSlot == null) {
            startSwap = false;
            swapTick = 0;
            return;
        }

        // Если предмет в хотбаре — не берём его (он там нужен игроку)
        // Берём только если это именно тот тип что настроен
        if (validSlot.getStack().getItem() != targetItem) {
            startSwap = false;
            swapTick = 0;
            return;
        }

        // Добавляем задачу только если очередь пуста (не спамим)
        if (!space.visuals.Zenith.getInstance().getScriptManager().isFinished()) return;

        // Сразу сбрасываем флаг — задача будет добавлена один раз
        startSwap = false;

        PlayerInventoryComponent.addTask(() -> {
            if (isWPressed()) mc.options.forwardKey.setPressed(true);
            if (isAPressed()) mc.options.leftKey.setPressed(true);
            if (isDPressed()) mc.options.rightKey.setPressed(true);
            if (isSPressed()) mc.options.backKey.setPressed(true);
            if (isJumpPressed()) mc.options.jumpKey.setPressed(true);

            if (swapTick >= 1) {
                ItemStack swappedStack = validSlot != null ? validSlot.getStack().copy() : ItemStack.EMPTY;
                PlayerInventoryUtil.swapHand(validSlot, Hand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
                if (!swappedStack.isEmpty()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(swappedStack);
                }
                swapTick = 0;
            } else {
                swapTick++;
                // Не сбрасываем startSwap здесь — он уже сброшен выше
                mc.options.jumpKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                // Нужно ещё один тик — возвращаем флаг
                startSwap = true;
            }
        });
    }

    /**
     * Ищет предмет в порядке близости:
     * 1. Хотбар (слоты 36-44) — слева направо
     * 2. Инвентарь строчками сверху вниз (слоты 9-35)
     * Только точное совпадение по Item — никаких других предметов.
     */
    private Slot findNearestSlot(Item targetItem) {
        if (mc.player == null) return null;
        java.util.List<net.minecraft.screen.slot.Slot> slots = mc.player.currentScreenHandler.slots;

        // 1. Хотбар (36-44), предпочитаем зачарованный
        net.minecraft.screen.slot.Slot hotbarEnchanted = null;
        net.minecraft.screen.slot.Slot hotbarPlain = null;
        for (net.minecraft.screen.slot.Slot s : slots) {
            if (s.id >= 36 && s.id <= 44 && s.getStack().getItem() == targetItem) {
                if (s.getStack().hasEnchantments()) {
                    if (hotbarEnchanted == null) hotbarEnchanted = s;
                } else {
                    if (hotbarPlain == null) hotbarPlain = s;
                }
            }
        }
        if (hotbarEnchanted != null) return hotbarEnchanted;
        if (hotbarPlain != null) return hotbarPlain;

        // 2. Инвентарь строчками сверху вниз (9-35), предпочитаем зачарованный
        net.minecraft.screen.slot.Slot invEnchanted = null;
        net.minecraft.screen.slot.Slot invPlain = null;
        for (net.minecraft.screen.slot.Slot s : slots) {
            if (s.id >= 9 && s.id <= 35 && s.getStack().getItem() == targetItem) {
                if (s.getStack().hasEnchantments()) {
                    if (invEnchanted == null) invEnchanted = s;
                } else {
                    if (invPlain == null) invPlain = s;
                }
            }
        }
        if (invEnchanted != null) return invEnchanted;
        if (invPlain != null) return invPlain;

        return null;
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
