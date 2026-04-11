package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.request.ScriptManager;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.impl.movement.ElytraRecast;
import space.visuals.utility.game.player.MovingUtil;
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

    private int elytraDelay = 0;       // задержка перед активацией полёта
    private int fireworkCooldown = 0;  // кулдаун между фейерверками
    private boolean swapping = false;  // идёт ли свап элитры

    public boolean isSwapping() { return swapping; }

    private ElytraHelper() {
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.isKeyDown(elytraSetting.getKeyCode()) && !swapping) {
            Slot slot = chestPlate();
            if (slot != null) {
                swapping = true;
                ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
                Zenith.getInstance().getScriptManager().addTask(task);

                // тик 1: открываем инвентарь (визуально скрыто, но сервер получает пакет)
                task.schedule(EventUpdate.class, ev -> {
                    mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player));
                    return true;
                });
                // тик 2: свап
                task.schedule(EventUpdate.class, ev -> {
                    PlayerInventoryUtil.moveItem(slot, 6, false);
                    return true;
                });
                // тик 3: закрываем инвентарь
                task.schedule(EventUpdate.class, ev -> {
                    PlayerInventoryUtil.closeScreen(true);
                    mc.setScreen(null);
                    elytraDelay = 0;
                    swapping = false;
                    return true;
                });
            }
        } else if (e.isKeyDown(fireworkSetting.getKeyCode()) && mc.player.isGliding() && fireworkCooldown <= 0) {
            useFirework();
        }
    }

    private void useFirework() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.FIREWORK_ROCKET);
        if (slot == null) return;

        Rotation angle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        // случайный кулдаун 8-12 тиков (~400-600мс)
        fireworkCooldown = 8 + (int)(Math.random() * 5);

        ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(task);

        // тик 1: свап фейерверка в руку
        task.schedule(EventUpdate.class, ev -> {
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            return true;
        });
        // тик 2: использование
        task.schedule(EventUpdate.class, ev -> {
            PlayerIntersectionUtil.useItem(Hand.MAIN_HAND, angle);
            return true;
        });
        // тик 3: свап обратно
        task.schedule(EventUpdate.class, ev -> {
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            return true;
        });
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (fireworkCooldown > 0) fireworkCooldown--;

        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)
                && !mc.player.isTouchingWater()) {
            if (!ElytraRecast.INSTANCE.isEnabled()
                    || (mc.player.isUsingItem() || !MovingUtil.hasPlayerMovement())) {
                if (!mc.player.isOnGround() && !mc.player.isGliding()) {
                    elytraDelay++;
                    if (elytraDelay == 4) {
                        PlayerIntersectionUtil.startFallFlying();
                    }
                } else {
                    elytraDelay = 0;
                }
            }
        }
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
