package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

/**
 * FriendMarkers — уникализация друзей в табе.
 *
 * Настройки:
 *  - Подсветка в табе (зелёный цвет ника + фон строки)
 *  - Цвет подсветки в табе
 *  - Префикс в табе (Star / Heart / Diamond / Lightning)
 */
@ModuleAnnotation(name = "FriendMarkers", category = Category.RENDER, description = "Уникализация друзей в табе")
public final class FriendMarkers extends Module {
    public static final FriendMarkers INSTANCE = new FriendMarkers();

    // ── Таб ──────────────────────────────────────────────────────────────────
    /** Подсвечивать ник и фон строки друга в табе */
    public final BooleanSetting tabHighlight = new BooleanSetting("Подсветка в табе", true);

    /** Цвет подсветки ника и фона в табе */
    public final ColorSetting tabColor = new ColorSetting("Цвет в табе",
            new ColorRGBA(80, 220, 80, 255),
            () -> tabHighlight.isEnabled());

    /** Показывать префикс перед ником в табе */
    public final BooleanSetting tabPrefix = new BooleanSetting("Префикс в табе", true);

    /** Стиль префикса — ASCII-совместимые названия */
    public final ModeSetting tabPrefixStyle = new ModeSetting("Символ префикса",
            () -> tabPrefix.isEnabled(),
            "Star", "Heart", "Diamond", "Lightning", "Crown");

    private FriendMarkers() {}

    // ── Геттеры ──────────────────────────────────────────────────────────────

    public boolean isTabHighlightEnabled() {
        return this.isEnabled() && tabHighlight.isEnabled();
    }

    public ColorRGBA getTabColor() {
        return tabColor.getColor();
    }

    public boolean isTabPrefixEnabled() {
        return this.isEnabled() && tabPrefix.isEnabled();
    }

    /** Возвращает Unicode-символ по выбранному названию */
    public String getTabPrefixSymbol() {
        return switch (tabPrefixStyle.get()) {
            case "Heart"     -> "\u2665 "; // ♥
            case "Diamond"   -> "\u25C6 "; // ◆
            case "Lightning" -> "\u26A1 "; // ⚡
            case "Crown"     -> "\u265B "; // ♛
            default          -> "\u2605 "; // ★ (Star)
        };
    }
}
