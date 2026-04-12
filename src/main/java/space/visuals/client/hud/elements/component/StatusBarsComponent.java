package space.visuals.client.hud.elements.component;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.world.GameMode;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

public class StatusBarsComponent extends DraggableHudElement {

    // Настройки
    public final NumberSetting barHeight     = new NumberSetting("Высота баров", 10f, 4f, 20f, 0.5f);
    public final BooleanSetting customColors = new BooleanSetting("Кастомные цвета", false);
    public final ModeSetting style           = new ModeSetting("Стиль", () -> customColors.isEnabled(), "Классик", "Zenith");
    public final ColorSetting hpColor        = new ColorSetting("Цвет HP",  new ColorRGBA(100, 220, 120, 255), () -> customColors.isEnabled());
    public final ColorSetting foodColor      = new ColorSetting("Цвет еды", new ColorRGBA(220, 150, 60, 255),  () -> customColors.isEnabled());

    private final Animation hpAnim   = new Animation(350, 1, Easing.QUAD_IN_OUT);
    private final Animation absAnim  = new Animation(350, 0, Easing.QUAD_IN_OUT);
    private final Animation foodAnim = new Animation(350, 1, Easing.QUAD_IN_OUT);

    private static final float HOTBAR_W    = 216f;
    private static final float HOTBAR_H    = 24f;
    private static final float HOTBAR_PAD_B = 2f;
    private static final float GAP_BETWEEN = 36f;
    private static final float TEXT_BAR_GAP = 3f;
    private static final float ABOVE_HOTBAR = 5f;

    public StatusBarsComponent(String name, float initialX, float initialY,
                               float windowWidth, float windowHeight,
                               float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        width  = HOTBAR_W;
        height = barHeight.getCurrent() + 10f;
    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;
        if (mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE
         || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) return;

        // Выбор стиля
        boolean isZenith = customColors.isEnabled() && style.is("Zenith");

        if (isZenith) {
            renderZenith(ctx);
        } else {
            renderClassic(ctx);
        }
    }

    // ─── Классический стиль ───────────────────────────────────────────────────

    private void renderClassic(CustomDrawContext ctx) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font = Fonts.MEDIUM.getFont(5f);

        float h  = barHeight.getCurrent();
        float bw = (HOTBAR_W - GAP_BETWEEN) / 2f;
        float screenW = ctx.getScaledWindowWidth();
        float screenH = ctx.getScaledWindowHeight();

        float hotbarTop  = screenH - HOTBAR_H - HOTBAR_PAD_B;
        float hotbarLeft = (screenW - HOTBAR_W) / 2f;
        float textH      = font.height();
        float barY       = hotbarTop - ABOVE_HOTBAR - h;
        float textY      = barY - TEXT_BAR_GAP - textH;
        float hpX        = hotbarLeft;
        float foodX      = hotbarLeft + bw + GAP_BETWEEN;

        // HP
        float maxHp  = (float) mc.player.getAttributeValue(EntityAttributes.MAX_HEALTH);
        float curHp  = mc.player.getHealth();
        float absorp = mc.player.getAbsorptionAmount();
        float hpT    = maxHp > 0 ? Math.min(1f, curHp / maxHp) : 0f;
        float absT   = maxHp > 0 ? Math.min(1f, absorp / maxHp) : 0f;
        hpAnim.animateTo(hpT);
        absAnim.animateTo(absT);
        float hpV  = (float) hpAnim.update();
        float absV = (float) absAnim.update();

        ColorRGBA hpCol = customColors.isEnabled() ? hpColor.getColor() : getHealthColor(hpV);
        ColorRGBA absCl = new ColorRGBA(255, 200, 60, 230);

        String hpStr = absorp > 0 ? (int)curHp + "+" + (int)absorp : (int)curHp + "/" + (int)maxHp;
        ctx.drawText(font, hpStr, hpX + bw - font.width(hpStr), textY, absorp > 0 ? absCl : hpCol);
        renderClassicBar(ctx, hpX, barY, bw, h, hpV, hpCol);
        if (absV > 0.01f) renderClassicBar(ctx, hpX, barY, bw, h, absV, absCl);

        // Еда
        float curFood = mc.player.getHungerManager().getFoodLevel();
        float foodT   = curFood / 20f;
        foodAnim.animateTo(foodT);
        float foodV = (float) foodAnim.update();
        ColorRGBA foodCol = customColors.isEnabled() ? foodColor.getColor() : getFoodColor(foodV);

        String foodStr = (int)curFood + "/20";
        ctx.drawText(font, foodStr, foodX, textY, foodCol);
        renderClassicBar(ctx, foodX, barY, bw, h, foodV, foodCol);

        width  = HOTBAR_W;
        height = textH + TEXT_BAR_GAP + h + ABOVE_HOTBAR;
    }

    private void renderClassicBar(CustomDrawContext ctx, float x, float y, float w, float h, float progress, ColorRGBA col) {
        float r = h / 2f;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(r), new ColorRGBA(0, 0, 0, 80));
        if (progress > 0.01f) {
            float fill = Math.max(r * 2f, progress * w);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, fill, h, BorderRadius.all(r), col);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y + 1f, fill, h / 3.5f, BorderRadius.top(r, r), new ColorRGBA(255, 255, 255, 35));
        }
    }

    // ─── Zenith стиль ────────────────────────────────────────────────────────

    private void renderZenith(CustomDrawContext ctx) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font font   = Fonts.MEDIUM.getFont(5f);
        Font iconFont = Fonts.ICONS.getFont(5.5f);

        float h  = barHeight.getCurrent();
        float bw = (HOTBAR_W - GAP_BETWEEN) / 2f;
        float screenW = ctx.getScaledWindowWidth();
        float screenH = ctx.getScaledWindowHeight();

        float hotbarTop  = screenH - HOTBAR_H - HOTBAR_PAD_B;
        float hotbarLeft = (screenW - HOTBAR_W) / 2f;

        float pad  = 3f;
        float textH = font.height();
        // Высота блока: padding + бар + padding (текст рисуется поверх бара по центру)
        float blockH = pad + h + pad;
        float blockW = bw;

        float hpX   = hotbarLeft;
        float foodX = hotbarLeft + bw + GAP_BETWEEN;
        float blockY = hotbarTop - ABOVE_HOTBAR - blockH;

        // HP данные
        float maxHp  = (float) mc.player.getAttributeValue(EntityAttributes.MAX_HEALTH);
        float curHp  = mc.player.getHealth();
        float absorp = mc.player.getAbsorptionAmount();
        float hpT    = maxHp > 0 ? Math.min(1f, curHp / maxHp) : 0f;
        float absT   = maxHp > 0 ? Math.min(1f, absorp / maxHp) : 0f;
        hpAnim.animateTo(hpT);
        absAnim.animateTo(absT);
        float hpV  = (float) hpAnim.update();
        float absV = (float) absAnim.update();

        ColorRGBA hpCol    = hpColor.getColor();
        ColorRGBA absCl    = new ColorRGBA(255, 200, 60, 230);
        ColorRGBA actHpCol = absorp > 0 ? absCl : hpCol;

        // Еда данные
        float curFood = mc.player.getHungerManager().getFoodLevel();
        float foodT   = curFood / 20f;
        foodAnim.animateTo(foodT);
        float foodV   = (float) foodAnim.update();
        ColorRGBA foodCol = foodColor.getColor();

        float innerBarW = blockW - pad * 2;
        float barY      = blockY + pad;

        // ── HP блок ──
        DrawUtil.drawBlurHud(ctx.getMatrices(), hpX, blockY, blockW, blockH, 18, BorderRadius.all(4f), ColorRGBA.WHITE);
        ctx.drawRoundedRect(hpX, blockY, blockW, blockH, BorderRadius.all(4f), theme.getForegroundColor());
        ctx.drawRoundedBorder(hpX, blockY, blockW, blockH, 0.5f, BorderRadius.all(4f), theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), hpX, blockY, blockW, blockH, 0.5f, 12f, theme.getColor(), BorderRadius.all(4f));

        // Бар HP (фон)
        ctx.drawRoundedRect(hpX + pad, barY, innerBarW, h, BorderRadius.all(h / 2f), theme.getForegroundLight());
        if (hpV > 0.01f) {
            float fill = Math.max(h, hpV * innerBarW);
            ctx.drawRoundedRect(hpX + pad, barY, fill, h, BorderRadius.all(h / 2f), hpCol);
            ctx.drawRoundedRect(hpX + pad, barY + 1f, fill, h / 3.5f, BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 40));
        }
        if (absV > 0.01f) {
            float fill = Math.max(h, absV * innerBarW);
            ctx.drawRoundedRect(hpX + pad, barY, fill, h, BorderRadius.all(h / 2f), absCl);
            ctx.drawRoundedRect(hpX + pad, barY + 1f, fill, h / 3.5f, BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 40));
        }
        // Текст HP по центру бара
        String hpStr = absorp > 0 ? (int)curHp + "+" + (int)absorp : (int)curHp + "/" + (int)maxHp;
        float hpTextX = hpX + pad + (innerBarW - font.width(hpStr)) / 2f;
        float hpTextY = barY + (h - textH) / 2f;
        ctx.drawText(font, hpStr, hpTextX + 0.5f, hpTextY + 0.5f, new ColorRGBA(0, 0, 0, 160));
        ctx.drawText(font, hpStr, hpTextX, hpTextY, new ColorRGBA(255, 255, 255, 255));

        // ── Еда блок ──
        DrawUtil.drawBlurHud(ctx.getMatrices(), foodX, blockY, blockW, blockH, 18, BorderRadius.all(4f), ColorRGBA.WHITE);
        ctx.drawRoundedRect(foodX, blockY, blockW, blockH, BorderRadius.all(4f), theme.getForegroundColor());
        ctx.drawRoundedBorder(foodX, blockY, blockW, blockH, 0.5f, BorderRadius.all(4f), theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), foodX, blockY, blockW, blockH, 0.5f, 12f, theme.getColor(), BorderRadius.all(4f));

        // Бар еды (фон)
        ctx.drawRoundedRect(foodX + pad, barY, innerBarW, h, BorderRadius.all(h / 2f), theme.getForegroundLight());
        if (foodV > 0.01f) {
            float fill = Math.max(h, foodV * innerBarW);
            ctx.drawRoundedRect(foodX + pad, barY, fill, h, BorderRadius.all(h / 2f), foodCol);
            ctx.drawRoundedRect(foodX + pad, barY + 1f, fill, h / 3.5f, BorderRadius.top(h / 2f, h / 2f), new ColorRGBA(255, 255, 255, 40));
        }
        // Текст еды по центру бара
        String foodStr = (int)curFood + "/20";
        float foodTextX = foodX + pad + (innerBarW - font.width(foodStr)) / 2f;
        float foodTextY = barY + (h - textH) / 2f;
        ctx.drawText(font, foodStr, foodTextX + 0.5f, foodTextY + 0.5f, new ColorRGBA(0, 0, 0, 160));
        ctx.drawText(font, foodStr, foodTextX, foodTextY, new ColorRGBA(255, 255, 255, 255));

        width  = HOTBAR_W;
        height = blockH + ABOVE_HOTBAR;
    }

    // ─── Цвета по умолчанию ──────────────────────────────────────────────────

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
