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

    // Точные размеры из Panel.java: width-20=107, height-81=201
    private static final float W = NewClickGui.PANEL_W;
    private static final float H = NewClickGui.PANEL_H;

    // Из Panel.java: y+19f — заголовок, контент начинается с y+18
    private static final float HEADER_H = 19f;
    // Высота строки модуля из Panel.java: originalHeight = 18f
    private static final float ROW_H = 18f;

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    private float x, y;
    private float scrolling = 0f;
    private float scrollingOut = 0f;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> mods = new ArrayList<>();
        for (Module m : Zenith.getInstance().getModuleManager().getModules()) {
            if (m.getCategory() == category) mods.add(m);
        }
        // Сортировка по алфавиту как в Panel.java
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module m : mods) entries.add(new NewGuiModuleEntry(m));
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        // Плавный скролл как в Panel.java: AnimationMath.fast(out, in, 20)
        scrollingOut += (scrolling - scrollingOut) * 0.15f;

        // Фон панели — точно из Panel.java: rgba(0,0,0,190), radius=5
        DrawUtil.drawRoundedRect(ms, x, y, W, H,
                BorderRadius.all(5f), new ColorRGBA(0, 0, 0, 190));

        // Заголовок категории — Fonts[18], rgba(255,255,255,210), по центру, y+7
        String catName = category.getName();
        float catW = Fonts.SEMIBOLD.getWidth(catName, 9f);
        MsdfRenderer.renderText(Fonts.SEMIBOLD, catName, 9f,
                new ColorRGBA(255, 255, 255, 210).getRGB(),
                ms.peek().getPositionMatrix(),
                x + W / 2f - catW / 2f, y + 7f, 0);

        // Scissor — контент начинается с y+18 как в Panel.java
        ctx.enableScissor((int)x, (int)(y + HEADER_H), (int)(x + W), (int)(y + H));

        float off = HEADER_H - 6.5f; // offset = -4f, off = 11f → y + off + offset + scroll + 12.5
        for (NewGuiModuleEntry e : entries) {
            // Поиск
            NewClickGui gui = getGui();
            if (gui != null && gui.isSearching()) {
                String q = gui.getSearchText().toLowerCase();
                if (!q.isEmpty() && !e.getModule().getName().toLowerCase().contains(q)) {
                    continue;
                }
            }

            float moduleY = y + off + scrollingOut;
            e.render(ctx, mouseX, mouseY, x, moduleY, W);
            off += e.getTotalHeight() + 2f; // offset(-4) + 20 + expand = ~16 per module
        }

        ctx.disableScissor();
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        // Зона клика: x, y+18, width, height+12 — из Panel.java
        if (mx < x || mx > x + W || my < y + HEADER_H || my > y + H + 12) return;

        float off = HEADER_H - 6.5f;
        for (NewGuiModuleEntry e : entries) {
            float moduleY = y + off + scrollingOut;
            e.onMouseClicked(mx, my, btn, x, moduleY, W);
            off += e.getTotalHeight() + 2f;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        if (mx < x || mx > x + W || my < y || my > y + H + 32) return;
        scrolling += delta * 25f;
        clampScroll();
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    private void clampScroll() {
        float off = HEADER_H - 6.5f;
        for (NewGuiModuleEntry e : entries) off += e.getTotalHeight() + 2f;
        float max2 = off - 37f;
        if (max2 < H - 6f) { scrolling = 0f; return; }
        scrolling = MathHelper.clamp(scrolling, -(max2 - (H - 16f)), 0f);
    }

    private NewClickGui getGui() {
        try { return space.visuals.client.modules.impl.render.Menu.INSTANCE.getNewClickGui(); }
        catch (Exception e) { return null; }
    }
}
