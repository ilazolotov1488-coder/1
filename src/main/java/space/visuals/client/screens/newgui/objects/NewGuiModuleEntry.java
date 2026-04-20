package space.visuals.client.screens.newgui.objects;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.setting.Setting;
import space.visuals.client.modules.api.setting.impl.*;
import space.visuals.client.screens.newgui.settings.*;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Элемент модуля в новом Click GUI.
 * Отображает название, статус и настройки при раскрытии.
 */
public class NewGuiModuleEntry {

    private static final float MODULE_HEIGHT = 18f;
    private static final float SETTING_HEIGHT = 16f;
    private static final float SETTING_PADDING = 1.5f;

    private static final ColorRGBA MODULE_BG = new ColorRGBA(22, 23, 30, 0);
    private static final ColorRGBA MODULE_BG_HOVER = new ColorRGBA(35, 37, 50, 180);
    private static final ColorRGBA TEXT_ACTIVE = new ColorRGBA(255, 255, 255, 255);
    private static final ColorRGBA TEXT_INACTIVE = new ColorRGBA(120, 122, 135, 255);
    private static final ColorRGBA ACCENT = new ColorRGBA(100, 180, 255, 255);
    private static final ColorRGBA SEPARATOR = new ColorRGBA(35, 37, 50, 255);

    private static final Identifier ICON_CHECK = Identifier.of("space", "textures/check.png");
    private static final Identifier ICON_X = Identifier.of("space", "textures/x.png");

    private final Module module;
    private final List<NewGuiSettingEntry> settingEntries = new ArrayList<>();

    private boolean expanded = false;
    private boolean isBinding = false;

    private final Animation expandAnim = new Animation(200, 0f, Easing.CUBIC_IN_OUT);
    private final Animation enableAnim;
    private final Animation hoverAnim = new Animation(150, 0f, Easing.LINEAR);

    // Кэшированные bounds для hit-test
    private float lastX, lastY, lastWidth;

    public NewGuiModuleEntry(Module module) {
        this.module = module;
        this.enableAnim = new Animation(200, module.isEnabled() ? 1f : 0f, Easing.LINEAR);

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
            float settingsH = getSettingsHeight();
            h += settingsH * expandVal;
        }
        return h;
    }

    private float getSettingsHeight() {
        float h = 0;
        for (NewGuiSettingEntry entry : settingEntries) {
            if (entry.getSetting() == null || entry.getSetting().visible()) {
                h += entry.getHeight() + SETTING_PADDING;
            }
        }
        return h;
    }

    public void render(UIContext ctx, float mouseX, float mouseY, float panelX, float y, float panelWidth, float alpha) {
        MatrixStack matrices = ctx.getMatrices();
        lastX = panelX + 5f;
        lastY = y;
        lastWidth = panelWidth - 10f;

        // Анимации
        enableAnim.update(module.isEnabled() ? 1f : 0f);
        expandAnim.update(expanded ? 1f : 0f);

        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim.update(hovered ? 1f : 0f);

        float enableVal = enableAnim.getValue();
        float hoverVal = hoverAnim.getValue();

        // Фон модуля
        int bgAlpha = (int)((40 + 60 * hoverVal) * alpha);
        DrawUtil.drawRoundedRect(matrices, lastX, y, lastWidth, MODULE_HEIGHT - 2f,
                BorderRadius.all(3f), new ColorRGBA(30, 32, 42, bgAlpha));

        // Иконка статуса (check/x)
        float iconSize = 7.5f;
        float iconX = lastX + 4f;
        float iconY = y + MODULE_HEIGHT / 2f - iconSize / 2f - 1f;

        // Цвет иконки с анимацией
        int checkAlpha = (int)(255 * enableVal * alpha);
        int crossAlpha = (int)(255 * (1f - enableVal) * alpha);

        if (crossAlpha > 5) {
            ColorRGBA crossColor = new ColorRGBA(220, 60, 60, crossAlpha);
            DrawUtil.drawTexture(matrices, ICON_X, iconX, iconY, iconSize, iconSize, crossColor);
        }
        if (checkAlpha > 5) {
            ColorRGBA checkColor = new ColorRGBA(120, 220, 80, checkAlpha);
            DrawUtil.drawTexture(matrices, ICON_CHECK, iconX, iconY, iconSize, iconSize, checkColor);
        }

        // Название модуля
        String displayName = isBinding ? "..." : module.getName();
        ColorRGBA textColor = ColorRGBA.lerp(TEXT_INACTIVE, TEXT_ACTIVE, enableVal).withAlpha((int)(255 * alpha));

        float textX = lastX + 14f;
        float textY = y + MODULE_HEIGHT / 2f - 4f;
        MsdfRenderer.renderText(Fonts.REGULAR, displayName, 8f, textColor.getRGB(),
                matrices.peek().getPositionMatrix(), textX, textY, 0);

        // Индикатор настроек (три точки если есть настройки)
        if (!settingEntries.isEmpty()) {
            ColorRGBA dotsColor = new ColorRGBA(100, 102, 120, (int)(180 * alpha));
            float dotsX = lastX + lastWidth - 10f;
            float dotsY = y + MODULE_HEIGHT / 2f - 1.5f;
            for (int i = 0; i < 3; i++) {
                DrawUtil.drawRoundedRect(matrices, dotsX + i * 3f, dotsY, 1.5f, 1.5f,
                        BorderRadius.all(0.75f), dotsColor);
            }
        }

        // Настройки (раскрытые)
        float expandVal = expandAnim.getValue();
        if (expandVal > 0.01f) {
            float settingY = y + MODULE_HEIGHT;
            for (NewGuiSettingEntry entry : settingEntries) {
                if (entry.getSetting() != null && !entry.getSetting().visible()) continue;
                float entryH = entry.getHeight();
                // Scissor по высоте анимации
                float visibleH = getSettingsHeight() * expandVal;
                ctx.enableScissor((int) panelX, (int)(y + MODULE_HEIGHT - 1),
                        (int)(panelX + panelWidth), (int)(y + MODULE_HEIGHT + visibleH + 1));
                entry.render(ctx, mouseX, mouseY, lastX, settingY, lastWidth, alpha * expandVal);
                ctx.disableScissor();
                settingY += entryH + SETTING_PADDING;
            }
        }
    }

    public void onMouseClicked(float mouseX, float mouseY, MouseButton button, float panelX, float y, float panelWidth) {
        lastX = panelX + 5f;
        lastY = y;
        lastWidth = panelWidth - 10f;

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
                if (entry.getSetting() != null && !entry.getSetting().visible()) continue;
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
