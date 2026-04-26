package space.visuals.client.modules.impl.render;

import net.minecraft.util.math.MathHelper;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(name = "ShaderHands", category = Category.RENDER, description = "Шейдер рук: блюр и свечение.")
public final class ShaderHands extends Module {
    public static final ShaderHands INSTANCE = new ShaderHands();

    private final BooleanSetting glow           = new BooleanSetting("Глов", true);
    private final NumberSetting  glowSize        = new NumberSetting("Сила глова",     4f, 1f, 8f, 1f,    glow::isEnabled);
    private final NumberSetting  glowBrightness  = new NumberSetting("Яркость глова",  1f, 0f, 4f, 0.05f, glow::isEnabled);
    private final BooleanSetting blur            = new BooleanSetting("Блюр", true);
    private final NumberSetting  blurBrightness  = new NumberSetting("Яркость блюра",  1f, 0f, 2f, 0.05f, blur::isEnabled);
    private final NumberSetting  blurSize        = new NumberSetting("Сила блюра",     4f, 1f, 8f, 1f,    blur::isEnabled);
    private final ColorSetting   handColor       = new ColorSetting("Цвет рук", new ColorRGBA(255, 255, 255));
    private final NumberSetting  handAlpha       = new NumberSetting("Прозрачность рук", 0.65f, 0.2f, 1f, 0.05f);
    private final NumberSetting  handBrightness  = new NumberSetting("Яркость рук",    1f, 0.5f, 2.5f, 0.05f);


    private ShaderHands() {}

    public boolean shouldShaderHands() { return isEnabled(); }

    public float getHandAlpha() {
        if (!isEnabled()) return 1f;
        float alpha = handAlpha.getCurrent();
        if (blur.isEnabled()) {
            alpha -= (blurSize.getCurrent() - 1f) * 0.03f;
            alpha -= blurBrightness.getCurrent() * 0.05f;
        }
        return MathHelper.clamp(alpha, 0.2f, 1f);
    }

    public float getHandCombinedAlpha() {
        if (!isEnabled()) return 1f;
        float colorAlpha = (float) handColor.getColor().getAlpha() / 255f;
        return MathHelper.clamp(getHandAlpha() * colorAlpha, 0f, 1f);
    }

    public ColorRGBA getHandColor() { return handColor.getColor(); }

    public float getHandBrightness() {
        if (!isEnabled()) return 1f;
        float value = handBrightness.getCurrent();
        if (glow.isEnabled()) {
            value += glowBrightness.getCurrent() * 0.1f;
            value += (glowSize.getCurrent() - 1f) * 0.05f;
        }
        return MathHelper.clamp(value, 0.5f, 2.8f);
    }
}
