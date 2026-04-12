package space.visuals.client.hud.elements.component;

import net.minecraft.entity.attribute.EntityAttributes;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class StatusBarsComponent extends DraggableHudElement {

    // Настройки
    public final NumberSetting barHeight     = new NumberSetting("Высота баров", 6f, 3f, 20f, 0.5f);
    public final NumberSetting barWidth      = new NumberSetting("Ширина баров", 120f, 60f, 250f, 1f);
    public final BooleanSetting customColors = new BooleanSetting("Кастомные цвета", false);
    public final ColorSetting hpColor        = new ColorSetting("Цвет HP",  new ColorRGBA(100, 220, 120, 255), () -> customColors.isEnabled());
    public final ColorSetting foodColor      = new ColorSetting("Цвет еды", new ColorRGBA(220, 150, 60, 255),  () -> customColors.isEnabled());

    private final Animation hpAnim   = new Animation(350, 1, Easing.QUAD_IN_OUT);
    private final Animation absAnim  = new Animation(350, 0, Easing.QUAD_IN_OUT);
    private final Animation foodAnim = new Animation(350, 1, Easing.QUAD_IN_OUT);

    // Хотбар: высота 24px, отступ снизу ~2px
    private static final float HOTBAR_H    = 24f;
    private static final float HOTBAR_BOTTOM_PAD = 2f;
    private static final float GAP_BETWEEN = 16f; // зазор между HP и едой
    private static final float BAR_GAP     = 4f;  // отступ между баром и текстом

    public StatusBarsComponent(String name, float initialX, float initialY,
                               float windowWidth, float windowHeight,
                               float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        width  = barWidth.getCurrent() * 2 + GAP_BETWEEN;
        height = barHeight.getCurrent() + 10f;
    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(5f);

        float h  = barHeight.getCurrent();
        float bw = barWidth.getCurrent();
        float screenW = ctx.getScaledWindowWidth();
        float screenH = ctx.getScaledWindowHeight();

        // Хотбар начинается снизу: screenH - HOTBAR_H - HOTBAR_BOTTOM_PAD
        float hotbarTop = screenH - HOTBAR_H - HOTBAR_BOTTOM_PAD;

        // Бары рисуем выше хотбара
        float textH = font.height();
        float totalH = textH + BAR_GAP + h;
        float baseY = hotbarTop - totalH - 4f; // 4px зазор над хотбаром

        float centerX = screenW / 2f;
        float hpX   = centerX - bw - GAP_BETWEEN / 2f;
        float foodX = centerX + GAP_BETWEEN / 2f;

        // === HP ===
        float maxHp  = (float) mc.player.getAttributeValue(EntityAttributes.MAX_HEALTH);
        float curHp  = mc.player.getHealth();
        float absorp = mc.player.getAbsorptionAmount();

        float hpT  = maxHp > 0 ? Math.min(1f, curHp / maxHp) : 0f;
        float absT = maxHp > 0 ? Math.min(1f, absorp / maxHp) : 0f;

        hpAnim.animateTo(hpT);
        absAnim.animateTo(absT);
        float hpV  = (float) hpAnim.update();
        float absV = (float) absAnim.update();

        ColorRGBA hpCol = customColors.isEnabled() ? hpColor.getColor() : getHealthColor(hpV);
        ColorRGBA absCl = new ColorRGBA(255, 200, 60, 230);

        float barY = baseY + textH + BAR_GAP;

        // Текст HP — над баром, справа
        String hpStr = absorp > 0
                ? (int) curHp + "+" + (int) absorp
                : (int) curHp + "/" + (int) maxHp;
        ColorRGBA hpTextCol = absorp > 0 ? absCl : hpCol;
        ctx.drawText(font, hpStr, hpX + bw - font.width(hpStr), baseY, hpTextCol);

        // Фон бара HP — тёмный с лёгкой прозрачностью
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, bw, h,
                BorderRadius.all(h / 2f), new ColorRGBA(0, 0, 0, 70));
        // Тонкая подсветка сверху (highlight)
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, bw, h / 3f,
                BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 18));
        // HP заливка
        if (hpV > 0.01f) {
            float hpFill = Math.max(h, hpV * bw);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, hpFill, h,
                    BorderRadius.all(h / 2f), hpCol);
            // Highlight поверх заливки
            DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, hpFill, h / 3f,
                    BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 30));
        }
        // Поглощение (золотой слой)
        if (absV > 0.01f) {
            float absFill = Math.max(h, absV * bw);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, absFill, h,
                    BorderRadius.all(h / 2f), absCl);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), hpX, barY, absFill, h / 3f,
                    BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 30));
        }

        // === Еда ===
        float curFood = mc.player.getHungerManager().getFoodLevel();
        float foodT   = curFood / 20f;
        foodAnim.animateTo(foodT);
        float foodV = (float) foodAnim.update();

        ColorRGBA foodCol = customColors.isEnabled() ? foodColor.getColor() : getFoodColor(foodV);

        // Текст еды — над баром, слева
        String foodStr = (int) curFood + "/20";
        ctx.drawText(font, foodStr, foodX, baseY, foodCol);

        // Фон бара еды
        DrawUtil.drawRoundedRect(ctx.getMatrices(), foodX, barY, bw, h,
                BorderRadius.all(h / 2f), new ColorRGBA(0, 0, 0, 70));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), foodX, barY, bw, h / 3f,
                BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 18));
        // Заливка еды
        if (foodV > 0.01f) {
            float foodFill = Math.max(h, foodV * bw);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), foodX, barY, foodFill, h,
                    BorderRadius.all(h / 2f), foodCol);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), foodX, barY, foodFill, h / 3f,
                    BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 30));
        }

        // Обновляем размеры для drag-системы
        width  = bw * 2 + GAP_BETWEEN;
        height = totalH;
    }

    private ColorRGBA getHealthColor(float t) {
        if (t > 0.6f) return new ColorRGBA(100, 220, 120, 255);
        if (t > 0.3f) return new ColorRGBA(230, 190, 50, 255);
        return new ColorRGBA(230, 70, 70, 255);
    }

    private ColorRGBA getFoodColor(float t) {
        if (t > 0.6f) return new ColorRGBA(220, 150, 60, 255);
        if (t > 0.3f) return new ColorRGBA(230, 190, 50, 255);
        return new ColorRGBA(190, 80, 40, 255);
    }

    @Override
    protected void renderXLine(CustomDrawContext ctx, SheetCode nearest) {}
}
