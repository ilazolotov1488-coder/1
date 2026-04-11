package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import space.visuals.Zenith;
import space.visuals.base.events.impl.input.EventKey;
import space.visuals.base.events.impl.player.EventRotate;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
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

    // Тик-машина состояний (как в KeyPearl)
    private int step = 0;
    private int prevSlot = -1;
    private int itemSlot = -1;
    private boolean itemInHotbar = false;
    private Rotation throwAngle = null;

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
                    if (step == 0) tryUse(bind.item);
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

    private void tryUse(Item item) {
        if (mc.player == null || step != 0) return;
        int slot = findItem(item);
        if (slot == -1) return;
        itemSlot = slot;
        prevSlot = mc.player.getInventory().selectedSlot;
        itemInHotbar = itemSlot >= 36 && itemSlot <= 44;
        throwAngle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        step = 1;
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (step == 0) return;

        if (itemInHotbar) {
            tickHotbar();
        } else {
            tickInventory();
        }
    }

    private void tickHotbar() {
        switch (step) {
            case 1 -> {
                // выбираем слот как нажатие цифры
                mc.player.getInventory().selectedSlot = itemSlot - 36;
                step = 2;
            }
            case 2 -> {
                // используем предмет
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                step = 3;
            }
            case 3 -> {
                // возвращаем слот
                mc.player.getInventory().selectedSlot = prevSlot;
                reset();
            }
        }
    }

    private void tickInventory() {
        switch (step) {
            case 1 -> {
                if (!(mc.currentScreen instanceof InventoryScreen))
                    mc.setScreen(new InventoryScreen(mc.player));
                step = 2;
            }
            case 2 -> {
                swapSlot(itemSlot, prevSlot);
                step = 3;
            }
            case 3 -> {
                if (mc.currentScreen instanceof InventoryScreen)
                    mc.currentScreen.close();
                step = 4;
            }
            case 4 -> {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                step = 5;
            }
            case 5 -> {
                mc.setScreen(new InventoryScreen(mc.player));
                step = 6;
            }
            case 6 -> {
                swapSlot(itemSlot, prevSlot);
                step = 7;
            }
            case 7 -> {
                if (mc.currentScreen instanceof InventoryScreen)
                    mc.currentScreen.close();
                reset();
            }
        }
    }

    private void swapSlot(int slotId, int hotbarSlot) {
        if (mc.interactionManager == null || mc.player == null) return;
        if (hotbarSlot >= 0 && hotbarSlot <= 8) {
            int syncId = mc.player.currentScreenHandler.syncId;
            mc.interactionManager.clickSlot(syncId, slotId, hotbarSlot, SlotActionType.SWAP, mc.player);
        }
    }

    private int findItem(Item item) {
        if (mc.player == null) return -1;
        List<Slot> slots = mc.player.currentScreenHandler.slots;
        // сначала хотбар
        for (Slot slot : slots) {
            if (slot.id >= 36 && slot.id <= 44 && slot.getStack().isOf(item)) return slot.id;
        }
        // потом инвентарь
        for (Slot slot : slots) {
            if (slot.id >= 9 && slot.id <= 35 && slot.getStack().isOf(item)) return slot.id;
        }
        return -1;
    }

    private void reset() {
        step = 0;
        prevSlot = -1;
        itemSlot = -1;
        itemInHotbar = false;
        throwAngle = null;
    }

    @EventTarget
    public void onWorldRender(EventRender3D e) {
        Predictions.INSTANCE.drawPredictionInHand(e.getMatrix(),
                keyBindings.stream().filter(keyBind -> keyBind.draw.isValue())
                        .map(keyBind -> keyBind.item.getDefaultStack()).toList());
    }

    private Slot saveSlot = null;

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
