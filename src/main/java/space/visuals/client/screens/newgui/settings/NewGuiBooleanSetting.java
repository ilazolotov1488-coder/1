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

        // BooleanObject.java: outline at x+12, y+3, 9x9, r=1.8, rgba(255,255,255,100)
        float cbX = x + 12f, cbY = y + 3f;
        DrawUtil.drawRoundedBorder(matrices, cbX, cbY, 9f, 9f,
                1.8f, BorderRadius.all(1.8f),
                new ColorRGBA(255, 255, 255, (int)(100 * alpha)));

        // BooleanObject.java: check icon green rgba(165,255,0), x icon red rgba(255,50,50)
        float iconSize = 7.5f;
        int checkAlpha = (int)(255 * val * alpha);
        int crossAlpha = (int)(255 * (1f - val) * alpha);

        if (crossAlpha > 5) {
            DrawUtil.drawTexture(matrices, ICON_X, cbX + 0.75f, cbY + 0.75f, iconSize, iconSize,
                    new ColorRGBA(255, 50, 50, crossAlpha));
        }
        if (checkAlpha > 5) {
            DrawUtil.drawTexture(matrices, ICON_CHECK, cbX + 0.75f, cbY + 0.75f, iconSize, iconSize,
                    new ColorRGBA(165, 255, 0, checkAlpha));
        }

        // BooleanObject.java: text at x+24, y+7, Font[12], color lerp white/gray
        int colorfont = ColorRGBA.lerp(
                new ColorRGBA(255, 255, 255, 100),
                new ColorRGBA(255, 255, 255, 255),
                val
        ).withAlpha((int)(255 * alpha)).getRGB();

        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f, colorfont,
                matrices.peek().getPositionMatrix(), x + 24f, y + 4f, 0);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT && isHovered(mouseX, mouseY, x + 12f, y + 3f, 9f, 9f)) {
            setting.toggle();
        }
    }
}
