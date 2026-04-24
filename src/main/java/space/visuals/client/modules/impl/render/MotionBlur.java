package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.client.modules.impl.render.motionblur.MotionBlurShader;

@ModuleAnnotation(name = "MotionBlur", category = Category.RENDER, description = "Эффект размытия движения")
public final class MotionBlur extends Module {

    public static final MotionBlur INSTANCE = new MotionBlur();

    private final NumberSetting strength = new NumberSetting("Сила", -0.8f, -2.0f, 2.0f, 0.05f);
    private final ModeSetting quality   = new ModeSetting("Качество", "Низкое", "Среднее", "Высокое", "Ультра");
    private final BooleanSetting rrc    = new BooleanSetting("Масштаб по FPS", true);

    // Шейдер создаётся лениво при первом включении
    private MotionBlurShader shader;

    private MotionBlur() {}

    @Override
    public void onEnable() {
        super.onEnable();
        if (shader == null) shader = new MotionBlurShader();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public MotionBlurShader getShader() {
        return shader;
    }

    public float getStrength() {
        return strength.getCurrent();
    }

    public int getQuality() {
        return switch (quality.get()) {
            case "Низкое"  -> 0;
            case "Среднее" -> 1;
            case "Высокое" -> 2;
            case "Ультра"  -> 3;
            default        -> 1;
        };
    }

    public boolean isUseRRC() {
        return rrc.isEnabled();
    }
}
