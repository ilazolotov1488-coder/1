package space.visuals.client.modules.impl.player;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(name = "FastBreak", category = Category.PLAYER, description = "Ускоряет добычу блоков")
public final class FastBreak extends Module {
    public static final FastBreak INSTANCE = new FastBreak();
    
    private FastBreak() {
    }

    public final BooleanSetting speedMine = new BooleanSetting("Speed Mine", false);

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (speedMine.isEnabled()) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20, 1, false, false));
        } else {
            mc.player.removeStatusEffect(StatusEffects.HASTE);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.HASTE);
        }
    }
}
