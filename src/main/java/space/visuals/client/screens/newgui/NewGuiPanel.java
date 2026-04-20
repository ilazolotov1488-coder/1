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
 * 1:1 копия Panel.java из dart.ru clickgui.
 *
 * Panel constructor:
 *   this.x = passedX + 73
 *   this.y = passedY + 19
 *   this.width  = passedWidth  - 20  = 107
 *   this.height = passedHeight - 81  = 201
 *
 * Panel.render():
 *   bg: x+5, y, width-8=99, height-1+4+31=235, radius=5, rgba(0,0,0,190)
 *   header: centered at x+width/2, y+7, rgba(255,255,255,210)
 *   scissor: x, y+18, width, height+12
 *   offset=-4, off=11
 *   moduleY = y + off + offset + scrollingOut + 12.5
 *   off += (settingHeight + 9.5) * expandAnim  [per setting]
 *   off += offset + 20  [per module = 16]
 */
public class NewGuiPanel {

    // После конструктора Panel
    private float x, y;       // = passedX+73, passedY+19
    private float width;      // = passedWidth-20  = 107
    private float height;     // = passedHeight-81 = 201

    private final Category category;
    private final List<NewGuiModuleEntry> entries = new ArrayList<>();

    private float scrolling    = 0f;
    private float scrollingOut = 0f;

    // Panel.java: scroll persistence
    private float savedScrolling = 0f;
    private boolean shouldRestoreScroll = false;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> mods = new ArrayList<>();
        for (Module m : Zenith.getInstance().getModuleManager().getModules()) {
            if (m.getCategory() == category) mods.add(m);
        }
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module m : mods) entries.add(new NewGuiModuleEntry(m));
    }

    /** Window передаёт (position.x + offset, position.y, width2, height2) */
    public void init(float passedX, float passedY, float passedW, float passedH) {
        this.x      = passedX + 73f;
        this.y      = passedY + 19f;
        this.width  = passedW - 20f;   // 107
        this.height = passedH - 81f;   // 201
    }

    // Для совместимости
    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        // Panel.java: restore scroll on first open
        if (shouldRestoreScroll) {
            scrolling = savedScrolling;
            scrollingOut = savedScrolling;
            shouldRestoreScroll = false;
        }

        // Panel.java: scrollingOut = AnimationMath.fast(out, in, 20) ≈ lerp 0.15
        scrollingOut += (scrolling - scrollingOut) * 0.15f;

        // Panel.java: drawRoundedRect(x+5, y, width-8, height-1+4+31, 5, rgba(0,0,0,190))
        // = x+5, y, 99, 235, r=5
        DrawUtil.drawRoundedRect(ms,
                x + 5f, y,
                width - 8f, height - 1f + 4f + 31f,
                BorderRadius.all(6f),
                new ColorRGBA(18, 19, 25, 220));

        // Panel.java: Fonts[18].drawCenteredString(typeList.name(), x+width/2, y+7, rgba(255,255,255,210))
        String catName = category.getName();
        float catW = Fonts.BOLD.getWidth(catName, 10f);
        MsdfRenderer.renderText(Fonts.BOLD, catName, 10f,
                new ColorRGBA(255, 255, 255, 220).getRGB(),
                ms.peek().getPositionMatrix(),
                x + width / 2f - catW / 2f, y + 7f, 0);

        // Panel.java: scissor x, y+18, width, height+12
        ctx.enableScissor((int)x, (int)(y + 18), (int)(x + width), (int)(y + height + 12f));

        // Panel.java: offset=-4, off=11
        float offset = -4f;
        float off    = 11f;

        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching) {
                if (!e.getModule().getName().toLowerCase()
                        .contains(NewClickGui.searchText.toLowerCase())) continue;
            }

            // Panel.java: m.x = this.x+1, m.y = y+off+offset+scrollingOut+12.5
            float moduleY = y + off + offset + scrollingOut + 12.5f;

            e.render(ctx, mouseX, mouseY, x, moduleY, width);

            // Panel.java: off += (object.height+9.5)*expand_anim  [per setting]
            //             off += offset + 20  [per module]
            off += e.getSettingsExpandedOff() + offset + 20f;
        }

        ctx.disableScissor();

        // Panel.java: clamp scroll
        float max2 = off - 37f;
        if (max2 < height - 6f) scrolling = 0f;
        else scrolling = MathHelper.clamp(scrolling, -(max2 - (height - 16f)), 0f);
    }

    public void onMouseClicked(float mx, float my, MouseButton btn) {
        // Panel.java: зона x, y+18, width, height+12
        if (!inRegion(mx, my, x, y + 18f, width, height + 12f)) return;

        float offset = -2f;
        float off    = 15f;
        for (NewGuiModuleEntry e : entries) {
            if (NewClickGui.searching && !e.getModule().getName().toLowerCase()
                    .contains(NewClickGui.searchText.toLowerCase())) continue;

            float moduleY = y + off + offset + scrollingOut + 12.5f;
            e.onMouseClicked(mx, my, btn, x, moduleY, width);
            off += e.getSettingsExpandedOff() + offset + 20f;
        }
    }

    public void onMouseReleased(float mx, float my, MouseButton btn) {
        for (NewGuiModuleEntry e : entries) e.onMouseReleased(mx, my, btn);
    }

    public void onScroll(float mx, float my, float delta) {
        // Panel.java: isInRegion(x, y, width, height+32)
        if (!inRegion(mx, my, x, y, width, height + 32f)) return;
        scrolling += delta * 25f;
        savedScrolling = scrolling;
    }

    public float getScroll() { return scrolling; }

    public void setScroll(float scroll) {
        this.scrolling = scroll;
        this.savedScrolling = scroll;
    }

    public void saveScrollState() {
        this.shouldRestoreScroll = true;
        this.savedScrolling = this.scrolling;
    }

    public void loadScrollState(float scrollPosition) {
        this.savedScrolling = scrollPosition;
        this.shouldRestoreScroll = true;
    }

    public void onKeyPressed(int key, int scan, int mods) {
        for (NewGuiModuleEntry e : entries) e.onKeyPressed(key, scan, mods);
    }

    private boolean inRegion(float mx, float my, float rx, float ry, float rw, float rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    public Category getCategory() { return category; }
}
