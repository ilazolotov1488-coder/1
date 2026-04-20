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

    // Panel.java: originalHeight = 18f
    private static final float ROW_H = 18f;
    // Panel.java: object.height = 8f, gap = 9.5f
    private static final float OBJ_H   = 8f;
    private static final float OBJ_GAP = 9.5f;

    private final Module module;
    private final List<NewGuiSettingEntry> settingEntries = new ArrayList<>();

    private boolean expanded = false;
    private boolean isBinding = false;

    // Panel.java: expand_anim = AnimationMath.fast(anim, target, 20) → lerp ~0.15
    private float expandAnim = 0f;

    private float lastX, lastY, lastW;

    public NewGuiModuleEntry(Module module) {
        this.module = module;
        for (Setting s : module.getSettings()) {
            if      (s instanceof NumberSetting      ns) settingEntries.add(new NewGuiSliderSetting(ns));
            else if (s instanceof ModeSetting        ms) settingEntries.add(new NewGuiModeSetting(ms));
            else if (s instanceof BooleanSetting     bs) settingEntries.add(new NewGuiBooleanSetting(bs));
            else if (s instanceof MultiBooleanSetting mb) settingEntries.add(new NewGuiMultiSetting(mb));
            else if (s instanceof ButtonSetting      bt) settingEntries.add(new NewGuiButtonSetting(bt));
            else if (s instanceof KeySetting         ks) settingEntries.add(new NewGuiKeySetting(ks));
        }
    }

    public Module getModule() { return module; }
    public boolean isExpanded() { return expanded; }
    public List<NewGuiSettingEntry> getSettingEntries() { return settingEntries; }

    /** Высота раскрытых настроек с анимацией — для расчёта off в Panel */
    public float getSettingsExpandedHeight() {
        float h = 0;
        for (NewGuiSettingEntry s : settingEntries) {
            if (s.getSetting() == null || s.getSetting().isVisible()) {
                h += (s.getHeight() + OBJ_GAP) * expandAnim;
            }
        }
        return h;
    }

    public float getTotalHeight() {
        return ROW_H + getSettingsExpandedHeight();
    }

    public void render(UIContext ctx, float mx, float my, float panelX, float y, float panelW) {
        MatrixStack mat = ctx.getMatrices();

        // expand_anim lerp
        float target = expanded ? 1f : 0f;
        expandAnim += (target - expandAnim) * 0.15f;

        lastX = panelX + 1f;
        lastY = y;
        lastW = panelW - 1f;

        boolean on = module.isEnabled();

        // Panel.java: drawRoundedRect(m.x+6.5, m.y, m.width-12, moduleHeight-2.3, 3.2, rgba(0,0,0,60))
        float totalH = ROW_H + getSettingsExpandedHeight();
        DrawUtil.drawRoundedRect(mat,
                panelX + 6.5f, y,
                panelW - 12f, totalH - 2.3f,
                BorderRadius.all(3.2f),
                new ColorRGBA(0, 0, 0, 60));

        // Panel.java: Fonts[15].drawString(name, x+12, y+off+offset+scroll+18.2)
        // Относительно y модуля: текст на y+5.7 (18.2 - 12.5 = 5.7)
        String name = isBinding ? "..." : module.getName();
        int textColor = on
                ? new ColorRGBA(255, 255, 255, 255).getRGB()
                : new ColorRGBA(140, 140, 140, 128).getRGB();

        MsdfRenderer.renderText(Fonts.REGULAR, name, 8f, textColor,
                mat.peek().getPositionMatrix(),
                panelX + 12f, y + 5.7f, 0);

        // Panel.java: Fonts[12].drawCenteredString("...", m.x+m.width-size+2, m.y+5)
        // size=10, translate(-5,0) → x = panelX+panelW-10+2-5 = panelX+panelW-13
        if (!settingEntries.isEmpty()) {
            int dotsColor = on
                    ? new ColorRGBA(255, 255, 255, 200).getRGB()
                    : new ColorRGBA(140, 140, 140, 128).getRGB();
            float dotsX = panelX + panelW - 13f;
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 7f, dotsColor,
                    mat.peek().getPositionMatrix(), dotsX, y + 5f, 0);
        }

        // Настройки раскрытые
        if (expandAnim > 0.01f) {
            float yd = 6f;
            float sy = y + ROW_H;
            float visH = getSettingsExpandedHeight();

            ctx.enableScissor((int)panelX, (int)sy, (int)(panelX + panelW), (int)(sy + visH + 2f));

            for (NewGuiSettingEntry se : settingEntries) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                // Panel.java: object.x=this.x, object.y=y+yd+off+offset+scroll+25, object.width=this.width, object.height=8
                // Относительно sy: object.y = sy + yd - ROW_H + (25-12.5) = sy + yd - ROW_H + 12.5
                float objY = sy + yd - ROW_H + 12.5f;
                se.render(ctx, mx, my, panelX, objY, panelW, expandAnim);
                yd += (se.getHeight() + OBJ_GAP) * expandAnim;
            }

            ctx.disableScissor();
        }
    }

    public void onMouseClicked(float mx, float my, MouseButton btn, float panelX, float y, float panelW) {
        lastX = panelX + 1f;
        lastY = y;
        lastW = panelW - 1f;

        // Panel.java: isInRegion(mx, my, m.x+8, m.y, m.width-20+4, m.height) && button==1 → expand
        boolean inRow = mx >= panelX + 8f && mx <= panelX + panelW - 16f
                && my >= y && my <= y + ROW_H;

        if (inRow) {
            if (btn == MouseButton.LEFT) module.toggle();
            else if (btn == MouseButton.RIGHT && !settingEntries.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) isBinding = !isBinding;
        }

        // Передаём клик в настройки
        if (expanded && expandAnim > 0.5f) {
            float yd = 6f;
            float sy = y + ROW_H;
            for (NewGuiSettingEntry se : settingEntries) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                float objY = sy + yd - ROW_H + 12.5f;
                se.onMouseClicked(mx, my, btn, panelX, objY, panelW);
                yd += se.getHeight() + OBJ_GAP;
            }
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiSettingEntry se : settingEntries) se.onMouseReleased(mx, my, btn);
    }

    public void onKeyPressed(int key, int scan, int mods) {
        if (isBinding) {
            module.setKeyCode(key == 256 || key == 259 || key == 261 ? -1 : key);
            isBinding = false;
            return;
        }
        for (NewGuiSettingEntry se : settingEntries) se.onKeyPressed(key, scan, mods);
    }
}
