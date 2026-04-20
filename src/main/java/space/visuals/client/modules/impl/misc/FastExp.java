package space.visuals.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "FastExp", category = Category.MISC, description = "Быстро кидает бутылочки опыта.")
public final class FastExp extends Module {
    public static final FastExp INSTANCE = new FastExp();

    private final BooleanSetting expBottles = new BooleanSetting("Бутылочка опыта", true);
    private final NumberSetting  useDelay   = new NumberSetting("Задержка", 5f, 0f, 30f, 0.1f, expBottles::isEnabled);

    private int cooldownTimer = 0;

    private FastExp() {}

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) { cooldownTimer = 0; return; }

        // Не кидаем во время PvP
        if (!expBottles.isEnabled() || Zenith.getInstance().getServerHandler().isPvp()) {
            cooldownTimer = 0;
            return;
        }

        if (!mc.options.useKey.isPressed()) { cooldownTimer = 0; return; }

        if (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
            fastUseItem(Hand.MAIN_HAND);
            return;
        }
        if (mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
            fastUseItem(Hand.OFF_HAND);
        }
    }

    private void fastUseItem(Hand hand) {
        if (mc.interactionManager == null || mc.player == null) return;
        if (cooldownTimer >= (int) useDelay.getCurrent()) {
            mc.interactionManager.interactItem(mc.player, hand);
            cooldownTimer = 0;
        } else {
            cooldownTimer++;
        }
    }

    @Override
    public void onDisable() {
        cooldownTimer = 0;
        super.onDisable();
    }
}
