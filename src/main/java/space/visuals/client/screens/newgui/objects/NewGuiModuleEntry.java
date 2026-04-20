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
 * Точный порт Panel.java module rendering.
 *
 * Panel.java:
 *   m.x = panelX + 1, m.y = moduleY, m.width = panelW-1, m.height = 18
 *
 *   bg:   m.x+6.5, m.y, m.width-12, moduleHeight-2.3, r=3.2, rgba(0,0,0,60)
 *   name: panelX+12, moduleY+5.7  (= y+off+offset+scroll+18.2 - y-off-offset-scroll-12.5 = +5.7)
 *         enabled=rgba(255,255,255,255), disabled=rgba(140,140,140,128)
 *   dots: m.x+m.width-10+2 with translate(-5) = panelX+1+(panelW-1)-10+2-5 = panelX+panelW-13
 *         y = moduleY+5
 *
 *   settings: object.x=panelX, object.y=moduleY+yd+off+offset+scroll+25
 *             relative to moduleY: +yd + (off+offset+scroll+25 - (off+offset+scroll+12.5)) = +yd+12.5
 *             yd starts at 6, so first setting y = moduleY + 6 + 12.5 = moduleY + 18.5
 *   off += (object.height + 9.5) * expand_anim
 */
public class NewGuiModuleEntry implements IMinecraft {

    // Panel.java: originalHeight = 18f
    private static final float ROW_H   = 18f;
    // Panel.java: object.height=8f, gap=9.5f
    private static final float OBJ_GAP = 9.5f;

    private final Module module;
    private final List<NewGuiSettingEntry> settings = new ArrayList<>();

    private boolean expanded  = false;
    private boolean isBinding = false;

    // Panel.java: expand_anim = AnimationMath.fast(anim, target, 20) ≈ lerp 0.15
    private float expandAnim = 0f;

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

    /** Panel.java: off += (object.height+9.5)*expand_anim per visible setting */
    public float getSettingsExpandedOff() {
        float h = 0;
        for (NewGuiSettingEntry s : settings)
            if (s.getSetting() == null || s.getSetting().isVisible())
                h += (s.getHeight() + OBJ_GAP) * expandAnim;
        return h;
    }

    public void render(UIContext ctx, float mx, float my,
                       float panelX, float moduleY, float panelW) {
        MatrixStack mat = ctx.getMatrices();

        // expand_anim lerp ≈ AnimationMath.fast(anim, target, 20)
        expandAnim += ((expanded ? 1f : 0f) - expandAnim) * 0.15f;

        boolean on = module.isEnabled();

        // Panel.java: m.x=panelX+1, m.width=panelW-1
        float mx0 = panelX + 1f;
        float mw  = panelW - 1f;

        // Panel.java: moduleHeight = expanded ? (height + totalSettingsH) : height
        float settingsH = getSettingsExpandedOff();
        float totalH    = ROW_H + settingsH;

        // Panel.java: drawRoundedRect(m.x+6.5, m.y, m.width-12, moduleHeight-2.3, 3.2, rgba(0,0,0,60))
        DrawUtil.drawRoundedRect(mat,
                mx0 + 6.5f, moduleY,
                mw - 12f, totalH - 2.3f,
                BorderRadius.all(3.2f),
                new ColorRGBA(0, 0, 0, 60));

        // Panel.java: name at panelX+12, moduleY+5.7
        // enabled=rgba(255,255,255,255), disabled=rgba(140,140,140,128)
        String name = isBinding ? "..." : module.getName();
        int textColor = on
                ? new ColorRGBA(255, 255, 255, 255).getRGB()
                : new ColorRGBA(140, 140, 140, 128).getRGB();
        // enabled=SEMIBOLD (жирный), disabled=REGULAR
        MsdfRenderer.renderText(on ? Fonts.SEMIBOLD : Fonts.REGULAR, name, 8f, textColor,
                mat.peek().getPositionMatrix(),
                panelX + 12f, moduleY + 5.7f, 0);

        // Panel.java: dots at m.x+m.width-10+2 (with translate -5) = panelX+panelW-13, y=moduleY+5
        if (!settings.isEmpty()) {
            int dotsColor = on
                    ? new ColorRGBA(255, 255, 255, 200).getRGB()
                    : new ColorRGBA(140, 140, 140, 128).getRGB();
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 7f, dotsColor,
                    mat.peek().getPositionMatrix(),
                    panelX + panelW - 13f, moduleY + 5f, 0);
        }

        // Settings
        if (expandAnim > 0.01f) {
            float sy  = moduleY + ROW_H;
            float vis = settingsH;
            ctx.enableScissor((int)panelX, (int)sy, (int)(panelX + panelW), (int)(sy + vis + 2f));

            // Panel.java: object.y = moduleY + yd + 12.5, yd starts at 6
            float yd = 6f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                float objY = moduleY + yd + 12.5f;
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
            else if (btn == MouseButton.RIGHT && !settings.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) isBinding = !isBinding;
        }

        if (expanded && expandAnim > 0.5f) {
            float yd = 6f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                float objY = moduleY + yd + 12.5f;
                se.onMouseClicked(mx, my, btn, panelX, objY, panelW);
                yd += se.getHeight() + OBJ_GAP;
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
