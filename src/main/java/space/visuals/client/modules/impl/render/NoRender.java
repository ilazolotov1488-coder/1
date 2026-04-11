package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.MultiBooleanSetting;

import java.util.List;

@ModuleAnnotation(name = "NoRender", category = Category.RENDER, description = "Убирает лишние элементы с экрана")
public final class NoRender extends Module {
    public static final NoRender INSTANCE = new NoRender();

    private final MultiBooleanSetting settings = MultiBooleanSetting.create("Убрать", List.of(
            "Огонь",          // 0
            "Плохие эффекты", // 1
            "Тряска камеры",  // 2
            "Портал",         // 3
            "Тошнота",        // 4
            "Изменение FOV",  // 5
            "Погода",         // 6
            "Слепота",        // 7
            "Темнота",        // 8
            "Тыква",          // 9
            "Частицы блоков"  // 10
    ));

    private double oldFovEffectScale = -1;

    private NoRender() {
    }

    @Override
    public void onEnable() {
        if (mc.options != null)
            oldFovEffectScale = (Double) mc.options.getFovEffectScale().getValue();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (oldFovEffectScale != -1 && mc.options != null)
            mc.options.getFovEffectScale().setValue(oldFovEffectScale);
        super.onDisable();
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (mc.options == null) return;
        if (isRemoveFov()) {
            mc.options.getFovEffectScale().setValue(0.0);
        }
    }

    public boolean isRemoveFire() {
        return isEnabled() && settings.isEnable(0);
    }

    public boolean isRemoveBadEffect() {
        return isEnabled() && settings.isEnable(1);
    }

    public boolean isRemoveHurtCam() {
        return isEnabled() && settings.isEnable(2);
    }

    public boolean isRemovePortal() {
        return isEnabled() && settings.isEnable(3);
    }

    public boolean isRemoveNausea() {
        return isEnabled() && settings.isEnable(4);
    }

    public boolean isRemoveFov() {
        return isEnabled() && settings.isEnable(5);
    }

    public boolean isRemoveWeather() {
        return isEnabled() && settings.isEnable(6);
    }

    public boolean isRemoveBlindness() {
        return isEnabled() && settings.isEnable(7);
    }

    public boolean isRemoveDarkness() {
        return isEnabled() && settings.isEnable(8);
    }

    public boolean isRemovePumpkin() {
        return isEnabled() && settings.isEnable(9);
    }

    public boolean isRemoveBreakParticles() {
        return isEnabled() && settings.isEnable(10);
    }
}
