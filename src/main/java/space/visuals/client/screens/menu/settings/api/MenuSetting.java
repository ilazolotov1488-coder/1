package space.visuals.client.screens.menu.settings.api;

import space.visuals.base.theme.Theme;

import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;

public abstract class MenuSetting {

    protected float height;

    public abstract void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY, float moduleWidth, float alpha, float animEnable, ColorRGBA themeColor,ColorRGBA textColor,ColorRGBA descriptionColor, Theme theme);

    public abstract void onMouseClicked(double mouseX, double mouseY, MouseButton button);

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract boolean isVisible();

    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        return false;
    }

    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {

    }
}
