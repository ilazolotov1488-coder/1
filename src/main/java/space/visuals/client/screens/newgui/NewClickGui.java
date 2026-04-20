package space.visuals.client.screens.newgui;

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

/**
 * Новый Click GUI. Открывается при режиме "Новый" в настройках модуля Menu.
 */
public class NewClickGui extends CustomScreen {

    private static final float PANEL_WIDTH = 115f;
    private static final float PANEL_HEIGHT = 270f;

    private boolean searching = false;
    private String searchText = "";

    private final Animation openAnimation = new Animation(200, 0f, Easing.CUBIC_IN_OUT);
    private boolean initialized = false;

    private final List<NewGuiPanel> panels = new ArrayList<>();

    // finish = true → Menu.render2d вызовет toggle() → GUI закроется
    private boolean finish = false;

    public NewClickGui() {}

    public void initialize() {
        panels.clear();
        for (Category cat : Category.values()) {
            if (cat == Category.PLAYER) continue; // empty category
            panels.add(new NewGuiPanel(cat));
        }
    }

    @Override
    protected void init() {
        finish = false;
        openAnimation.animateTo(1f);

        if (!initialized) {
            initialize();
            initialized = true;
        }

        repositionPanels();
    }

    private void repositionPanels() {
        float totalWidth = panels.size() * (PANEL_WIDTH - 10f) + 10f;
        float startX = (width - totalWidth) / 2f;
        float startY = (height - PANEL_HEIGHT) / 2f;
        for (int i = 0; i < panels.size(); i++) {
            panels.get(i).setPosition(startX + i * (PANEL_WIDTH - 10f), startY);
        }
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        openAnimation.update();
        float alpha = (float) openAnimation.getValue();

        MatrixStack matrices = ctx.getMatrices();

        for (NewGuiPanel panel : panels) {
            panel.render(ctx, mouseX, mouseY, alpha);
        }

        // Подсказка поиска (dart.ru style)
        String hint = searching
                ? searchText + (System.currentTimeMillis() % 1000L > 500L ? "_" : "")
                : "для поиска нажми CTRL+F";
        float hintW = Fonts.REGULAR.getWidth(hint, 8f);
        float hintX = width / 2f - hintW / 2f;
        float hintY = height - 20f;
        DrawUtil.drawRoundedRect(matrices, hintX - 8, hintY - 4, hintW + 16, 16,
                BorderRadius.all(4f), new ColorRGBA(0, 0, 0, (int)(200 * alpha)));
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 8f,
                new ColorRGBA(200, 200, 200, (int)(200 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), hintX, hintY, 0);
    }

    /** Вызывается из Menu.render2d — ничего дополнительного не нужно */
    public void renderTop(UIContext ctx, float mouseX, float mouseY) {}

    public boolean isFinish() {
        return finish;
    }

    @Override
    public void close() {
        // Minecraft вызывает это при закрытии экрана (ESC, mc.setScreen(null) и т.д.)
        // Выключаем модуль Menu
        space.visuals.client.modules.impl.render.Menu menu =
                space.visuals.client.modules.impl.render.Menu.INSTANCE;
        if (menu != null && menu.isEnabled()) {
            menu.setEnabled(false);
            menu.onDisable();
        }
        super.close();
    }

    // ── Ввод ──────────────────────────────────────────────────────────────────

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        for (NewGuiPanel panel : panels) {
            panel.onMouseClicked((float) mouseX, (float) mouseY, button);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (NewGuiPanel panel : panels) {
            panel.onMouseReleased((float) mouseX, (float) mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (NewGuiPanel panel : panels) {
            panel.onScroll((float) mouseX, (float) mouseY, (float) verticalAmount);
        }
        return true; // поглощаем событие
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_F) {
            searching = true;
            searchText = "";
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searching) {
                searching = false;
                searchText = "";
            } else {
                close();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searching && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && searching) {
            searching = false;
            return true;
        }
        for (NewGuiPanel panel : panels) {
            panel.onKeyPressed(keyCode, scanCode, modifiers);
        }
        return true; // поглощаем все клавиши — не пускаем в игру
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searching && searchText.length() < 20) {
            searchText += chr;
        }
        return true;
    }

    // Не ставим игру на паузу
    @Override
    public boolean shouldPause() {
        return false;
    }

    // Убираем стандартный Minecraft blur/dim фон
    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // ничего не рисуем — фон рисуют сами панели
    }

    public String getSearchText() {
        return searching ? searchText : "";
    }

    public boolean isSearching() {
        return searching;
    }
}
