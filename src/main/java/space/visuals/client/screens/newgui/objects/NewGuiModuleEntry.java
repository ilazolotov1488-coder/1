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

public class NewGuiModuleEntry implements IMinecraft {

    private static final float ROW_H = 22f;
    private static final float SETTING_PAD = 1f;

    // Цвета как в референсе
    private static final ColorRGBA TEXT_ON  = new ColorRGBA(255, 255, 255, 255);
    private static final ColorRGBA TEXT_OFF = new ColorRGBA(140, 140, 140, 180);
    private static final ColorRGBA DOTS_ON  = new ColorRGBA(200, 200, 210, 200);
    private static final ColorRGBA DOTS_OFF = new ColorRGBA(100, 100, 110, 160);
    private static final ColorRGBA HOVER_BG = new ColorRGBA(35, 37, 48, 120);
    private static final ColorRGBA SET_BG   = new ColorRGBA(12, 13, 18, 180);

    private final Module module;
    private final List<NewGuiSettingEntry> settings = new ArrayList<>();

    private boolean expanded = false;
    private boolean binding = false;

    private final Animation expandAnim = new Animation(180, 0f, Easing.CUBIC_IN_OUT);
    private final Animation hoverAnim  = new Animation(120, 0f, Easing.LINEAR);

    private float lastX, lastY, lastW;

    public NewGuiModuleEntry(Module module) {
        this.module = module;
        for (Setting s : module.getSettings()) {
            if      (s instanceof NumberSetting      ns) settings.add(new NewGuiSliderSetting(ns));
            else if (s instanceof ModeSetting        ms) settings.add(new NewGuiModeSetting(ms));
            else if (s instanceof BooleanSetting     bs) settings.add(new NewGuiBooleanSetting(bs));
            else if (s instanceof MultiBooleanSetting mb) settings.add(new NewGuiMultiSetting(mb));
            else if (s instanceof ButtonSetting      bt) settings.add(new NewGuiButtonSetting(bt));
            else if (s instanceof KeySetting         ks) settings.add(new NewGuiKeySetting(ks));
        }
    }

    public Module getModule() { return module; }

    public float getTotalHeight() {
        float h = ROW_H;
        float ev = expandAnim.getValue();
        if (ev > 0.01f) h += getSettingsH() * ev;
        return h;
    }

    private float getSettingsH() {
        float h = 4f;
        for (NewGuiSettingEntry s : settings) {
            if (s.getSetting() == null || s.getSetting().isVisible()) h += s.getHeight() + SETTING_PAD;
        }
        return h + 4f;
    }

    public void render(UIContext ctx, float mx, float my, float panelX, float y, float panelW, float alpha) {
        MatrixStack mat = ctx.getMatrices();
        lastX = panelX + 8f;
        lastY = y;
        lastW = panelW - 16f;

        boolean hovered = isHovered(mx, my);
        hoverAnim.update(hovered ? 1f : 0f);
        expandAnim.update(expanded ? 1f : 0f);

        float hv = hoverAnim.getValue();
        float ev = expandAnim.getValue();
        boolean on = module.isEnabled();

        // Hover фон строки
        if (hv > 0.01f) {
            DrawUtil.drawRoundedRect(mat, lastX, y, lastW, ROW_H - 1f,
                    BorderRadius.all(3f),
                    new ColorRGBA(HOVER_BG.getRed(), HOVER_BG.getGreen(), HOVER_BG.getBlue(),
                            (int)(HOVER_BG.getAlpha() * hv * alpha)));
        }

        // Название модуля — жирный если включён
        String name = binding ? "..." : module.getName();
        ColorRGBA textColor = on
                ? new ColorRGBA(TEXT_ON.getRed(), TEXT_ON.getGreen(), TEXT_ON.getBlue(), (int)(TEXT_ON.getAlpha() * alpha))
                : new ColorRGBA(TEXT_OFF.getRed(), TEXT_OFF.getGreen(), TEXT_OFF.getBlue(), (int)(TEXT_OFF.getAlpha() * alpha));

        // Включённые — SEMIBOLD, выключенные — REGULAR (как в референсе)
        var font = on ? Fonts.SEMIBOLD : Fonts.REGULAR;
        MsdfRenderer.renderText(font, name, 8.5f, textColor.getRGB(),
                mat.peek().getPositionMatrix(), lastX + 4f, y + ROW_H / 2f - 4.5f, 0);

        // "..." справа если есть настройки
        if (!settings.isEmpty()) {
            ColorRGBA dotsC = on
                    ? new ColorRGBA(DOTS_ON.getRed(), DOTS_ON.getGreen(), DOTS_ON.getBlue(), (int)(DOTS_ON.getAlpha() * alpha))
                    : new ColorRGBA(DOTS_OFF.getRed(), DOTS_OFF.getGreen(), DOTS_OFF.getBlue(), (int)(DOTS_OFF.getAlpha() * alpha));
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 8f, dotsC.getRGB(),
                    mat.peek().getPositionMatrix(),
                    lastX + lastW - Fonts.REGULAR.getWidth("...", 8f) - 2f,
                    y + ROW_H / 2f - 4.5f, 0);
        }

        // Раскрытые настройки
        if (ev > 0.01f) {
            float settH = getSettingsH() * ev;
            float settY = y + ROW_H;

            // Фон настроек
            DrawUtil.drawRoundedRect(mat, lastX, settY, lastW, settH,
                    BorderRadius.bottom(3f, 3f),
                    new ColorRGBA(SET_BG.getRed(), SET_BG.getGreen(), SET_BG.getBlue(),
                            (int)(SET_BG.getAlpha() * ev * alpha)));

            ctx.enableScissor((int)panelX, (int)settY, (int)(panelX + panelW), (int)(settY + settH));
            float sy = settY + 4f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                se.render(ctx, mx, my, lastX, sy, lastW, alpha * ev);
                sy += se.getHeight() + SETTING_PAD;
            }
            ctx.disableScissor();
        }
    }

    public void onMouseClicked(float mx, float my, MouseButton btn, float panelX, float y, float panelW) {
        lastX = panelX + 8f;
        lastY = y;
        lastW = panelW - 16f;

        if (isHovered(mx, my)) {
            if (btn == MouseButton.LEFT)   module.toggle();
            else if (btn == MouseButton.RIGHT && !settings.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) binding = !binding;
        }

        if (expanded) {
            float sy = y + ROW_H + 4f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                se.onMouseClicked(mx, my, btn, lastX, sy, lastW);
                sy += se.getHeight() + SETTING_PAD;
            }
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiSettingEntry se : settings) se.onMouseReleased(mx, my, btn);
    }

    public void onKeyPressed(int key, int scan, int mods) {
        if (binding) {
            module.setKeyCode(key == 256 || key == 259 || key == 261 ? -1 : key);
            binding = false;
            return;
        }
        for (NewGuiSettingEntry se : settings) se.onKeyPressed(key, scan, mods);
    }

    private boolean isHovered(float mx, float my) {
        return mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + ROW_H - 1f;
    }
}
