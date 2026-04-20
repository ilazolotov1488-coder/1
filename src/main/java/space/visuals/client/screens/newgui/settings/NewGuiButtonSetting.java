package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.ButtonSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class NewGuiButtonSetting extends NewGuiSettingEntry {

    private final ButtonSetting setting;
    private boolean hovered = false;

    public NewGuiButtonSetting(ButtonSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() { return 14f; }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();
        float btnX = x + 4f;
        float btnW = width - 8f;
        float btnH = getHeight() - 2f;

        hovered = isHovered(mouseX, mouseY, btnX, y, btnW, btnH);

        int bgAlpha = (int)((hovered ? 160 : 100) * alpha);
        DrawUtil.drawRoundedRect(matrices, btnX, y, btnW, btnH,
                BorderRadius.all(3f), new ColorRGBA(60, 100, 180, bgAlpha));

        float textW = Fonts.REGULAR.getWidth(setting.getName(), 7.5f);
        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(220, 225, 240, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(),
                btnX + btnW / 2f - textW / 2f, y + btnH / 2f - 3.5f, 0);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT && hovered) {
            setting.getRunnable().run();
        }
    }
}
