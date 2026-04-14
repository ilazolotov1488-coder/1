package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(name = "Animation", category = Category.RENDER, description = "Анимации открытия инвентаря и TAB")
public final class AnimationModule extends Module {
    public static final AnimationModule INSTANCE = new AnimationModule();

    public final BooleanSetting animateInventory  = new BooleanSetting("Инвентарь", true);
    public final BooleanSetting animateTabList    = new BooleanSetting("TAB список", true);

    private AnimationModule() {}
}
