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
import java.util.List;

public class NewClickGui extends CustomScreen {

    // Точные размеры из Panel.java референса
    public static final float PANEL_W  = 107f; // width - 20 (127-20)
    public static final float PANEL_H  = 201f; // height - 81 (282-81)
    public static final float PANEL_GAP = 0f;  // панели вплотную

    private boolean searching = false;
    private String searchText = "";
    private boolean initialized = false;
    private final List<NewGuiPanel> panels = new ArrayList<>();

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
        if (!initialized) { initialize(); initialized = true; }
        repositionPanels();
    }

    private void repositionPanels() {
        int n = panels.size();
        float totalW = n * PANEL_W;
        float startX = (width - totalW) / 2f;
        float startY = (height - PANEL_H) / 2f - 20f; // чуть выше центра как в референсе
        for (int i = 0; i < n; i++) {
            panels.get(i).setPosition(startX + i * PANEL_W, startY);
        }
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        for (NewGuiPanel panel : panels) {
            panel.render(ctx, mouseX, mouseY);
        }

        // Поиск — точно как в Window.java: позиция width/2-55, height/1.17f-60, размер 110x15
        float searchBoxX = width / 2f - 55f;
        float searchBoxY = height / 1.17f - 60f;
        float searchBoxW  = 110f;
        float searchBoxH  = 15f;

        DrawUtil.drawRoundedRect(ms, searchBoxX, searchBoxY, searchBoxW, searchBoxH,
                BorderRadius.all(2f), new ColorRGBA(0, 0, 0, 200));

        // Текст в поле поиска
        String searchDisplay = searching || !searchText.isEmpty()
                ? searchText + (searching && System.currentTimeMillis() % 1000L > 500L ? "_" : "")
                : "Поиск";
        float sdW = Fonts.REGULAR.getWidth(searchDisplay, 8f);
        MsdfRenderer.renderText(Fonts.REGULAR, searchDisplay, 8f,
                new ColorRGBA(200, 200, 200, 200).getRGB(),
                ms.peek().getPositionMatrix(),
                searchBoxX + searchBoxW / 2f - sdW / 2f,
                searchBoxY + searchBoxH / 2f - 4f, 0);

        // Подсказка под полем — как в Window.java
        String hint = "Для активации поиска нажмите CTRL + F";
        float hintW = Fonts.REGULAR.getWidth(hint, 7.5f);
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 7.5f,
                new ColorRGBA(255, 255, 255, 220).getRGB(),
                ms.peek().getPositionMatrix(),
                width / 2f - hintW / 2f + 1f,
                height / 1.17f - 38f, 0);
    }

    public void renderTop(UIContext ctx, float mouseX, float mouseY) {}
    public boolean isFinish() { return false; }

    @Override
    public void close() {
        space.visuals.client.modules.impl.render.Menu menu =
                space.visuals.client.modules.impl.render.Menu.INSTANCE;
        if (menu != null && menu.isEnabled()) {
            menu.setEnabled(false);
            menu.onDisable();
        }
        super.close();
    }

    @Override
    public void onMouseClicked(double mx, double my, MouseButton btn) {
        // Клик по полю поиска
        float sx = width / 2f - 55f, sy = height / 1.17f - 55f;
        if (mx >= sx && mx <= sx + 110 && my >= sy && my <= sy + 13) {
            searching = !searching;
            return;
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
        if (key == 341 || key == 345) { /* ctrl */ }
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
            searching = true; searchText = ""; return true;
        }
        for (NewGuiPanel p : panels) p.onKeyPressed(key, scan, mods);
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && searching && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
        }
        if (key == GLFW.GLFW_KEY_ENTER) { searchText = ""; searching = false; }
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (searching && searchText.length() < 13) searchText += c;
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Убираем стандартный Minecraft blur/dim
    }

    public String getSearchText() { return searching ? searchText : ""; }
    public boolean isSearching() { return searching; }
}
