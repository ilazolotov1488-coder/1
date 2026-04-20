package space.visuals.client.screens.newgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.MsdfRenderer;
import space.visuals.client.modules.api.Category;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.game.other.render.CustomScreen;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NewClickGui extends CustomScreen {

    // Размеры панели — подобраны под референс
    public static final float PANEL_W = 130f;
    public static final float PANEL_H = 300f;

    public static boolean searching  = false;
    public static String  searchText = "";

    private boolean initialized = false;
    private final List<NewGuiPanel> panels = new ArrayList<>();
    private final HashMap<Category, Float> scrollStates = new HashMap<>();

    public NewClickGui() {}

    public void initialize() {
        panels.clear();
        for (Category cat : Category.values()) {
            if (cat == Category.PLAYER) continue;
            panels.add(new NewGuiPanel(cat));
        }
    }

    @Override
    protected void init() {
        searching  = false;
        searchText = "";
        if (!initialized) { initialize(); initialized = true; }
        repositionPanels();
        for (NewGuiPanel p : panels) {
            if (scrollStates.containsKey(p.getCategory()))
                p.setScroll(scrollStates.get(p.getCategory()));
        }
    }

    private void repositionPanels() {
        int n = panels.size();
        float totalW = n * PANEL_W;
        float startX = (width  - totalW) / 2f;
        float startY = (height - PANEL_H) / 2f;
        for (int i = 0; i < n; i++)
            panels.get(i).setPosition(startX + i * PANEL_W, startY);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();
        for (NewGuiPanel p : panels) p.render(ctx, mouseX, mouseY);

        // Поиск внизу по центру
        float sbX = width / 2f - 55f;
        float sbY = height / 1.17f - 60f;
        DrawUtil.drawRoundedRect(ms, sbX, sbY, 110f, 16f,
                BorderRadius.all(3f), new ColorRGBA(18, 19, 25, 220));

        String display = (!searching && searchText.isEmpty()) ? "Поиск"
                : searchText + (searching && System.currentTimeMillis() % 1000L > 500L ? "_" : "");
        float dw = Fonts.REGULAR.getWidth(display, 8f);
        MsdfRenderer.renderText(Fonts.REGULAR, display, 8f,
                new ColorRGBA(200, 200, 200, 200).getRGB(),
                ms.peek().getPositionMatrix(),
                sbX + 55f - dw / 2f, sbY + 4f, 0);

        String hint = "Для активации поиска нажмите CTRL + F";
        float hw = Fonts.REGULAR.getWidth(hint, 7.5f);
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 7.5f,
                new ColorRGBA(255, 255, 255, 180).getRGB(),
                ms.peek().getPositionMatrix(),
                width / 2f - hw / 2f, height / 1.17f - 38f, 0);
    }

    public void renderTop(UIContext ctx, float mx, float my) {}
    public boolean isFinish() { return false; }

    @Override
    public void close() {
        for (NewGuiPanel p : panels) scrollStates.put(p.getCategory(), p.getScroll());
        var menu = space.visuals.client.modules.impl.render.Menu.INSTANCE;
        if (menu != null && menu.isEnabled()) { menu.setEnabled(false); menu.onDisable(); }
        super.close();
    }

    @Override
    public void onMouseClicked(double mx, double my, MouseButton btn) {
        float sbX = width / 2f - 55f, sbY = height / 1.17f - 55f;
        if (mx >= sbX && mx <= sbX + 110 && my >= sbY && my <= sbY + 16) {
            searching = !searching; return;
        }
        for (NewGuiPanel p : panels) p.onMouseClicked((float)mx, (float)my, btn);
    }

    @Override
    public void onMouseReleased(double mx, double my, MouseButton btn) {
        for (NewGuiPanel p : panels) p.onMouseReleased((float)mx, (float)my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        for (NewGuiPanel p : panels) p.onScroll((float)mx, (float)my, (float)v);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
            searching = true; searchText = ""; return true;
        }
        for (NewGuiPanel p : panels) p.onKeyPressed(key, scan, mods);
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && searching && !searchText.isEmpty())
            searchText = searchText.substring(0, searchText.length() - 1);
        if (key == GLFW.GLFW_KEY_ENTER) { searchText = ""; searching = false; }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (searching && searchText.length() < 20) searchText += c;
        return true;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    public String  getSearchText() { return searching ? searchText : ""; }
    public boolean isSearching()   { return searching; }
}
