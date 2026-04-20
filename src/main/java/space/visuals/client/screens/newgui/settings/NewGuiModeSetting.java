package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class NewGuiModeSetting extends NewGuiSettingEntry {

    private final ModeSetting setting;
    private final List<Animation> optionAnims = new ArrayList<>();

    public NewGuiModeSetting(ModeSetting setting) {
        this.setting = setting;
        for (int i = 0; i < setting.getValues().size(); i++) {
            boolean selected = setting.getValues().get(i).isSelected();
            optionAnims.add(new Animation(150, selected ? 1f : 0f, Easing.LINEAR));
        }
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() {
        // Название + строки с опциями
        return 10f + getOptionsHeight();
    }

    private float getOptionsHeight() {
        float maxW = 90f;
        float hPad = 5f;
        float optH = 9f;
        float curX = 0;
        int lines = 1;
        for (ModeSetting.Value val : setting.getValues()) {
            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f) + hPad;
            if (curX + tw > maxW) {
                curX = 0;
                lines++;
            }
            curX += tw;
        }
        return lines * (optH + 1f);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();

        // ModeObject.java: name at x+11, y+2, Font[13]
        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(240, 240, 240, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + 11f, y + 2f, 0);

        float startX = x + 4f;
        float startY = y + 11f;
        float maxW = width - 8f;
        float hPad = 5f;
        float optH = 9f;
        float curX = startX;
        float curY = startY;

        List<ModeSetting.Value> values = setting.getValues();
        for (int i = 0; i < values.size(); i++) {
            ModeSetting.Value val = values.get(i);
            boolean selected = val.isSelected();
            if (i < optionAnims.size()) {
                optionAnims.get(i).update(selected ? 1f : 0f);
            }
            float anim = i < optionAnims.size() ? optionAnims.get(i).getValue() : (selected ? 1f : 0f);

            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f);
            float boxW = tw + hPad;

            if (curX - startX + boxW > maxW) {
                curX = startX;
                curY += optH + 1f;
            }

            // Фон опции
            int fillAlpha = (int)(anim * 120 * alpha);
            if (fillAlpha > 0) {
                DrawUtil.drawRoundedRect(matrices, curX, curY, boxW, optH - 1f,
                        BorderRadius.all(2f), new ColorRGBA(80, 140, 255, fillAlpha));
            }

            // Рамка
            int outlineAlpha = (int)((60 + anim * 120) * alpha);
            DrawUtil.drawRoundedBorder(matrices, curX, curY, boxW, optH - 1f,
                    0.7f, BorderRadius.all(2f), new ColorRGBA(80, 140, 255, outlineAlpha));

            // Текст
            ColorRGBA textColor = ColorRGBA.lerp(
                    new ColorRGBA(130, 132, 145, 255),
                    new ColorRGBA(220, 225, 255, 255),
                    anim
            ).withAlpha((int)(255 * alpha));

            MsdfRenderer.renderText(Fonts.REGULAR, val.getName(), 7f, textColor.getRGB(),
                    matrices.peek().getPositionMatrix(), curX + hPad / 2f, curY + 1.5f, 0);

            curX += boxW + 2f;
        }
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button != MouseButton.LEFT) return;

        float startX = x + 4f;
        float startY = y + 11f;
        float maxW = width - 8f;
        float hPad = 5f;
        float optH = 9f;
        float curX = startX;
        float curY = startY;

        List<ModeSetting.Value> values = setting.getValues();
        for (int i = 0; i < values.size(); i++) {
            ModeSetting.Value val = values.get(i);
            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f);
            float boxW = tw + hPad;

            if (curX - startX + boxW > maxW) {
                curX = startX;
                curY += optH + 1f;
            }

            if (isHovered(mouseX, mouseY, curX, curY, boxW, optH)) {
                val.select();
                return;
            }
            curX += boxW + 2f;
        }
    }
}
