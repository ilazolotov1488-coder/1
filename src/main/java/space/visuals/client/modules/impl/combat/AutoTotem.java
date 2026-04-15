package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.game.player.PlayerInventoryComponent;
import space.visuals.utility.game.player.PlayerInventoryUtil;
import space.visuals.client.modules.impl.misc.ElytraHelper;

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

    private int cooldownTicks = 0;
    private Item previousItem = null;

    @EventTarget
    public void onPlayerTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Не мешаем свапу элитры
        if (ElytraHelper.INSTANCE.isSwapping()) return;

        final Item current = mc.player.getOffHandStack().isEmpty()
                ? null : mc.player.getOffHandStack().getItem();

        if (shouldUseTotem()) {
            // Если нужен тотем и в руке зачарованный — свапаем на обычный (если включено)
            if (saveEnchanted.isEnabled() && current == TOTEM_OF_UNDYING) {
                boolean isEnchanted = mc.player.getOffHandStack().get(DataComponentTypes.ENCHANTMENTS) != null
                        && !mc.player.getOffHandStack().get(DataComponentTypes.ENCHANTMENTS).isEmpty();
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
                        swapToOffhand(plainTotem);
                        return;
                    }
                }
            }

            // Если тотема нет в руке — берём любой
            if (current != TOTEM_OF_UNDYING) {
                Slot slot = PlayerInventoryUtil.getSlot(TOTEM_OF_UNDYING);
                if (slot != null) {
                    swapToOffhand(slot);
                    previousItem = current;
                }
            }
        } else if (current == TOTEM_OF_UNDYING && previousItem != null) {
            Slot slot = PlayerInventoryUtil.getSlot(previousItem);
            if (slot != null) {
                swapToOffhand(slot);
            }
            previousItem = null;
        }
    }

    private void swapToOffhand(Slot slot) {
        PlayerInventoryComponent.addTask(() -> {
            PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
        });
        cooldownTicks = 0;
    }

    private boolean shouldUseTotem() {
        float healthValue = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (healthValue <= health.getCurrent()) return true;

        return fall.isEnabled() && mc.player.fallDistance >= fallDistance.getCurrent();
    }
}
