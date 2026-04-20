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

    private static final float W = NewClickGui.PANEL_W;
    private static final float H = NewClickGui.PANEL_H;
    private static final float HEADER_H = 30f;
    private static final float ROW_H = 22f;
    private static final float ROW_PAD = 0f;

    // Цвета как в референсе
    private static final ColorRGBA BG       = new ColorRGBA(18, 19, 24, 230);
    private static final ColorRGBA HEADER_BG= new ColorRGBA(22, 23, 30, 255);
    private static final ColorRGBA DIVIDER  = new ColorRGBA(45, 47, 58, 255);
    private static final ColorRGBA BORDER   = new ColorRGBA(40, 42, 52, 255);

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    private float x, y;
    private float scroll = 0f;
    private float scrollSmooth = 0f;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> mods = new ArrayList<>();
        for (Module m : Zenith.getInstance().getModuleManager().getModules()) {
            if (m.getCategory() == category) mods.add(m);
        }
        // Сортировка по алфавиту как в референсе
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module m : mods) entries.add(new NewGuiModuleEntry(m));
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        MatrixStack ms = ctx.getMatrices();
        scrollSmooth += (scroll - scrollSmooth) * 0.25f;

        // Фон панели
        DrawUtil.drawRoundedRect(ms, x, y, W, H, BorderRadius.all(6f),
                new ColorRGBA(BG.getRed(), BG.getGreen(), BG.getBlue(), (int)(BG.getAlpha() * alpha)));

        // Заголовок
        DrawUtil.drawRoundedRect(ms, x, y, W, HEADER_H,
                BorderRadius.top(6f, 6f),
                new ColorRGBA(HEADER_BG.getRed(), HEADER_BG.getGreen(), HEADER_BG.getBlue(), (int)(HEADER_BG.getAlpha() * alpha)));

        String catName = category.getName();
        float catW = Fonts.SEMIBOLD.getWidth(catName, 9f);
        MsdfRenderer.renderText(Fonts.SEMIBOLD, catName, 9f,
                new ColorRGBA(220, 222, 230, (int)(240 * alpha)).getRGB(),
                ms.peek().getPositionMatrix(),
                x + W / 2f - catW / 2f, y + HEADER_H / 2f - 4.5f, 0);

        // Разделитель
        DrawUtil.drawRect(ms, x + 8f, y + HEADER_H, W - 16f, 0.8f,
                new ColorRGBA(DIVIDER.getRed(), DIVIDER.getGreen(), DIVIDER.getBlue(), (int)(DIVIDER.getAlpha() * alpha)));

        // Контент со scissor
        ctx.enableScissor((int)x, (int)(y + HEADER_H + 1), (int)(x + W), (int)(y + H));

        float offsetY = y + HEADER_H + 4f + scrollSmooth;
        for (NewGuiModuleEntry e : entries) {
            // Поиск
            NewClickGui gui = getGui();
            if (gui != null && gui.isSearching()) {
                String q = gui.getSearchText().toLowerCase();
                if (!q.isEmpty() && !e.getModule().getName().toLowerCase().contains(q)) continue;
            }
            e.render(ctx, mouseX, mouseY, x, offsetY, W, alpha);
            offsetY += e.getTotalHeight() + ROW_PAD;
        }

        ctx.disableScissor();

        // Граница панели
        DrawUtil.drawRoundedBorder(ms, x, y, W, H, 0.8f, BorderRadius.all(6f),
                new ColorRGBA(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), (int)(BORDER.getAlpha() * alpha)));
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        if (mx < x || mx > x + W || my < y + HEADER_H || my > y + H) return;
        float offsetY = y + HEADER_H + 4f + scrollSmooth;
        for (NewGuiModuleEntry e : entries) {
            e.onMouseClicked(mx, my, btn, x, offsetY, W);
            offsetY += e.getTotalHeight() + ROW_PAD;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        if (mx < x || mx > x + W || my < y || my > y + H) return;
        scroll += delta * 20f;
        clampScroll();
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    private void clampScroll() {
        float total = 0;
        for (NewGuiModuleEntry e : entries) total += e.getTotalHeight() + ROW_PAD;
        float maxScroll = Math.max(0, total - (H - HEADER_H - 8f));
        scroll = MathHelper.clamp(scroll, -maxScroll, 0f);
    }

    private NewClickGui getGui() {
        try {
            return space.visuals.client.modules.impl.render.Menu.INSTANCE.getNewClickGui();
        } catch (Exception e) { return null; }
    }
}
