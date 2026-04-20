package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
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

    public NewGuiSliderSetting(NumberSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() { return 17f; }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();

        if (sliding) {
            float sliderX = x + 14f;
            float sliderW = width - 38f + 10f;
            float raw = ((mouseX - sliderX) / sliderW) * (setting.getMax() - setting.getMin()) + setting.getMin();
            float clamped = net.minecraft.util.math.MathHelper.clamp(raw, setting.getMin(), setting.getMax());
            float step = setting.getIncrement();
            if (step > 0) clamped = Math.round(clamped / step) * step;
            setting.setCurrent(clamped);
        }

        float min = setting.getMin();
        float max = setting.getMax();
        float cur = setting.getCurrent();

        // SliderObject.java: name at x+10, y+3, Font[13]
        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(255, 255, 255, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + 10f, y + 3f, 0);

        // SliderObject.java: sliderY = y + height/2 + 6, track: x+6+8, sliderY, width-38+10, 3
        float sliderH = 3f;
        float sliderY = y + getHeight() / 2f + 6f;
        float sliderX = x + 6f + 8f;
        float sliderW = width - 38f + 10f;

        float progress = (cur - min) / (max - min);
        float fillW = Math.max(0, progress * sliderW);

        // Track: rgba(100,100,100,100)
        DrawUtil.drawRoundedRect(matrices, sliderX, sliderY, sliderW, sliderH,
                BorderRadius.all(0.8f), new ColorRGBA(100, 100, 100, (int)(100 * alpha)));

        // Fill: theme color (accent blue)
        if (fillW > 0) {
            DrawUtil.drawRoundedRect(matrices, sliderX, sliderY, fillW, sliderH,
                    BorderRadius.all(0.8f), new ColorRGBA(100, 160, 255, (int)(220 * alpha)));
        }

        // Thumb: 6.5x6.5, radius 3.2, white
        float thumbX = sliderX + fillW - 3f;
        DrawUtil.drawRoundedRect(matrices, thumbX, sliderY - 1.4f, 6.5f, 6.5f,
                BorderRadius.all(3.2f), new ColorRGBA(255, 255, 255, (int)(255 * alpha)));

        // Value text: centered above thumb
        String valStr = (cur == (int)cur) ? String.valueOf((int)cur) : String.format("%.1f", cur);
        float textX = sliderX + sliderW + 4f;
        float textY = sliderY - 7f;
        MsdfRenderer.renderText(Fonts.REGULAR, valStr, 7f,
                new ColorRGBA(255, 255, 255, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), textX, textY, 0);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT) {
            float sliderX = x + 6f + 8f;
            float sliderY = y + getHeight() / 2f + 6f;
            float sliderW = width - 38f + 10f;
            if (isHovered(mouseX, mouseY, sliderX, sliderY - 3f, sliderW, 9f)) {
                sliding = true;
            }
        }
    }

    @Override
    public void onMouseReleased(float mouseX, float mouseY, MouseButton button) {
        sliding = false;
    }
}
