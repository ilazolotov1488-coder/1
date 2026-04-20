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

/**
 * Точный порт Panel.java без dart.ru-специфичных смещений.
 *
 * Panel.java render():
 *   bg:     x+5, y,  width-8, height+34,  r=5, rgba(0,0,0,190)
 *   header: centered at x+width/2, y+7,   rgba(255,255,255,210)
 *   scissor: x, y+18, width, height+12
 *   offset=-4, off=11
 *   moduleY = y + off + offset + scrollOut + 12.5
 *   per module: off += settingsExpandedH + offset + 20  (= settingsH + 16)
 */
public class NewGuiPanel {

    // Размеры панели — как в Window.java: width2=127, height2=282
    // Panel: width=107 (127-20), height=201 (282-81)
    // Rect: width-8=99, height+34=235
    private static final float PW = 107f;  // panel width
    private static final float PH = 201f;  // panel height (content zone)

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    private float x, y;
    private float scrolling    = 0f;
    private float scrollingOut = 0f;
    private float savedScrolling = 0f;
    private boolean shouldRestoreScroll = false;

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

    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        if (shouldRestoreScroll) {
            scrolling = savedScrolling;
            scrollingOut = savedScrolling;
            shouldRestoreScroll = false;
        }

        // Panel.java: scrollingOut = AnimationMath.fast(out, in, 20) ≈ lerp 0.15
        scrollingOut += (scrolling - scrollingOut) * 0.15f;

        // Panel.java: drawRoundedRect(x+5, y, width-8, height+34, 5, rgba(0,0,0,190))
        DrawUtil.drawRoundedRect(ms,
                x + 5f, y,
                PW - 8f, PH + 34f,
                BorderRadius.all(5f),
                new ColorRGBA(0, 0, 0, 190));

        // Panel.java: header centered at x+width/2, y+7, rgba(255,255,255,210)
        String catName = category.getName();
        float catW = Fonts.SEMIBOLD.getWidth(catName, 9f);
        MsdfRenderer.renderText(Fonts.SEMIBOLD, catName, 9f,
                new ColorRGBA(255, 255, 255, 210).getRGB(),
                ms.peek().getPositionMatrix(),
                x + PW / 2f - catW / 2f, y + 7f, 0);

        // Panel.java: scissor x, y+18, width, height+12
        ctx.enableScissor((int)x, (int)(y + 18), (int)(x + PW), (int)(y + PH + 12f));

        // Panel.java: offset=-4, off=11
        float offset = -4f;
        float off    = 11f;

        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching) {
                if (!e.getModule().getName().toLowerCase()
                        .contains(NewClickGui.searchText.toLowerCase())) continue;
            }

            // Panel.java: moduleY = y + off + offset + scrollingOut + 12.5
            float moduleY = y + off + offset + scrollingOut + 12.5f;
            e.render(ctx, mouseX, mouseY, x, moduleY, PW);

            // Panel.java: off += settingsExpandedH + offset + 20
            off += e.getSettingsExpandedOff() + offset + 20f;
        }

        ctx.disableScissor();

        // Panel.java: clamp scroll
        float max2 = off - 37f;
        if (max2 < PH - 6f) scrolling = 0f;
        else scrolling = MathHelper.clamp(scrolling, -(max2 - (PH - 16f)), 0f);
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        // Panel.java: zone x, y+18, width, height+12
        if (mx < x || mx > x + PW || my < y + 18f || my > y + PH + 12f) return;

        float offset = -2f;
        float off    = 15f;
        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching && !e.getModule().getName().toLowerCase()
                    .contains(NewClickGui.searchText.toLowerCase())) continue;
            float moduleY = y + off + offset + scrollingOut + 12.5f;
            e.onMouseClicked(mx, my, btn, x, moduleY, PW);
            off += e.getSettingsExpandedOff() + offset + 20f;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        // Panel.java: zone x, y, width, height+32
        if (mx < x || mx > x + PW || my < y || my > y + PH + 32f) return;
        scrolling += delta * 25f;
        savedScrolling = scrolling;
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    public float    getScroll()        { return scrolling; }
    public void     setScroll(float s) { scrolling = s; scrollingOut = s; savedScrolling = s; }
    public Category getCategory()      { return category; }
}
