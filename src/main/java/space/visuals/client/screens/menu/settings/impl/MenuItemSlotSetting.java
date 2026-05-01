package space.visuals.client.screens.menu.settings.impl;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import space.visuals.Zenith;
import space.visuals.base.font.Font;
import space.visuals.base.font.Fonts;
import space.visuals.base.theme.Theme;
import space.visuals.client.modules.api.setting.impl.ItemSlotSetting;
import space.visuals.client.screens.menu.settings.api.MenuSetting;
import space.visuals.utility.game.other.MouseButton;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.Rect;
import space.visuals.utility.render.display.base.UIContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;

/**
 * Рендер ItemSlotSetting в меню.
 * Показывает название + иконку предмета (или пустой слот).
 * Клик → закрывает меню, открывает инвентарь для выбора предмета.
 */
public class MenuItemSlotSetting extends MenuSetting {

    @Getter
    private final ItemSlotSetting setting;
    private Rect slotBounds;

    public MenuItemSlotSetting(ItemSlotSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float x, float settingY,
                       float moduleWidth, float alpha, float animEnable,
                       ColorRGBA themeColor, ColorRGBA textColor, ColorRGBA descriptionColor, Theme theme) {

        Font settingFont = Fonts.MEDIUM.getFont(7);
        float textY = settingY + (8 - settingFont.height()) / 2f - 0.5f;

        // Название настройки
        ctx.drawText(settingFont, setting.getName(), x + 8 + 10, textY, textColor);

        // Иконка слева (как в других настройках)
        ctx.drawText(Fonts.ICONS.getFont(6), "V", x + 9.5f, textY + 1, themeColor);

        // Слот предмета справа
        float slotSize = 16f;
        float slotX = x + moduleWidth - slotSize - 8f;
        float slotY = settingY - 4f;

        // Фон слота
        ColorRGBA slotBg = theme.getForegroundLight().mulAlpha(alpha);
        ctx.drawRoundedRect(slotX, slotY, slotSize, slotSize, BorderRadius.all(3), slotBg);
        ctx.drawRoundedBorder(slotX, slotY, slotSize, slotSize, -0.1f, BorderRadius.all(3),
                themeColor.mulAlpha(0.6f * alpha));

        // Иконка предмета
        ItemStack stack = setting.getStack();
        if (!stack.isEmpty()) {
            ctx.drawItem(stack, (int)(slotX), (int)(slotY));
        } else {
            // Пустой слот — знак +
            Font plusFont = Fonts.MEDIUM.getFont(8);
            float plusX = slotX + (slotSize - plusFont.width("+")) / 2f;
            float plusY = slotY + (slotSize - plusFont.height()) / 2f;
            ctx.drawText(plusFont, "+", plusX, plusY, theme.getGray().mulAlpha(alpha));
        }

        slotBounds = new Rect(slotX, slotY, slotSize, slotSize);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (slotBounds == null || !slotBounds.contains(mouseX, mouseY)) return;
        if (button != MouseButton.LEFT) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Регистрируем ожидание выбора
        ItemSlotPickerManager.INSTANCE.startPicking(setting);

        // Закрываем меню и открываем инвентарь через 2 тика
        mc.execute(() -> {
            mc.setScreen(null); // закрываем меню
            mc.execute(() -> mc.execute(() ->
                mc.setScreen(new InventoryScreen(mc.player))
            ));
        });
    }

    @Override
    public float getWidth() { return 0; }

    @Override
    public float getHeight() { return 8; }

    @Override
    public boolean isVisible() { return setting.getVisible().get(); }
}
