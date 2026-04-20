package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.client.modules.impl.misc.ElytraHelper;
import space.visuals.Zenith;

import java.util.Comparator;

import static net.minecraft.item.Items.TOTEM_OF_UNDYING;

@ModuleAnnotation(
        name = "AutoTotem",
        category = Category.COMBAT, description = "При условиях берет тотем в руку"
)
public final class AutoTotem extends Module {
    public static final AutoTotem INSTANCE = new AutoTotem();

    private AutoTotem() {}

    private final NumberSetting health = new NumberSetting("Здоровье", 5f, 0, 36, 0.1f);

    private final BooleanSetting fall = new BooleanSetting("Падение", true);
    private final NumberSetting fallDistance = new NumberSetting("При падении", 20f, 10, 50, 0.1f, fall::isEnabled);

    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять зачарованные", false);
    private final BooleanSetting returnTotem = new BooleanSetting("Возвращать тотем", true);

    // Состояние свапа (как в AutoSwap)
    private boolean swapping = false;
    private int swapTick = 0;
    private Slot pendingSlot = null;

    // Сохраняем копию предмета который был в оффхенде до тотема
    private ItemStack savedOffhand = ItemStack.EMPTY;
    // Флаг что тотем был взят нами
    private boolean totemTaken = false;

    @EventTarget
    public void onPlayerTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (ElytraHelper.INSTANCE.isSwapping()) return;

        // Обрабатываем текущий свап
        if (swapping && pendingSlot != null) {
            PlayerInventoryComponent.addTask(() -> {
                if (swapTick >= 2) {
                    PlayerInventoryUtil.swapHand(pendingSlot, Hand.OFF_HAND, false);
                    PlayerInventoryUtil.closeScreen(true);
                    swapping = false;
                    swapTick = 0;
                    pendingSlot = null;
                } else {
                    swapTick++;
                    mc.options.jumpKey.setPressed(false);
                    mc.options.forwardKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    mc.options.sprintKey.setPressed(false);
                }
            });
            return;
        }

        // Не запускаем новый свап пока идёт предыдущий
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        final ItemStack offhandStack = mc.player.getOffHandStack();
        final Item current = offhandStack.isEmpty() ? null : offhandStack.getItem();

        // Если тотем был взят нами, но оффхенд уже пустой —
        // значит обычный тотем сработал. Возвращаем сохранённый предмет (зачарованный тотем).
        if (totemTaken && current != TOTEM_OF_UNDYING && !swapping) {
            totemTaken = false;
            if (!savedOffhand.isEmpty() && returnTotem.isEnabled()) {
                Slot returnSlot = PlayerInventoryUtil.getSlot(
                        s -> !s.getStack().isEmpty()
                            && ItemStack.areItemsAndComponentsEqual(s.getStack(), savedOffhand)
                            && s.id != 45 && s.id != 46
                );
                if (returnSlot == null) {
                    returnSlot = PlayerInventoryUtil.getSlot(
                            savedOffhand.getItem(),
                            Comparator.comparingInt(s -> s.id),
                            s -> s.id != 45 && s.id != 46
                    );
                }
                if (returnSlot != null) doSwap(returnSlot);
            }
            savedOffhand = ItemStack.EMPTY;
        }

        if (shouldUseTotem()) {
            if (saveEnchanted.isEnabled() && current == TOTEM_OF_UNDYING) {
                boolean isEnchanted = offhandStack.get(DataComponentTypes.ENCHANTMENTS) != null
                        && !offhandStack.get(DataComponentTypes.ENCHANTMENTS).isEmpty();
                if (isEnchanted) {
                    Slot plainTotem = PlayerInventoryUtil.getSlot(
                            TOTEM_OF_UNDYING,
                            Comparator.comparing(s -> s.getStack().get(DataComponentTypes.ENCHANTMENTS) == null
                                    || s.getStack().get(DataComponentTypes.ENCHANTMENTS).isEmpty()),
                            s -> s.id != 46 && s.id != 45
                                    && (s.getStack().get(DataComponentTypes.ENCHANTMENTS) == null
                                    || s.getStack().get(DataComponentTypes.ENCHANTMENTS).isEmpty())
                    );
                    if (plainTotem != null) {
                        // Сохраняем зачарованный тотем для возврата
                        savedOffhand = offhandStack.copy();
                        totemTaken = true;
                        doSwap(plainTotem);
                        return;
                    }
                }
            }
            if (current != TOTEM_OF_UNDYING) {
                Slot slot = PlayerInventoryUtil.getSlot(TOTEM_OF_UNDYING);
                if (slot != null) {
                    savedOffhand = offhandStack.copy();
                    totemTaken = true;
                    doSwap(slot);
                }
            }
        } else {
            // Условие пропало — возвращаем зачарованный тотем
            if (totemTaken && returnTotem.isEnabled() && !swapping) {
                // Проверяем что в оффхенде сейчас НЕ тот предмет что мы сохранили
                boolean offhandChanged = !ItemStack.areItemsAndComponentsEqual(offhandStack, savedOffhand);
                if (offhandChanged && !savedOffhand.isEmpty()) {
                    totemTaken = false;
                    Slot returnSlot = PlayerInventoryUtil.getSlot(
                            s -> !s.getStack().isEmpty()
                                && ItemStack.areItemsAndComponentsEqual(s.getStack(), savedOffhand)
                                && s.id != 45 && s.id != 46
                    );
                    if (returnSlot == null) {
                        returnSlot = PlayerInventoryUtil.getSlot(
                                savedOffhand.getItem(),
                                Comparator.comparingInt(s -> s.id),
                                s -> s.id != 45 && s.id != 46
                        );
                    }
                    if (returnSlot != null) doSwap(returnSlot);
                    savedOffhand = ItemStack.EMPTY;
                }
            } else if (totemTaken && !returnTotem.isEnabled()) {
                totemTaken = false;
                savedOffhand = ItemStack.EMPTY;
            }
        }
    }

    private void doSwap(Slot slot) {
        if (swapping) return;
        pendingSlot = slot;
        swapping = true;
        swapTick = 0;
    }

    private boolean shouldUseTotem() {
        float healthValue = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (healthValue <= health.getCurrent()) return true;

        return fall.isEnabled() && mc.player.fallDistance >= fallDistance.getCurrent();
    }

    @Override
    public void onDisable() {
        totemTaken = false;
        savedOffhand = ItemStack.EMPTY;
        swapping = false;
        swapTick = 0;
        pendingSlot = null;
        super.onDisable();
    }
}
