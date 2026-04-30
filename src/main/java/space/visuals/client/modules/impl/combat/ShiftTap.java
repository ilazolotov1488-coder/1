package space.visuals.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.hit.EntityHitResult;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "ShiftTap",
        category = Category.COMBAT,
        description = "Автоматически зажимает Shift при атаке"
)
public final class ShiftTap extends Module {
    public static final ShiftTap INSTANCE = new ShiftTap();

    private final NumberSetting intervalTicks = new NumberSetting("Интервал (тики)", 3f, 1f, 10f, 1f);

    private boolean wasAttackPressed = false;
    private int sneakTicksRemaining = 0;
    private boolean shouldReleaseNext = false;

    private ShiftTap() {}

    @Override
    public void onDisable() {
        if (mc != null && mc.options != null && mc.player != null) {
            if (!mc.player.isUsingItem()) {
                mc.options.sneakKey.setPressed(false);
            }
        }
        sneakTicksRemaining = 0;
        wasAttackPressed = false;
        shouldReleaseNext = false;
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.player == null) return;

        boolean isUsingItem = mc.player.isUsingItem();
        if (isUsingItem) {
            sneakTicksRemaining = 0;
            shouldReleaseNext = false;
            wasAttackPressed = mc.options.attackKey.isPressed();
            return;
        }

        boolean attackPressed = mc.options.attackKey.isPressed();

        if (shouldReleaseNext) {
            mc.options.sneakKey.setPressed(false);
            shouldReleaseNext = false;
        }

        if (attackPressed && !wasAttackPressed) {
            boolean isEntityTarget = mc.crosshairTarget instanceof EntityHitResult;
            if (isEntityTarget) {
                int hold = Math.max(1, Math.round(intervalTicks.getCurrent()));
                mc.options.sneakKey.setPressed(true);
                if (hold == 1) {
                    shouldReleaseNext = true;
                } else {
                    sneakTicksRemaining = hold - 1;
                }
            }
        }

        wasAttackPressed = attackPressed;

        if (sneakTicksRemaining > 0) {
            sneakTicksRemaining--;
            if (sneakTicksRemaining == 0) {
                mc.options.sneakKey.setPressed(false);
            }
        }
    }
}
