package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class NewGuiSliderSetting extends NewGuiSettingEntry {

    private final NumberSetting setting;
    private boolean sliding = false;

    private final Animation fillAnim = new Animation(100, 0f, Easing.LINEAR);

    public NewGuiSliderSetting(NumberSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() { return 20f; }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();

        if (sliding) {
            float sliderX = x + 4f;
            float sliderW = width - 8f;
            float raw = ((mouseX - sliderX) / sliderW) * (setting.getMax() - setting.getMin()) + setting.getMin();
            float clamped = net.minecraft.util.math.MathHelper.clamp(raw, setting.getMin(), setting.getMax());
            // Округляем до шага
            float step = setting.getIncrement();
            if (step > 0) {
                clamped = Math.round(clamped / step) * step;
            }
            setting.setCurrent(clamped);
        }

        float min = setting.getMin();
        float max = setting.getMax();
        float cur = setting.getCurrent();
        float progress = (cur - min) / (max - min);

        float sliderX = x + 4f;
        float sliderY = y + 13f;
        float sliderW = width - 8f;
        float sliderH = 3f;

        // Название + значение
        String valStr = (cur == (int) cur) ? String.valueOf((int) cur) : String.format("%.1f", cur);
        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(200, 202, 215, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + 4f, y + 2f, 0);

        float valW = Fonts.REGULAR.getWidth(valStr, 7f);
        MsdfRenderer.renderText(Fonts.REGULAR, valStr, 7f,
                new ColorRGBA(140, 180, 255, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + width - valW - 4f, y + 2f, 0);

        // Трек
        DrawUtil.drawRoundedRect(matrices, sliderX, sliderY, sliderW, sliderH,
                BorderRadius.all(1.5f), new ColorRGBA(45, 47, 60, (int)(200 * alpha)));

        // Заполнение
        fillAnim.update(progress * sliderW);
        float fillW = Math.max(0, fillAnim.getValue());
        if (fillW > 0) {
            DrawUtil.drawRoundedRect(matrices, sliderX, sliderY, fillW, sliderH,
                    BorderRadius.all(1.5f), new ColorRGBA(100, 160, 255, (int)(220 * alpha)));
        }

        // Ползунок
        float thumbX = sliderX + fillW - 3f;
        DrawUtil.drawRoundedRect(matrices, thumbX, sliderY - 1.5f, 6f, 6f,
                BorderRadius.all(3f), new ColorRGBA(220, 225, 240, (int)(255 * alpha)));
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT) {
            float sliderX = x + 4f;
            float sliderY = y + 11f;
            float sliderW = width - 8f;
            if (isHovered(mouseX, mouseY, sliderX, sliderY, sliderW, 8f)) {
                sliding = true;
            }
        }
    }

    @Override
    public void onMouseReleased(float mouseX, float mouseY, MouseButton button) {
        sliding = false;
    }
}
