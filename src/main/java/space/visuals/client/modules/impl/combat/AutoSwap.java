package space.visuals.client.modules.impl.combat;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;

import java.util.Comparator;

@ModuleAnnotation(
        name = "AutoSwap",
        category = Category.COMBAT,
        description = "Автоматический свап предметов"
)
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

        Slot first = PlayerInventoryUtil.getSlot(
                getItemByType(itemType.get()),
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                s -> s.id != 46 && s.id != 45
        );
        Slot second = PlayerInventoryUtil.getSlot(
                getItemByType(swapType.get()),
                Comparator.comparing(s -> s.getStack().hasEnchantments()),
                s -> s.id != 46 && s.id != 45
        );

        Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem()
                ? first : second;

        PlayerInventoryComponent.addTask(() -> {
            if (isWPressed()) {
                mc.options.forwardKey.setPressed(true);
            }
            if (isAPressed()) {
                mc.options.leftKey.setPressed(true);
            }
            if (isDPressed()) {
                mc.options.rightKey.setPressed(true);
            }
            if (isSPressed()) {
                mc.options.backKey.setPressed(true);
            }
            if (isJumpPressed()) {
                mc.options.jumpKey.setPressed(true);
            }

            if (swapTick >= 1) {
                PlayerInventoryUtil.swapHand(validSlot, Hand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
                startSwap = false;
                swapTick = 0;
            } else {
                swapTick++;
                mc.options.jumpKey.setPressed(false); // Jump
                mc.options.forwardKey.setPressed(false); // W
                mc.options.leftKey.setPressed(false); // A
                mc.options.rightKey.setPressed(false); // D
                mc.options.backKey.setPressed(false); // S
            }
        });
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
