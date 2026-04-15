package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.request.ScriptManager;
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

    // Кулдаун между свапами для легитимности (тики)
    private int cooldownTicks = 0;
    private static final int SWAP_COOLDOWN = 3;

    // Сохраняем копию предмета который был в оффхенде до тотема
    private ItemStack savedOffhand = ItemStack.EMPTY;
    // Флаг что тотем был взят нами
    private boolean totemTaken = false;

    @EventTarget
    public void onPlayerTick(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // Не мешаем свапу элитры
        if (ElytraHelper.INSTANCE.isSwapping()) return;

        // Не мешаем если ScriptManager занят
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        final ItemStack offhandStack = mc.player.getOffHandStack();
        final Item current = offhandStack.isEmpty() ? null : offhandStack.getItem();

        if (shouldUseTotem()) {
            // Если нужен тотем и в руке зачарованный — свапаем на обычный (если включено)
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
                        doSwap(plainTotem);
                        return;
                    }
                }
            }

            // Если тотема нет в руке — берём
            if (current != TOTEM_OF_UNDYING) {
                Slot slot = PlayerInventoryUtil.getSlot(TOTEM_OF_UNDYING);
                if (slot != null) {
                    // Сохраняем что было в оффхенде
                    savedOffhand = offhandStack.copy();
                    totemTaken = true;
                    doSwap(slot);
                }
            }
        } else {
            // Условие пропало — возвращаем предмет если мы его убирали
            if (totemTaken && current == TOTEM_OF_UNDYING) {
                totemTaken = false;
                if (!savedOffhand.isEmpty()) {
                    // Ищем слот с сохранённым предметом
                    Slot returnSlot = PlayerInventoryUtil.getSlot(
                            savedOffhand.getItem(),
                            Comparator.comparingInt(s -> s.id),
                            s -> s.id != 45 && s.id != 46
                    );
                    if (returnSlot != null) {
                        doSwap(returnSlot);
                    }
                    savedOffhand = ItemStack.EMPTY;
                }
            }
        }
    }

    private void doSwap(Slot slot) {
        // Легитимный свап через ScriptTask с задержкой
        ScriptManager.ScriptTask task = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(task);

        // Тик 1: свап
        task.schedule(EventUpdate.class, ev -> {
            if (mc.player == null) return true;
            PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, false);
            PlayerInventoryUtil.closeScreen(true);
            return true;
        });

        cooldownTicks = SWAP_COOLDOWN;
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
        cooldownTicks = 0;
        super.onDisable();
    }
}
