package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

/**
 * Анимации интерфейса.
 * Анимация инвентаря и списка игроков управляется через миксины/хуки в других местах.
 * Здесь хранятся настройки, которые читают другие системы.
 */
@ModuleAnnotation(name = "Animation", category = Category.RENDER, description = "Анимации открытия инвентаря и списка игроков")
public final class AnimationModule extends Module {
    public static final AnimationModule INSTANCE = new AnimationModule();

    public final BooleanSetting animatePlayerList  = new BooleanSetting("Список игроков", false);
    public final BooleanSetting animateInventory   = new BooleanSetting("Инвентарь", false);
    public final BooleanSetting animateZoom        = new BooleanSetting("Приближение камеры", false);
    public final BooleanSetting animatePerspective = new BooleanSetting("Изменение перспективы", false);

    public final ModeSetting chunkAnimMode = new ModeSetting("Анимация чанков", "Выкл", "Выкл", "Quart", "Circ", "Sine", "Cubic");
    public final NumberSetting chunkSpeed  = new NumberSetting("Скорость чанков", 6f, 2f, 10f, 1f,
            () -> !chunkAnimMode.is("Выкл"));

    private AnimationModule() {}
}
