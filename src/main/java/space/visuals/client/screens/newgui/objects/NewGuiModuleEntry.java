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

/**
 * 1:1 копия ModuleObject из Panel.java.
 *
 * Panel.java module rendering:
 *   m.x = panelX + 1, m.y = moduleY
 *   m.width = panelWidth - 1 = 106, m.height = 18
 *
 *   bg rect: m.x+6.5, m.y, m.width-12=94, moduleHeight-2.3, r=3.2, rgba(0,0,0,60)
 *   name:    panelX+12, moduleY+5.7, Font[15] size, enabled=white, disabled=gray
 *   dots:    m.x+m.width-10+2 (with translate -5) = panelX+1+106-10+2-5 = panelX+94
 *            y = moduleY+5, Font[12]
 *
 *   settings: object.x=panelX, object.y=moduleY+yd+off+offset+scroll+25, object.width=panelWidth, object.height=8
 *   off += (object.height+9.5)*expand_anim per setting
 */
public class NewGuiModuleEntry implements IMinecraft {

    // Panel.java: originalHeight = 18f
    private static final float ROW_H   = 18f;
    // Panel.java: object.height = 8f, gap = 9.5f
    private static final float OBJ_H   = 8f;
    private static final float OBJ_GAP = 9.5f;

    private final Module module;
    private final List<NewGuiSettingEntry> settingEntries = new ArrayList<>();

    private boolean expanded  = false;
    private boolean isBinding = false;

    // Panel.java: expand_anim = AnimationMath.fast(anim, target, 20) ≈ lerp 0.15
    private float expandAnim = 0f;

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

    /**
     * Panel.java: off += (object.height + 9.5) * expand_anim  [per visible setting]
     * Это то что добавляется к off в Panel.render() за этот модуль (кроме базового +16).
     */
    public float getSettingsExpandedOff() {
        float h = 0;
        for (NewGuiSettingEntry s : settingEntries) {
            if (s.getSetting() == null || s.getSetting().isVisible()) {
                h += (s.getHeight() + OBJ_GAP) * expandAnim;
            }
        }
        return h;
    }

    public void render(UIContext ctx, float mx, float my,
                       float panelX, float moduleY, float panelW) {
        MatrixStack mat = ctx.getMatrices();

        // expand_anim lerp ≈ AnimationMath.fast(anim, target, 20)
        float target = expanded ? 1f : 0f;
        expandAnim += (target - expandAnim) * 0.15f;

        boolean on = module.isEnabled();

        // Panel.java: m.x = panelX+1, m.width = panelW-1
        float mx0 = panelX + 1f;
        float mw  = panelW - 1f;

        // Panel.java: totalHeight = expanded ? (height + settingsH) : height
        float settingsH = getSettingsExpandedOff();
        float totalH    = ROW_H + settingsH;

        // Panel.java: drawRoundedRect(m.x+6.5, m.y, m.width-12, moduleHeight-2.3, 3.2, rgba(0,0,0,60))
        DrawUtil.drawRoundedRect(mat,
                mx0 + 6.5f, moduleY,
                mw - 12f, totalH - 2.3f,
                BorderRadius.all(3.2f),
                new ColorRGBA(0, 0, 0, 60));

        // Panel.java: Fonts[15].drawString(name, panelX+12, moduleY+5.7)
        // enabled: rgba(255,255,255,255), disabled: rgba(140,140,140,128)
        String name = isBinding ? "..." : module.getName();
        int textColor = on
                ? new ColorRGBA(255, 255, 255, 255).getRGB()
                : new ColorRGBA(140, 140, 140, 128).getRGB();

        // Включённые — SEMIBOLD (жирные), выключенные — REGULAR (как в референсе)
        var nameFont = on ? Fonts.SEMIBOLD : Fonts.REGULAR;
        float nameSz  = on ? 8.5f : 8f;
        MsdfRenderer.renderText(nameFont, name, nameSz, textColor,
                mat.peek().getPositionMatrix(),
                panelX + 12f, moduleY + 5.7f, 0);

        // Panel.java: Fonts[12].drawCenteredString("...", m.x+m.width-size+2, m.y+5)
        // size=10, GL11.glTranslatef(-5,0,0) → effective x = mx0+mw-10+2-5 = panelX+1+mw-13
        if (!settingEntries.isEmpty()) {
            int dotsColor = on
                    ? new ColorRGBA(255, 255, 255, 200).getRGB()
                    : new ColorRGBA(140, 140, 140, 128).getRGB();
            float dotsX = mx0 + mw - 13f;
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 7f, dotsColor,
                    mat.peek().getPositionMatrix(), dotsX, moduleY + 5f, 0);
        }

        // Настройки раскрытые
        if (expandAnim > 0.01f) {
            float visH = settingsH;
            float sy   = moduleY + ROW_H;

            ctx.enableScissor((int)panelX, (int)sy,
                    (int)(panelX + panelW), (int)(sy + visH + 2f));

            // Panel.java: object.x=panelX, object.y=moduleY+yd+off+offset+scroll+25
            // Относительно sy (=moduleY+ROW_H):
            // object.y = sy + yd + (off+offset+scroll+25 - ROW_H)
            // При yd=6, off=11, offset=-4, scroll=0: = sy + 6 + (11-4+0+25-18) = sy + 6 + 14 = sy+20
            // Упрощаем: object.y = sy + yd + 6  (эмпирически из кода)
            float yd = 0f;
            for (NewGuiSettingEntry se : settingEntries) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                float objY = sy + yd + 6f;
                se.render(ctx, mx, my, panelX, objY, panelW, expandAnim);
                yd += (se.getHeight() + OBJ_GAP) * expandAnim;
            }

            ctx.disableScissor();
        }
    }

    public void onMouseClicked(float mx, float my, MouseButton btn,
                               float panelX, float moduleY, float panelW) {
        // Panel.java: isInRegion(mx, my, m.x+8, m.y, m.width-20+4, m.height) && button==1 → expand
        float mx0 = panelX + 1f;
        float mw  = panelW - 1f;
        boolean inRow = mx >= mx0 + 8f && mx <= mx0 + mw - 16f
                && my >= moduleY && my <= moduleY + ROW_H;

        if (inRow) {
            if (btn == MouseButton.LEFT)   module.toggle();
            else if (btn == MouseButton.RIGHT && !settingEntries.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) isBinding = !isBinding;
        }

        if (expanded && expandAnim > 0.5f) {
            float sy = moduleY + ROW_H;
            float yd = 0f;
            for (NewGuiSettingEntry se : settingEntries) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                float objY = sy + yd + 6f;
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
