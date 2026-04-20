package space.visuals.client.screens.newgui.objects;

import net.minecraft.client.util.math.MatrixStack;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.*;
import space.visuals.client.screens.newgui.settings.*;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.interfaces.IMinecraft;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Элемент модуля в новом Click GUI (dart.ru style).
 * Текст белый если включён, серый если выключен.
 * "..." справа если есть настройки (ПКМ для раскрытия).
 */
public class NewGuiModuleEntry implements IMinecraft {

    private static final float MODULE_HEIGHT = 20f;
    private static final float SETTING_HEIGHT = 16f;
    private static final float SETTING_PADDING = 1.5f;

    // dart.ru colors
    private static final ColorRGBA TEXT_ACTIVE   = new ColorRGBA(255, 255, 255, 255);
    private static final ColorRGBA TEXT_INACTIVE = new ColorRGBA(140, 140, 140, 128);
    private static final ColorRGBA MODULE_BG_HOVER = new ColorRGBA(25, 26, 33, 100);
    private static final ColorRGBA SETTINGS_BG   = new ColorRGBA(0, 0, 0, 60);

    private final Module module;
    private final List<NewGuiSettingEntry> settingEntries = new ArrayList<>();

    private boolean expanded = false;
    private boolean isBinding = false;

    private final Animation expandAnim = new Animation(200, 0f, Easing.CUBIC_IN_OUT);
    private final Animation hoverAnim  = new Animation(150, 0f, Easing.LINEAR);

    // Кэшированные bounds для hit-test
    private float lastX, lastY, lastWidth;

    public NewGuiModuleEntry(Module module) {
        this.module = module;

        for (Setting setting : module.getSettings()) {
            if (setting instanceof NumberSetting s) {
                settingEntries.add(new NewGuiSliderSetting(s));
            } else if (setting instanceof ModeSetting s) {
                settingEntries.add(new NewGuiModeSetting(s));
            } else if (setting instanceof BooleanSetting s) {
                settingEntries.add(new NewGuiBooleanSetting(s));
            } else if (setting instanceof MultiBooleanSetting s) {
                settingEntries.add(new NewGuiMultiSetting(s));
            } else if (setting instanceof ButtonSetting s) {
                settingEntries.add(new NewGuiButtonSetting(s));
            } else if (setting instanceof KeySetting s) {
                settingEntries.add(new NewGuiKeySetting(s));
            }
        }
    }

    public Module getModule() {
        return module;
    }

    /**
     * Полная высота элемента (модуль + раскрытые настройки).
     */
    public float getTotalHeight() {
        float h = MODULE_HEIGHT;
        float expandVal = expandAnim.getValue();
        if (expandVal > 0.01f) {
            h += getSettingsHeight() * expandVal;
        }
        return h;
    }

    private float getSettingsHeight() {
        float h = 0;
        for (NewGuiSettingEntry entry : settingEntries) {
            if (entry.getSetting() == null || entry.getSetting().isVisible()) {
                h += entry.getHeight() + SETTING_PADDING;
            }
        }
        return h;
    }

    public void render(UIContext ctx, float mouseX, float mouseY, float panelX, float y, float panelWidth, float alpha) {
        MatrixStack matrices = ctx.getMatrices();
        lastX = panelX + 4f;
        lastY = y;
        lastWidth = panelWidth - 8f;

        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim.update(hovered ? 1f : 0f);
        float hoverVal = hoverAnim.getValue();

        expandAnim.update(expanded ? 1f : 0f);
        float expandVal = expandAnim.getValue();

        // Фон модуля при наведении (dart.ru: rgba(25,26,33,100))
        if (hoverVal > 0.01f) {
            DrawUtil.drawRoundedRect(matrices, lastX, y, lastWidth, MODULE_HEIGHT - 2f,
                    BorderRadius.all(3f),
                    new ColorRGBA(25, 26, 33, (int)(100 * hoverVal * alpha)));
        }

        // Название модуля — белый если включён, серый если нет (dart.ru style)
        String displayName = isBinding ? "..." : module.getName();
        ColorRGBA textColor = module.isEnabled()
                ? new ColorRGBA(255, 255, 255, (int)(255 * alpha))
                : new ColorRGBA(140, 140, 140, (int)(128 * alpha));

        float textY = y + MODULE_HEIGHT / 2f - 4f;
        MsdfRenderer.renderText(Fonts.REGULAR, displayName, 8f, textColor.getRGB(),
                matrices.peek().getPositionMatrix(), lastX + 6f, textY, 0);

        // "..." справа если есть настройки
        if (!settingEntries.isEmpty()) {
            ColorRGBA dotsColor = module.isEnabled()
                    ? new ColorRGBA(255, 255, 255, (int)(200 * alpha))
                    : new ColorRGBA(140, 140, 140, (int)(128 * alpha));
            float dotsX = lastX + lastWidth - 12f;
            float dotsY = y + MODULE_HEIGHT / 2f - 1.5f;
            for (int i = 0; i < 3; i++) {
                DrawUtil.drawRoundedRect(matrices, dotsX + i * 3f, dotsY, 1.5f, 1.5f,
                        BorderRadius.all(0.75f), dotsColor);
            }
        }

        // Настройки (раскрытые) — фон rgba(0,0,0,60)
        if (expandVal > 0.01f) {
            float settingsH = getSettingsHeight() * expandVal;
            DrawUtil.drawRoundedRect(matrices, lastX, y + MODULE_HEIGHT - 1f, lastWidth, settingsH,
                    BorderRadius.all(3f), new ColorRGBA(0, 0, 0, (int)(60 * alpha)));

            float settingY = y + MODULE_HEIGHT;
            for (NewGuiSettingEntry entry : settingEntries) {
                if (entry.getSetting() != null && !entry.getSetting().isVisible()) continue;
                float entryH = entry.getHeight();
                ctx.enableScissor((int) panelX, (int)(y + MODULE_HEIGHT - 1),
                        (int)(panelX + panelWidth), (int)(y + MODULE_HEIGHT + settingsH + 1));
                entry.render(ctx, mouseX, mouseY, lastX, settingY, lastWidth, alpha * expandVal);
                ctx.disableScissor();
                settingY += entryH + SETTING_PADDING;
            }
        }
    }

    public void onMouseClicked(float mouseX, float mouseY, MouseButton button, float panelX, float y, float panelWidth) {
        lastX = panelX + 4f;
        lastY = y;
        lastWidth = panelWidth - 8f;

        if (isHovered(mouseX, mouseY)) {
            if (button == MouseButton.LEFT) {
                module.toggle();
            } else if (button == MouseButton.RIGHT) {
                if (!settingEntries.isEmpty()) {
                    expanded = !expanded;
                }
            } else if (button == MouseButton.MIDDLE) {
                isBinding = !isBinding;
            }
        }

        // Передаём клик в настройки
        if (expanded) {
            float settingY = y + MODULE_HEIGHT;
            for (NewGuiSettingEntry entry : settingEntries) {
                if (entry.getSetting() != null && !entry.getSetting().isVisible()) continue;
                entry.onMouseClicked(mouseX, mouseY, button, lastX, settingY, lastWidth);
                settingY += entry.getHeight() + SETTING_PADDING;
            }
        }
    }

    public void onMouseReleased(float mouseX, float mouseY, MouseButton button) {
        for (NewGuiSettingEntry entry : settingEntries) {
            entry.onMouseReleased(mouseX, mouseY, button);
        }
    }

    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (isBinding) {
            if (keyCode == 256 || keyCode == 259 || keyCode == 261) {
                module.setKeyCode(-1);
            } else {
                module.setKeyCode(keyCode);
            }
            isBinding = false;
            return;
        }
        for (NewGuiSettingEntry entry : settingEntries) {
            entry.onKeyPressed(keyCode, scanCode, modifiers);
        }
    }

    private boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= lastX && mouseX <= lastX + lastWidth
                && mouseY >= lastY && mouseY <= lastY + MODULE_HEIGHT - 2f;
    }
}
