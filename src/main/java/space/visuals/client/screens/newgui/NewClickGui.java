package space.visuals.client.screens.newgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;
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
 * Новый Click GUI в стиле dart.ru, адаптированный под ZENITH/Fabric API.
 * Открывается при выборе режима "Новый" в настройках модуля Menu.
 */
public class NewClickGui extends CustomScreen {

    // Размеры окна
    private static final float PANEL_WIDTH = 115f;
    private static final float PANEL_HEIGHT = 270f;
    private static final float HEADER_HEIGHT = 18f;

    // Цвета
    private static final ColorRGBA BG = new ColorRGBA(14, 15, 20, 210);
    private static final ColorRGBA MODULE_BG = new ColorRGBA(20, 21, 28, 180);
    private static final ColorRGBA MODULE_BG_HOVER = new ColorRGBA(30, 32, 42, 200);
    private static final ColorRGBA TEXT_ACTIVE = new ColorRGBA(255, 255, 255, 255);
    private static final ColorRGBA TEXT_INACTIVE = new ColorRGBA(130, 130, 140, 255);
    private static final ColorRGBA ACCENT = new ColorRGBA(100, 180, 255, 255);
    private static final ColorRGBA SEPARATOR = new ColorRGBA(40, 42, 55, 255);

    // Поиск
    private boolean searching = false;
    private String searchText = "";

    // Анимация открытия
    private final Animation openAnimation = new Animation(200, 0f, Easing.CUBIC_IN_OUT);
    private boolean initialized = false;

    // Панели по категориям
    private final List<NewGuiPanel> panels = new ArrayList<>();

    // Флаг закрытия
    private boolean closing = false;
    private boolean finish = false;

    public NewClickGui() {}

    public void initialize() {
        panels.clear();
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            panels.add(new NewGuiPanel(categories[i]));
        }
    }

    @Override
    protected void init() {
        closing = false;
        finish = false;
        openAnimation.animateTo(1f);

        if (!initialized) {
            initialize();
            initialized = true;
        }

        // Позиционируем панели
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

        if (closing) {
            openAnimation.animateTo(0f);
            if (openAnimation.isDone()) {
                finish = true;
            }
        }

        MatrixStack matrices = ctx.getMatrices();

        for (NewGuiPanel panel : panels) {
            panel.render(ctx, mouseX, mouseY, alpha);
        }

        // Подсказка поиска
        float hintY = height / 1.17f - 38f;
        String hint = searching
                ? "Поиск: " + searchText + (System.currentTimeMillis() % 1000L > 500L ? "_" : "")
                : "CTRL+F для поиска";
        float hintW = Fonts.REGULAR.getWidth(hint, 8f);
        float hintX = width / 2f - hintW / 2f;
        DrawUtil.drawRoundedRect(matrices, hintX - 6, hintY - 3, hintW + 12, 14, BorderRadius.all(3f), new ColorRGBA(0, 0, 0, (int)(160 * alpha)));
        MsdfRenderer.renderText(Fonts.REGULAR, hint, 8f,
                new ColorRGBA(200, 200, 200, (int)(200 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(), hintX, hintY, 0);
    }

    public void renderTop(UIContext ctx, float mouseX, float mouseY) {
        // ничего дополнительного поверх
    }

    public boolean isFinish() {
        return finish;
    }

    public void startClose() {
        closing = true;
    }

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
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return true;
        }
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
                startClose();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searching && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            searching = false;
            return true;
        }
        for (NewGuiPanel panel : panels) {
            panel.onKeyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searching && searchText.length() < 20) {
            searchText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public String getSearchText() {
        return searching ? searchText : "";
    }

    public boolean isSearching() {
        return searching;
    }
}
