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

    private static final float W          = NewClickGui.PANEL_W;
    private static final float H          = NewClickGui.PANEL_H;
    private static final float HEADER_H   = 26f;  // высота заголовка
    private static final float ROW_H      = 18f;  // высота строки модуля
    private static final float ROW_PAD    = 2f;   // отступ между строками

    // Цвета — точно как в референсе
    private static final ColorRGBA BG         = new ColorRGBA(18, 19, 25, 230);
    private static final ColorRGBA DIVIDER    = new ColorRGBA(50, 52, 65, 200);
    private static final ColorRGBA HDR_TEXT   = new ColorRGBA(255, 255, 255, 220);

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    private float x, y;
    private float scroll    = 0f;
    private float scrollOut = 0f;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> mods = new ArrayList<>();
        for (Module m : Zenith.getInstance().getModuleManager().getModules())
            if (m.getCategory() == category) mods.add(m);
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module m : mods) entries.add(new NewGuiModuleEntry(m));
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void init(float x, float y, float w, float h) { this.x = x; this.y = y; }

    public void render(UIContext ctx, float mx, float my) {
        MatrixStack ms = ctx.getMatrices();
        scrollOut += (scroll - scrollOut) * 0.2f;

        // Фон панели
        DrawUtil.drawRoundedRect(ms, x, y, W, H, BorderRadius.all(6f), BG);

        // Заголовок
        String cat = category.getName();
        float cw = Fonts.BOLD.getWidth(cat, 9.5f);
        MsdfRenderer.renderText(Fonts.BOLD, cat, 9.5f, HDR_TEXT.getRGB(),
                ms.peek().getPositionMatrix(),
                x + W / 2f - cw / 2f, y + 8f, 0);

        // Разделитель под заголовком
        DrawUtil.drawRect(ms, x + 8f, y + HEADER_H - 1f, W - 16f, 0.8f, DIVIDER);

        // Scissor — контент
        ctx.enableScissor((int)x, (int)(y + HEADER_H), (int)(x + W), (int)(y + H));

        float rowY = y + HEADER_H + 4f + scrollOut;
        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching) {
                if (!e.getModule().getName().toLowerCase()
                        .contains(NewClickGui.searchText.toLowerCase())) continue;
            }
            e.render(ctx, mx, my, x, rowY, W);
            rowY += e.getTotalHeight() + ROW_PAD;
        }

        ctx.disableScissor();

        // Граница
        DrawUtil.drawRoundedBorder(ms, x, y, W, H, 0.8f, BorderRadius.all(6f),
                new ColorRGBA(45, 47, 58, 180));

        // Clamp scroll
        float totalH = 0;
        for (NewGuiModuleEntry e : entries) totalH += e.getTotalHeight() + ROW_PAD;
        float contentH = H - HEADER_H - 8f;
        if (totalH <= contentH) scroll = 0f;
        else scroll = MathHelper.clamp(scroll, -(totalH - contentH), 0f);
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        if (mx < x || mx > x + W || my < y + HEADER_H || my > y + H) return;
        float rowY = y + HEADER_H + 4f + scrollOut;
        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching && !e.getModule().getName().toLowerCase()
                    .contains(NewClickGui.searchText.toLowerCase())) continue;
            e.onMouseClicked(mx, my, btn, x, rowY, W);
            rowY += e.getTotalHeight() + ROW_PAD;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        if (mx < x || mx > x + W || my < y || my > y + H) return;
        scroll += delta * 20f;
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    public float    getScroll()              { return scroll; }
    public void     setScroll(float s)       { scroll = s; scrollOut = s; }
    public Category getCategory()            { return category; }
}
