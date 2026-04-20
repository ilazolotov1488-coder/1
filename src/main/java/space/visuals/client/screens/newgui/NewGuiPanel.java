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
 * Панель одной категории в новом Click GUI (dart.ru style).
 */
public class NewGuiPanel {

    private static final float PANEL_WIDTH = 127f;
    private static final float PANEL_HEIGHT = 282f;
    private static final float HEADER_HEIGHT = 20f;
    private static final float MODULE_HEIGHT = 20f;
    private static final float MODULE_PADDING = 2f;

    private static final ColorRGBA BG = new ColorRGBA(0, 0, 0, 190);
    private static final ColorRGBA HEADER_TEXT = new ColorRGBA(255, 255, 255, 255);

    private final Category category;
    private final List<NewGuiModuleEntry> moduleEntries = new ArrayList<>();

    private float x, y;
    private float scrollOffset = 0f;
    private float scrollOffsetSmooth = 0f;

    public NewGuiPanel(Category category) {
        this.category = category;
        List<Module> sorted = new ArrayList<>();
        for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == category) {
                sorted.add(module);
            }
        }
        sorted.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        for (Module module : sorted) {
            moduleEntries.add(new NewGuiModuleEntry(module));
        }
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        MatrixStack matrices = ctx.getMatrices();

        // Плавный скролл
        scrollOffsetSmooth += (scrollOffset - scrollOffsetSmooth) * 0.2f;

        // Фон панели (dart.ru: rgba(0,0,0,190) с gaussian blur)
        DrawUtil.drawRoundedRect(matrices, x, y, PANEL_WIDTH, PANEL_HEIGHT,
                BorderRadius.all(5f), new ColorRGBA(0, 0, 0, (int)(190 * alpha)));

        // Заголовок — название категории по центру, белый текст size 9f
        String catName = category.getName();
        float catTextWidth = Fonts.REGULAR.getWidth(catName, 9f);
        MsdfRenderer.renderText(Fonts.REGULAR, catName, 9f,
                new ColorRGBA(255, 255, 255, (int)(210 * alpha)).getRGB(),
                matrices.peek().getPositionMatrix(),
                x + PANEL_WIDTH / 2f - catTextWidth / 2f,
                y + HEADER_HEIGHT / 2f - 4.5f,
                0);

        // Scissor — обрезаем контент панели
        ctx.enableScissor((int) x, (int)(y + HEADER_HEIGHT), (int)(x + PANEL_WIDTH), (int)(y + PANEL_HEIGHT));

        float offsetY = y + HEADER_HEIGHT + 2f + scrollOffsetSmooth;

        for (NewGuiModuleEntry entry : moduleEntries) {
            // Фильтр поиска
            NewClickGui gui = getGui();
            if (gui != null && gui.isSearching()) {
                String search = gui.getSearchText().toLowerCase();
                if (!search.isEmpty() && !entry.getModule().getName().toLowerCase().contains(search)) {
                    continue;
                }
            }

            entry.render(ctx, mouseX, mouseY, x, offsetY, PANEL_WIDTH, alpha);
            offsetY += entry.getTotalHeight() + MODULE_PADDING;
        }

        ctx.disableScissor();
    }

    public void onMouseClicked(float mouseX, float mouseY, MouseButton button) {
        if (mouseX < x || mouseX > x + PANEL_WIDTH || mouseY < y + HEADER_HEIGHT || mouseY > y + PANEL_HEIGHT) return;

        float offsetY = y + HEADER_HEIGHT + 2f + scrollOffsetSmooth;
        for (NewGuiModuleEntry entry : moduleEntries) {
            entry.onMouseClicked(mouseX, mouseY, button, x, offsetY, PANEL_WIDTH);
            offsetY += entry.getTotalHeight() + MODULE_PADDING;
        }
    }

    public void onMouseReleased(float mouseX, float mouseY, MouseButton button) {
        for (NewGuiModuleEntry entry : moduleEntries) {
            entry.onMouseReleased(mouseX, mouseY, button);
        }
    }

    public void onScroll(float mouseX, float mouseY, float delta) {
        if (mouseX < x || mouseX > x + PANEL_WIDTH || mouseY < y || mouseY > y + PANEL_HEIGHT) return;
        scrollOffset += delta * 18f;
        clampScroll();
    }

    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        for (NewGuiModuleEntry entry : moduleEntries) {
            entry.onKeyPressed(keyCode, scanCode, modifiers);
        }
    }

    private void clampScroll() {
        float totalHeight = 0;
        for (NewGuiModuleEntry entry : moduleEntries) {
            totalHeight += entry.getTotalHeight() + MODULE_PADDING;
        }
        float maxScroll = Math.max(0, totalHeight - (PANEL_HEIGHT - HEADER_HEIGHT - 6f));
        scrollOffset = MathHelper.clamp(scrollOffset, -maxScroll, 0f);
    }

    private NewClickGui getGui() {
        try {
            space.visuals.client.modules.impl.render.Menu menu =
                    space.visuals.client.modules.impl.render.Menu.INSTANCE;
            if (menu != null) {
                return menu.getNewClickGui();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
