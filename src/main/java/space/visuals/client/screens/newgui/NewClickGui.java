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

    // Window.java: width2=127, height2=282, step=width2-23=104
    // Panel.java: panel.x = x+73, panel.y = y+19, panel.width = width-20=107, panel.height = height-81=201
    // Итого: каждая панель занимает 104px по горизонтали, рисует rect шириной 107
    public static final float PANEL_STEP = 104f;  // offset += width2 - 23
    public static final float PANEL_W    = 107f;  // width - 20
    public static final float PANEL_H    = 282f;  // полная высота (height2)
    public static final float PANEL_INNER_H = 201f; // height - 81 (контентная зона)

    public static boolean searching = false;
    public static String searchText = "";

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
        searching = false;
        searchText = "";
        if (!initialized) { initialize(); initialized = true; }
        repositionPanels();
    }

    private void repositionPanels() {
        int n = panels.size();
        // Window.java: position.x = scaledWidth/2 - n*width2/2 - 30
        float startX = width / 2f - n * PANEL_STEP / 2f - 30f;
        float startY = height / 2f - PANEL_H / 2f;
        float offset = 10f; // Window.java: float offset = 10.0f
        for (int i = 0; i < n; i++) {
            // Panel.java: this.x = x + 73, this.y = y + 19
            panels.get(i).setPosition(startX + offset + 73f, startY + 19f);
            offset += PANEL_STEP;
        }
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        MatrixStack ms = ctx.getMatrices();

        for (NewGuiPanel panel : panels) {
            panel.render(ctx, mouseX, mouseY);
        }

        // Window.java: search box at sr.scaledWidth()/2-55, sr.scaledHeight()/1.17f-60, 110x15
        float sbX = width / 2f - 55f;
        float sbY = height / 1.17f - 60f;

        DrawUtil.drawRoundedRect(ms, sbX, sbY, 110f, 15f,
                BorderRadius.all(2f), new ColorRGBA(0, 0, 0, 200));

        // Текст поиска
        String display = (!searching && searchText.isEmpty())
                ? "Поиск"
                : searchText + (searching && System.currentTimeMillis() % 1000L > 500L ? "_" : "");
        float dw = Fonts.REGULAR.getWidth(display, 8f);
        MsdfRenderer.renderText(Fonts.REGULAR, display, 8f,
                new ColorRGBA(200, 200, 200, 200).getRGB(),
                ms.peek().getPositionMatrix(),
                sbX + 55f - dw / 2f, sbY + 3.5f, 0);

        // Подсказка: "Для активации поиска нажмите CTRL + F"
        String hint = "Для активации поиска нажмите CTRL + F";
        float hw = Fonts.REGULAR.getWidth(hint, 7.5f);
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 7.5f,
                new ColorRGBA(255, 255, 255, 220).getRGB(),
                ms.peek().getPositionMatrix(),
                width / 2f - hw / 2f + 1f, height / 1.17f - 38f, 0);
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
        float sbX = width / 2f - 55f, sbY = height / 1.17f - 55f;
        if (mx >= sbX && mx <= sbX + 110 && my >= sbY && my <= sbY + 13) {
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
        if (searching && searchText.length() < 13) searchText += c;
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Убираем стандартный Minecraft blur/dim — НЕ вызываем super
    }

    public String getSearchText() { return searching ? searchText : ""; }
    public boolean isSearching() { return searching; }
}
