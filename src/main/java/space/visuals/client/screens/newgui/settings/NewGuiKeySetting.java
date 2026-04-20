package space.visuals.client.screens.newgui.settings;

import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.KeySetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class NewGuiKeySetting extends NewGuiSettingEntry {

    private final KeySetting setting;
    private boolean binding = false;

    public NewGuiKeySetting(KeySetting setting) {
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

        MsdfRenderer.renderText(Fonts.REGULAR, setting.getName(), 7.5f,
                new ColorRGBA(200, 202, 215, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), x + 4f, y + getHeight() / 2f - 3.5f, 0);

        String keyStr = binding ? "..." : (setting.getNameKey() == null || setting.getNameKey().isEmpty() ? "NONE" : setting.getNameKey());
        float keyW = Fonts.REGULAR.getWidth(keyStr, 7f) + 6f;
        float keyX = x + width - keyW - 4f;

        DrawUtil.drawRoundedRect(matrices, keyX, y + 2f, keyW, getHeight() - 4f,
                BorderRadius.all(2f), new ColorRGBA(40, 80, 160, (int)(120 * alpha)));

        float textW = Fonts.REGULAR.getWidth(keyStr, 7f);
        MsdfRenderer.renderText(Fonts.REGULAR, keyStr, 7f,
                new ColorRGBA(180, 200, 240, (int)(255 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(),
                keyX + keyW / 2f - textW / 2f, y + getHeight() / 2f - 3.5f, 0);
    }

    @Override
    public void onMouseClicked(float mouseX, float mouseY, MouseButton button,
                               float x, float y, float width) {
        if (button == MouseButton.LEFT) {
            float keyW = Fonts.REGULAR.getWidth(binding ? "..." : (setting.getNameKey() == null ? "NONE" : setting.getNameKey()), 7f) + 6f;
            float keyX = x + width - keyW - 4f;
            if (isHovered(mouseX, mouseY, keyX, y + 2f, keyW, getHeight() - 4f)) {
                binding = !binding;
            }
        }
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                setting.setKeyCode(-1);
            } else {
                setting.setKeyCode(keyCode);
            }
            binding = false;
        }
    }
}
