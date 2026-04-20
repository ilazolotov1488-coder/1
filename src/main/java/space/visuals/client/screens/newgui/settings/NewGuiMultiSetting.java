package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.MultiBooleanSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class NewGuiMultiSetting extends NewGuiSettingEntry {

    private final MultiBooleanSetting setting;
    private final List<Animation> optionAnims = new ArrayList<>();

    public NewGuiMultiSetting(MultiBooleanSetting setting) {
        this.setting = setting;
        for (int i = 0; i < setting.getBooleanSettings().size(); i++) {
            boolean enabled = setting.getBooleanSettings().get(i).isEnabled();
            optionAnims.add(new Animation(150, enabled ? 1f : 0f, Easing.LINEAR));
        }
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() {
        return 10f + getOptionsHeight();
    }

    private float getOptionsHeight() {
        float maxW = 90f;
        float hPad = 5f;
        float optH = 9f;
        float curX = 0;
        int lines = 1;
        for (var val : setting.getBooleanSettings()) {
            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f) + hPad;
            if (curX + tw > maxW) { curX = 0; lines++; }
            curX += tw;
        }
        return lines * (optH + 1f);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();

        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(200, 202, 215, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + 4f, y + 1f, 0);

        float startX = x + 4f;
        float startY = y + 11f;
        float maxW = width - 8f;
        float hPad = 5f;
        float optH = 9f;
        float curX = startX;
        float curY = startY;

        var values = setting.getBooleanSettings();
        for (int i = 0; i < values.size(); i++) {
            var val = values.get(i);
            boolean enabled = val.isEnabled();
            if (i < optionAnims.size()) optionAnims.get(i).update(enabled ? 1f : 0f);
            float anim = i < optionAnims.size() ? optionAnims.get(i).getValue() : (enabled ? 1f : 0f);

            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f);
            float boxW = tw + hPad;

            if (curX - startX + boxW > maxW) { curX = startX; curY += optH + 1f; }

            int fillAlpha = (int)(anim * 120 * alpha);
            if (fillAlpha > 0) {
                DrawUtil.drawRoundedRect(matrices, curX, curY, boxW, optH - 1f,
                        BorderRadius.all(2f), new ColorRGBA(80, 200, 120, fillAlpha));
            }
            int outlineAlpha = (int)((60 + anim * 120) * alpha);
            DrawUtil.drawRoundedBorder(matrices, curX, curY, boxW, optH - 1f,
                    0.7f, BorderRadius.all(2f), new ColorRGBA(80, 200, 120, outlineAlpha));

            ColorRGBA textColor = ColorRGBA.lerp(
                    new ColorRGBA(130, 132, 145, 255),
                    new ColorRGBA(200, 240, 210, 255),
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

        var values = setting.getBooleanSettings();
        for (int i = 0; i < values.size(); i++) {
            var val = values.get(i);
            float tw = Fonts.REGULAR.getWidth(val.getName(), 7f);
            float boxW = tw + hPad;
            if (curX - startX + boxW > maxW) { curX = startX; curY += optH + 1f; }
            if (isHovered(mouseX, mouseY, curX, curY, boxW, optH)) {
                val.setEnabled(!val.isEnabled());
                return;
            }
            curX += boxW + 2f;
        }
    }
}
