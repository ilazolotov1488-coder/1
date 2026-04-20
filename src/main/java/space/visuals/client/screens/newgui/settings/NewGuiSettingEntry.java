package space.visuals.client.screens.newgui.settings;

import space.visuals.client.modules.api.setting.Setting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.UIContext;

/**
 * Базовый класс для элементов настроек в новом Click GUI.
 */
public abstract class NewGuiSettingEntry {

    public abstract Setting getSetting();

    public abstract float getHeight();

    public abstract void render(UIContext ctx, float mouseX, float mouseY,
                                float x, float y, float width, float alpha);

    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {}

    public void onMouseReleased(float mouseX, float mouseY, MouseButton button) {}

    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {}

    protected boolean isHovered(float mouseX, float mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
