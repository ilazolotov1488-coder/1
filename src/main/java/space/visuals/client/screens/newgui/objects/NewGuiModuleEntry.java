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

    // Из Panel.java: originalHeight = 18f
    private static final float ROW_H = 18f;
    // Из Panel.java: object.height = 8f, gap = 9.5f
    private static final float SET_H_UNIT = 8f;
    private static final float SET_GAP    = 9.5f;

    private final Module module;
    private final List<NewGuiSettingEntry> settings = new ArrayList<>();

    private boolean expanded = false;
    private boolean isBinding = false;

    // expand_anim из Panel.java: AnimationMath.fast(anim, target, 20)
    private float expandAnim = 0f;

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
        if (expandAnim > 0.01f) h += getSettingsH() * expandAnim;
        return h;
    }

    private float getSettingsH() {
        float h = 0;
        for (NewGuiSettingEntry s : settings) {
            if (s.getSetting() == null || s.getSetting().isVisible()) {
                h += s.getHeight() + SET_GAP;
            }
        }
        return h;
    }

    public void render(UIContext ctx, float mx, float my, float panelX, float y, float panelW) {
        MatrixStack mat = ctx.getMatrices();

        // expand_anim: fast lerp к target
        float target = expanded ? 1f : 0f;
        expandAnim += (target - expandAnim) * 0.2f;

        lastX = panelX + 1f;
        lastY = y;
        lastW = panelW - 1f;

        boolean on = module.isEnabled();

        // Фон строки модуля — из Panel.java: rgba(0,0,0,60), radius=3.2, x+6.5, y, width-12, height-2.3
        float totalH = on && expanded ? (ROW_H + getSettingsH() * expandAnim) : ROW_H;
        DrawUtil.drawRoundedRect(mat,
                panelX + 6.5f, y,
                panelW - 12f, totalH - 2.3f,
                BorderRadius.all(3.2f),
                new ColorRGBA(0, 0, 0, 60));

        // Название — из Panel.java: Fonts[15], x+12, y+off+scroll+18.2 → относительно y: +6.2
        // включён: rgba(255,255,255,255), выключен: rgba(140,140,140,128)
        String name = isBinding ? "..." : module.getName();
        int textColor = on
                ? new ColorRGBA(255, 255, 255, 255).getRGB()
                : new ColorRGBA(140, 140, 140, 128).getRGB();

        MsdfRenderer.renderText(Fonts.REGULAR, name, 8f, textColor,
                mat.peek().getPositionMatrix(),
                panelX + 12f, y + 5.7f, 0);

        // "..." справа — из Panel.java: Fonts[12], x+width-10+2, y+5
        if (!settings.isEmpty()) {
            int dotsColor = on
                    ? new ColorRGBA(255, 255, 255, 200).getRGB()
                    : new ColorRGBA(140, 140, 140, 128).getRGB();
            float dotsX = panelX + panelW - 10f + 2f - 5f; // -5 компенсация translate
            MsdfRenderer.renderText(Fonts.REGULAR, "...", 7f, dotsColor,
                    mat.peek().getPositionMatrix(),
                    dotsX, y + 5f, 0);
        }

        // Настройки раскрытые
        if (expandAnim > 0.01f) {
            float settH = getSettingsH() * expandAnim;
            float sy = y + ROW_H;

            ctx.enableScissor((int)panelX, (int)sy, (int)(panelX + panelW), (int)(sy + settH + 2));
            float yd = 6f;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                // Из Panel.java: object.x=this.x, object.y=y+yd+off+scroll+25, object.width=this.width, object.height=8
                se.render(ctx, mx, my, panelX, sy + yd - ROW_H, panelW, expandAnim);
                yd += (se.getHeight() + SET_GAP) * expandAnim;
            }
            ctx.disableScissor();
        }
    }

    public void onMouseClicked(float mx, float my, MouseButton btn, float panelX, float y, float panelW) {
        lastX = panelX + 1f;
        lastY = y;
        lastW = panelW - 1f;

        // Из Panel.java: isInRegion(mx, my, m.x+8, m.y, m.width-20+4, m.height) && button==1 → expand
        boolean inRow = mx >= panelX + 8f && mx <= panelX + panelW - 16f
                && my >= y && my <= y + ROW_H;

        if (inRow) {
            if (btn == MouseButton.LEFT) module.toggle();
            else if (btn == MouseButton.RIGHT && !settings.isEmpty()) expanded = !expanded;
            else if (btn == MouseButton.MIDDLE) isBinding = !isBinding;
        }

        if (expanded && expandAnim > 0.5f) {
            float yd = 6f;
            float sy = y + ROW_H;
            for (NewGuiSettingEntry se : settings) {
                if (se.getSetting() != null && !se.getSetting().isVisible()) continue;
                se.onMouseClicked(mx, my, btn, panelX, sy + yd - ROW_H, panelW);
                yd += se.getHeight() + SET_GAP;
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
}
