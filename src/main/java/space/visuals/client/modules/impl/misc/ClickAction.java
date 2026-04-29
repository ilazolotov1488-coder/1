package space.visuals.client.modules.impl.misc;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventRotate;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.request.ScriptManager;
import space.visuals.base.rotation.RotationTarget;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.client.modules.impl.render.Predictions;
import space.visuals.utility.game.player.PlayerIntersectionUtil;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.utility.game.player.SimulatedPlayer;
import space.visuals.utility.game.player.rotation.Rotation;
import space.visuals.utility.game.player.rotation.RotationUtil;
import space.visuals.utility.math.Timer;
import space.visuals.utility.other.BooleanSettable;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "ClickAction", description = "Делает что то по бинду", category = Category.MISC)
public final class ClickAction extends Module {

    private final KeySetting friendBind = new KeySetting("Добавить друга");
    private final KeySetting expBind = new KeySetting("Пузырек опыта");

    private final List<KeyBind> keyBindings = new ArrayList<>();
    private final Timer timer = new Timer();
    public static final ClickAction INSTANCE = new ClickAction();

    private ClickAction() {
        keyBindings.add(new KeyBind(Items.ENDER_PEARL, new KeySetting("Эндер перл"), new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.WIND_CHARGE, new KeySetting("Заряд ветра"), new BooleanSettable()));
    }

    @Override
    public List<Setting> getSettings() {
        ArrayList<Setting> settings = new ArrayList<>();
        settings.add(expBind);
        settings.add(friendBind);
        settings.addAll(keyBindings.stream().map(KeyBind::setting).toList());
        return settings;
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onKey(EventKey e) {
        if (e.isKeyDown(friendBind.getKeyCode())
                && mc.crosshairTarget instanceof EntityHitResult result
                && result.getEntity() instanceof PlayerEntity player) {
            if (Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName()))
                Zenith.getInstance().getFriendManager().removeFriend(player.getGameProfile().getName());
            else
                Zenith.getInstance().getFriendManager().add(player.getGameProfile().getName());
        }

        keyBindings.stream()
                .filter(bind -> e.isKeyDown(bind.setting.getKeyCode()) && PlayerInventoryUtil.getSlot(bind.item) != null)
                .forEach(bind -> bind.draw.setValue(true));

        keyBindings.stream()
                .filter(bind -> e.isKeyReleased(bind.setting.getKeyCode()))
                .forEach(bind -> {
                    if (mc.currentScreen == null && Zenith.getInstance().getScriptManager().isFinished())
                        swapAndUseWithReset(bind.item);
                    bind.draw.setValue(false);
                });

        if (e.isKeyDown(expBind.getKeyCode())) {
            Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);
            if (slot == null) {
                Zenith.getInstance().getNotifyManager().addNotification("M",
                        Text.of(Items.EXPERIENCE_BOTTLE.getName().copy()
                                .setStyle(Style.EMPTY.withColor(zenith.getThemeManager().getCurrentTheme().getColor().getRGB()))
                                .append(Text.of("не найден").copy()
                                        .setStyle(Style.EMPTY.withColor(zenith.getThemeManager().getCurrentTheme().getWhite().getRGB())))));
            }
        }
    }

    // Легитный бросок снаряда — логика как в ServerHelper (пакетный своп без открытия инвентаря)
    private void swapAndUseWithReset(Item item) {
        if (mc.player == null) return;
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0f);
        if (cooldown > 0) return;

        // Приоритет хотбара: ищем сначала в хотбаре (слоты 36-44)
        Slot slot = PlayerInventoryUtil.getSlot(item, s -> s.id >= 36 && s.id <= 44);
        if (slot == null) slot = PlayerInventoryUtil.getSlot(item);
        if (slot == null) return;

        final int prevSlot = mc.player.getInventory().selectedSlot;
        final boolean inHotbar = slot.id >= 36 && slot.id <= 44;
        final Slot finalSlot = slot;

        space.visuals.utility.game.player.rotation.Rotation angle = Zenith.getInstance().getRotationManager().getCurrentRotation();

        ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(task);

        // Тик 1: сброс спринта и движения
        task.schedule(EventUpdate.class, ev -> {
            mc.options.sprintKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return true;
        });
        // Тик 2: сброс спринта и движения
        task.schedule(EventUpdate.class, ev -> {
            mc.options.sprintKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return true;
        });

        if (inHotbar) {
            // Переключить слот
            task.schedule(EventUpdate.class, ev -> {
                mc.player.getInventory().selectedSlot = finalSlot.id - 36;
                return true;
            });
            task.schedule(EventUpdate.class, ev -> true); // +1 тик
            // Использовать
            task.schedule(EventUpdate.class, ev -> {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                return true;
            });
            // Вернуть слот
            task.schedule(EventUpdate.class, ev -> {
                mc.player.getInventory().selectedSlot = prevSlot;
                return true;
            });
        } else {
            // Своп пакетом в хотбар (без открытия инвентаря)
            task.schedule(EventUpdate.class, ev -> {
                mc.options.sprintKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    finalSlot.id, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                PlayerInventoryUtil.closeScreen(true);
                return true;
            });
            // +1 тик ожидания, движение заблокировано
            task.schedule(EventUpdate.class, ev -> {
                mc.options.sprintKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                return true;
            });
            // Использовать
            task.schedule(EventUpdate.class, ev -> {
                mc.options.sprintKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                return true;
            });
            // +1 тик после броска, движение заблокировано
            task.schedule(EventUpdate.class, ev -> {
                mc.options.sprintKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                return true;
            });
            // Вернуть предмет пакетом — движение всё ещё заблокировано
            task.schedule(EventUpdate.class, ev -> {
                mc.options.sprintKey.setPressed(false);
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    finalSlot.id, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
                PlayerInventoryUtil.closeScreen(true);
                return true;
            });
        }

        // 4 тика после возврата — предмет 100% вернулся, только потом восстанавливаем движение
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false); mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false); mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false); mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { mc.options.sprintKey.setPressed(false); mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false); mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false); return true; });
        task.schedule(EventUpdate.class, ev -> { restoreMoveKeys(); return true; });
    }

    private void restoreMoveKeys() {
        long win = mc.getWindow().getHandle();
        mc.options.sprintKey.setPressed(GLFW.glfwGetKey(win,  GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS);
        mc.options.forwardKey.setPressed(GLFW.glfwGetKey(win, GLFW.GLFW_KEY_W)     == GLFW.GLFW_PRESS);
        mc.options.leftKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_A)     == GLFW.GLFW_PRESS);
        mc.options.rightKey.setPressed(GLFW.glfwGetKey(win,   GLFW.GLFW_KEY_D)     == GLFW.GLFW_PRESS);
        mc.options.backKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_S)     == GLFW.GLFW_PRESS);
        mc.options.jumpKey.setPressed(GLFW.glfwGetKey(win,    GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
    }

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onWorldRender(EventRender3D e) {
        Predictions.INSTANCE.drawPredictionInHand(e.getMatrix(),
                keyBindings.stream().filter(keyBind -> keyBind.draw.isValue())
                        .map(keyBind -> keyBind.item.getDefaultStack()).toList());
    }

    private Slot saveSlot = null;

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onExpTick(EventRotate e) {
        boolean isMainHandItem = mc.player.getMainHandStack().getItem().equals(Items.EXPERIENCE_BOTTLE);
        if (PlayerIntersectionUtil.isKey(expBind)) {
            Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);
            SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(3);
            Rotation angle = new Rotation(mc.player.getYaw(), RotationUtil.calculateAngle(simulatedPlayer.boundingBox.getCenter()).getPitch());
            rotationManager.setRotation(new RotationTarget(angle, () ->
                    aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getInstantSetup()), 1, this);
            if (!isMainHandItem) {
                PlayerInventoryComponent.addTask(() -> {
                    if (saveSlot == null) saveSlot = slot;
                    PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
                    PlayerInventoryUtil.closeScreen(true);
                });
            } else if (timer.finished(70) && rotationManager.getCurrentRotation().rotationDeltaTo(angle).isInRange(180, 10)) {
                PlayerIntersectionUtil.useItem(Hand.MAIN_HAND, angle);
                timer.reset();
            }
        } else {
            if (saveSlot != null) {
                PlayerInventoryComponent.addTask(() -> {
                    if (!PlayerIntersectionUtil.isKey(expBind)) {
                        PlayerInventoryUtil.swapHand(saveSlot, Hand.MAIN_HAND, false);
                        PlayerInventoryUtil.closeScreen(true);
                        saveSlot = null;
                    }
                });
            }
        }
    }

    public record KeyBind(Item item, KeySetting setting, BooleanSettable draw) {
    }
}
