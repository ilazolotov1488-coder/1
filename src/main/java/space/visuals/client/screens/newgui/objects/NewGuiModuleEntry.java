package space.visuals.client.screens.newgui.objects;

import net.minecraft.client.util.math.MatrixStack;
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

    private static final float ROW_H   = 18f;
    private static final float OBJ_GAP = 9.5f;

    private final Module module;
    private final List<NewGuiSettingEntry> settings = new ArrayList<>();

    private boolean expanded  = false;
    private boolean isBinding = false;
    private float   expandAnim = 0f;

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

    private float settingsHeight() {
        float h = 0;
        for (NewGuiSettingEntry s : settings)
            if (s.getSetting() == null || s.getSetting().isVisible())
                h += s.getHeight() + OBJ_GAP;
        return h;
    }

    public float getTotalHeight() {
        return ROW_H + settingsHeight() * expandAnim;
    }

    public float getSettingsExpandedOff() {
        return settingsHeight() * expandAnim;
    }

    public void render(UIContext ctx, float mx, float my,
                       float px, float rowY, float pw) {
        MatrixStack mat = ctx.getMatrices();

        expandAnim += ((expanded ? 1f : 0f) - expandAnim) * 0.2f;

        boolean on = module.isEnabled();

        // Фон строки
        float totalH = ROW_H + settingsHeight() * expandAnim;
        DrawUtil.drawRoundedRect(mat, px + 4f, rowY, pw - 8f, totalH - 1f,
                BorderRadius.all(3f), new ColorRGBA(0, 0, 0, 50));

        // Название
        String name = isBinding ? "..." : module.getName();
        int col = on
                ? new ColorRGBA(255, 255, 255, 255).getRGB()
                : new ColorRGBA(140, 140, 140, 140).getRGB();
        var font = on ? Fonts.SEMIBOLD : Fonts.REGULAR;
        MsdfRenderer.renderText(font, name, 8f, col,
                mat.peek().getPositionMatrix(), px + 10f, rowY + 5f, 0);

        // "..." справа
        if (!settings.isEmpty()) {
            int dc = on
                    ? new ColorRGBA(255, 255, 255, 180).getRGB()
                    : new ColorRGBA(120, 120, 130, 150).getRGB();
            float dw = Fonts.REGULAR.getWidth("...", 7f);
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 7f, dc,
                    mat.peek().getPositionMatrix(), px + pw - dw - 8f, rowY + 5f, 0);
        }

        // Настройки
        if (expandAnim > 0.01f) {
            float sy  = rowY + ROW_H;
            float vis = settingsHeight() * expandAnim;
            ctx.enableScissor((int)px, (int)sy, (int)(px + pw), (int)(sy + vis + 2f));
            float oy = sy + 2f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                se.render(ctx, mx, my, px, oy, pw, expandAnim);
                oy += (se.getHeight() + OBJ_GAP) * expandAnim;
            }
            ctx.disableScissor();
        }
    }

    public void onMouseClicked(float mx, float my, MouseButton btn,
                               float px, float rowY, float pw) {
        boolean inRow = mx >= px + 4f && mx <= px + pw - 4f
                && my >= rowY && my <= rowY + ROW_H;
        if (inRow) {
            if (btn == MouseButton.LEFT)   module.toggle();
            else if (btn == MouseButton.RIGHT && !settings.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) isBinding = !isBinding;
        }
        if (expanded && expandAnim > 0.5f) {
            float sy = rowY + ROW_H + 2f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                se.onMouseClicked(mx, my, btn, px, sy, pw);
                sy += se.getHeight() + OBJ_GAP;
            }
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiSettingEntry se : settings) se.onMouseReleased(mx, my, btn);
    }

    public void onKeyPressed(int key, int scan, int mods) {
        if (isBinding) {
            module.setKeyCode(key == 256 || key == 259 || key == 261 ? -1 : key);
            isBinding = false;
            return;
        }
        for (NewGuiSettingEntry se : settings) se.onKeyPressed(key, scan, mods);
    }

    public List<NewGuiSettingEntry> getSettingEntries() { return settings; }
    public boolean isExpanded() { return expanded; }
}
