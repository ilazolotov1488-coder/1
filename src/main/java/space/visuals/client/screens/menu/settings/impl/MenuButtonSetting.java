package space.visuals.client.screens.menu.settings.impl;

import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.modules.api.setting.impl.ButtonSetting;
import space.visuals.client.screens.menu.settings.api.MenuSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.Rect;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;

public class MenuButtonSetting extends MenuSetting {
    private final ButtonSetting button;

    private final Animation animHovered = new Animation(200,0, Easing.QUAD_IN_OUT);
    Rect bounds;

    public MenuButtonSetting(ButtonSetting button) {
        this.button = button;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {
        bounds = new Rect(x+8,settingY,moduleWidth-8*2,16);
        ctx.drawRoundedRect(x+8,settingY,moduleWidth-8*2,16, BorderRadius.all(4),theme.getForegroundColor().mulAlpha(alpha));
        ctx.drawRoundedRect(x+8,settingY,moduleWidth-8*2,16, BorderRadius.all(4),theme.getForegroundStroke().mulAlpha(alpha));
        ctx.drawText(Fonts.MEDIUM.getFont(7),button.getName(),x+(moduleWidth-Fonts.MEDIUM.getWidth(button.getName(), 7))/2f,settingY+5,textColor);

    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if(bounds!=null && bounds.contains(mouseX,mouseY) && button==MouseButton.LEFT) {
            this.button.toggle();
        }
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 16;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
