package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.request.ScriptManager;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerIntersectionUtil;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.utility.game.player.rotation.Rotation;

import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleAnnotation(name = "ElytraHelper", description = "Помощник для элитр", category = Category.MISC)
public final class ElytraHelper extends Module {
    public static final ElytraHelper INSTANCE = new ElytraHelper();

    private final KeySetting elytraSetting = new KeySetting("Кнопка свапа");
    private final KeySetting fireworkSetting = new KeySetting("Кнопка фейерверка");

    // Состояние свапа элитры (механика AutoSwap)
    boolean startElytraSwap = false;
    int elytraSwapTick = 0;
    private Slot pendingElytraSlot = null;

    private int fireworkCooldown = 0;

    public boolean isSwapping() { return startElytraSwap; }

    private ElytraHelper() {}

    private boolean isWPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W)     == GLFW.GLFW_PRESS; }
    private boolean isAPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_A)     == GLFW.GLFW_PRESS; }
    private boolean isDPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_D)     == GLFW.GLFW_PRESS; }
    private boolean isSPressed()    { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_S)     == GLFW.GLFW_PRESS; }
    private boolean isJumpPressed() { return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS; }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.isKeyDown(elytraSetting.getKeyCode()) && !startElytraSwap) {
            Slot slot = chestPlate();
            if (slot != null) {
                // Уведомление о свапе элитры
                if (space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.isEnabled()
                        && space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.elytraHelper.isEnabled()) {
                    Zenith.getInstance().getNotifyManager().addSwapNotification(slot.getStack());
                }
                pendingElytraSlot = slot;
                startElytraSwap = true;
                elytraSwapTick = 0;
            }
        } else if (e.isKeyDown(fireworkSetting.getKeyCode()) && mc.player.isGliding() && fireworkCooldown <= 0) {
            useFirework();
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (fireworkCooldown > 0) fireworkCooldown--;

        if (!startElytraSwap || pendingElytraSlot == null) return;

        PlayerInventoryComponent.addTask(() -> {
            // Восстанавливаем клавиши движения после сброса
            if (isWPressed())    mc.options.forwardKey.setPressed(true);
            if (isAPressed())    mc.options.leftKey.setPressed(true);
            if (isDPressed())    mc.options.rightKey.setPressed(true);
            if (isSPressed())    mc.options.backKey.setPressed(true);
            if (isJumpPressed()) mc.options.jumpKey.setPressed(true);

            if (elytraSwapTick >= 2) {
                // Свап: открываем инвентарь, перемещаем в слот нагрудника (6), закрываем
                mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player));
                PlayerInventoryUtil.moveItem(pendingElytraSlot, 6, false);
                PlayerInventoryUtil.closeScreen(true);
                mc.setScreen(null);
                startElytraSwap = false;
                elytraSwapTick = 0;
                pendingElytraSlot = null;
            } else {
                elytraSwapTick++;
                // Сброс движения на 2 тика
                mc.options.jumpKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.backKey.setPressed(false);
            }
        });
    }

    @Override
    public void onDisable() {
        startElytraSwap = false;
        elytraSwapTick = 0;
        pendingElytraSlot = null;
        super.onDisable();
    }

    private void useFirework() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
        if (slot == null) return;

        // Уведомление о фейерверке
        if (space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.isEnabled()
                && space.visuals.client.modules.impl.render.SwapNotifications.INSTANCE.elytraHelper.isEnabled()) {
            Zenith.getInstance().getNotifyManager().addSwapNotification(slot.getStack());
        }

        Rotation angle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        fireworkCooldown = 8 + (int)(Math.random() * 5);

        ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(task);

        task.schedule(EventUpdate.class, ev -> {
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            return true;
        });
        task.schedule(EventUpdate.class, ev -> {
            PlayerIntersectionUtil.useItem(Hand.MAIN_HAND, angle);
            return true;
        });
        task.schedule(EventUpdate.class, ev -> {
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            return true;
        });
    }

    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return PlayerInventoryUtil.getSlot(List.of(
                    Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
                    Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE));
        else
            return PlayerInventoryUtil.getSlot(Items.ELYTRA);
    }
}
