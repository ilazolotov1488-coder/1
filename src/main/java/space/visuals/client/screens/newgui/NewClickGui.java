package space.visuals.client.screens.newgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
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

    // Размеры панели — как в референсе
    public static final float PANEL_W = 160f;
    public static final float PANEL_H = 340f;
    public static final float PANEL_GAP = 4f;

    private boolean searching = false;
    private String searchText = "";

    private final Animation openAnim = new Animation(180, 0f, Easing.CUBIC_IN_OUT);
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
        openAnim.animateTo(1f);
        if (!initialized) {
            initialize();
            initialized = true;
        }
        repositionPanels();
    }

    private void repositionPanels() {
        float totalW = panels.size() * PANEL_W + (panels.size() - 1) * PANEL_GAP;
        float startX = (width - totalW) / 2f;
        float startY = (height - PANEL_H) / 2f;
        for (int i = 0; i < panels.size(); i++) {
            panels.get(i).setPosition(startX + i * (PANEL_W + PANEL_GAP), startY);
        }
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        openAnim.update();
        float alpha = (float) openAnim.getValue();
        MatrixStack ms = ctx.getMatrices();

        for (NewGuiPanel panel : panels) {
            panel.render(ctx, mouseX, mouseY, alpha);
        }

        // Поиск внизу — как в референсе
        String searchLabel = searching
                ? searchText + (System.currentTimeMillis() % 1000L > 500L ? "|" : "")
                : "Поиск";
        String hint = "Для активации поиска нажмите CTRL+F";

        float boxW = 120f;
        float boxH = 22f;
        float boxX = width / 2f - boxW / 2f;
        float boxY = height - 60f;

        // Фон поля поиска
        DrawUtil.drawRoundedRect(ms, boxX, boxY, boxW, boxH,
                BorderRadius.all(4f), new ColorRGBA(30, 30, 38, (int)(220 * alpha)));
        DrawUtil.drawRoundedBorder(ms, boxX, boxY, boxW, boxH,
                0.8f, BorderRadius.all(4f), new ColorRGBA(60, 62, 75, (int)(180 * alpha)));

        float labelW = Fonts.REGULAR.getWidth(searchLabel, 9f);
        MsdfRenderer.renderText(Fonts.REGULAR, searchLabel, 9f,
                new ColorRGBA(200, 200, 210, (int)(220 * alpha)).getRGB(),
                ms.peek().getPositionMatrix(),
                boxX + boxW / 2f - labelW / 2f, boxY + boxH / 2f - 4.5f, 0);

        // Подсказка под полем
        float hintW = Fonts.REGULAR.getWidth(hint, 7.5f);
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 7.5f,
                new ColorRGBA(120, 120, 130, (int)(160 * alpha)).getRGB(),
                ms.peek().getPositionMatrix(),
                width / 2f - hintW / 2f, boxY + boxH + 5f, 0);
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
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        for (NewGuiPanel p : panels) p.onMouseClicked((float)mouseX, (float)mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (NewGuiPanel p : panels) p.onMouseReleased((float)mouseX, (float)mouseY, button);
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
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searching) { searching = false; searchText = ""; }
            else close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && searching && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1); return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER && searching) { searching = false; return true; }
        for (NewGuiPanel p : panels) p.onKeyPressed(key, scan, mods);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (searching && searchText.length() < 20) searchText += c;
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    public String getSearchText() { return searching ? searchText : ""; }
    public boolean isSearching() { return searching; }
}
