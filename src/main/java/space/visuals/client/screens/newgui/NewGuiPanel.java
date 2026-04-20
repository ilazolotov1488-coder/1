package space.visuals.client.screens.newgui;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import space.visuals.Zenith;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.screens.newgui.objects.NewGuiModuleEntry;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NewGuiPanel {

    // Panel.java: this.width = width-20 = 107, this.height = height-81 = 201
    private static final float W = NewClickGui.PANEL_W;       // 107
    private static final float H = NewClickGui.PANEL_INNER_H; // 201

    // Panel.java: заголовок рисуется на y+7, контент начинается с y+18 (off=11, offset=-4, +12.5 = 19.5)
    private static final float HEADER_H = 18f;
    // Panel.java: originalHeight = 18f
    private static final float ROW_H = 18f;

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    // x,y — уже с учётом смещения из Window (panel.x = x+73, panel.y = y+19)
    private float x, y;
    private float scrolling = 0f;
    private float scrollingOut = 0f;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> mods = new ArrayList<>();
        for (Module m : Zenith.getInstance().getModuleManager().getModules()) {
            if (m.getCategory() == category) mods.add(m);
        }
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module m : mods) entries.add(new NewGuiModuleEntry(m));
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        // Panel.java: scrollingOut = AnimationMath.fast(out, in, 20) → lerp ~0.15
        scrollingOut += (scrolling - scrollingOut) * 0.15f;

        // Panel.java: drawRoundedRect(x+5, y, width-8, height-1+4+31, 5, rgba(0,0,0,190))
        // x+5 = x+5, width-8 = 99, height-1+4+31 = 235
        DrawUtil.drawRoundedRect(ms,
                x + 5f, y,
                W - 8f, H - 1f + 4f + 31f,
                BorderRadius.all(5f),
                new ColorRGBA(0, 0, 0, 190));

        // Panel.java: Fonts[18].drawCenteredString(x+width/2, y+7, rgba(255,255,255,210))
        String catName = category.getName();
        float catW = Fonts.SEMIBOLD.getWidth(catName, 9f);
        MsdfRenderer.renderText(Fonts.SEMIBOLD, catName, 9f,
                new ColorRGBA(255, 255, 255, 210).getRGB(),
                ms.peek().getPositionMatrix(),
                x + W / 2f - catW / 2f, y + 7f, 0);

        // Scissor: x, y+18, width, height+12
        ctx.enableScissor((int)x, (int)(y + HEADER_H), (int)(x + W), (int)(y + H + 12f));

        // Panel.java: offset=-4, off=11 → первый модуль y = y + off + offset + scroll + 12.5 = y+19.5+scroll
        float offset = -4f;
        float off = 11f;

        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching) {
                if (!e.getModule().getName().toLowerCase().contains(NewClickGui.searchText.toLowerCase()))
                    continue;
            }

            float moduleY = y + off + offset + scrollingOut + 12.5f;

            e.render(ctx, mouseX, mouseY, x, moduleY, W);

            // Panel.java: off += (object.height + 9.5) * expand_anim для каждого setting
            // затем off += offset + 20 = -4 + 20 = 16 за модуль
            off += e.getSettingsExpandedHeight() + offset + 20f;
        }

        ctx.disableScissor();

        // Clamp scroll
        float max2 = off - 37f;
        if (max2 < H - 6f) scrolling = 0f;
        else scrolling = MathHelper.clamp(scrolling, -(max2 - (H - 16f)), 0f);
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        // Panel.java: зона x, y+18, width, height+12
        if (!isInRegion(mx, my, x, y + HEADER_H, W, H + 12f)) return;

        float offset = -4f;
        float off = 15f;
        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching && !e.getModule().getName().toLowerCase()
                    .contains(NewClickGui.searchText.toLowerCase())) continue;

            float moduleY = y + off + offset + scrollingOut + 12.5f;
            e.onMouseClicked(mx, my, btn, x, moduleY, W);

            if (e.isExpanded()) {
                float yd = 5f;
                for (var se : e.getSettingEntries()) {
                    if (se.getSetting() == null || !se.getSetting().isVisible()) continue;
                    yd += se.getHeight() + 5f;
                }
            }
            off += e.getSettingsExpandedHeight() + offset + 20f;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        // Panel.java: isInRegion(x, y, width, height+32)
        if (!isInRegion(mx, my, x, y, W, H + 32f)) return;
        scrolling += delta * 25f;
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    private boolean isInRegion(float mx, float my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }
}
