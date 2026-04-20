package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class NewGuiBooleanSetting extends NewGuiSettingEntry {

    private static final Identifier ICON_CHECK = Identifier.of("space", "textures/check.png");
    private static final Identifier ICON_X = Identifier.of("space", "textures/x.png");

    private final BooleanSetting setting;
    private final Animation enableAnim;

    public NewGuiBooleanSetting(BooleanSetting setting) {
        this.setting = setting;
        this.enableAnim = new Animation(150, setting.isEnabled() ? 1f : 0f, Easing.LINEAR);
    }

    @Override
    public Setting getSetting() { return setting; }

    @Override
    public float getHeight() { return 14f; }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY,
                       float x, float y, float width, float alpha) {
        MatrixStack matrices = ctx.getMatrices();
        enableAnim.update(setting.isEnabled() ? 1f : 0f);
        float val = enableAnim.getValue();

        // Иконка
        float iconSize = 7f;
        float iconX = x + 4f;
        float iconY = y + getHeight() / 2f - iconSize / 2f;

        int checkAlpha = (int)(255 * val * alpha);
        int crossAlpha = (int)(255 * (1f - val) * alpha);

        // Рамка чекбокса
        DrawUtil.drawRoundedBorder(matrices, iconX - 0.5f, iconY - 0.5f, iconSize + 1f, iconSize + 1f,
                0.8f, BorderRadius.all(2f), new ColorRGBA(100, 102, 120, (int)(150 * alpha)));

        if (crossAlpha > 5) {
            DrawUtil.drawTexture(matrices, ICON_X, iconX, iconY, iconSize, iconSize,
                    new ColorRGBA(220, 60, 60, crossAlpha));
        }
        if (checkAlpha > 5) {
            DrawUtil.drawTexture(matrices, ICON_CHECK, iconX, iconY, iconSize, iconSize,
                    new ColorRGBA(120, 220, 80, checkAlpha));
        }

        // Название
        ColorRGBA textColor = ColorRGBA.lerp(
                new ColorRGBA(110, 112, 125, 255),
                new ColorRGBA(220, 222, 235, 255),
                val
        ).withAlpha((int)(255 * alpha));

        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f, textColor.getRGB(),
                matrices.peek().getPositionMatrix(), x + 14f, y + getHeight() / 2f - 3.5f, 0);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT && isHovered(mouseX, mouseY, x + 3f, y, 11f, getHeight())) {
            setting.toggle();
        }
    }
}
