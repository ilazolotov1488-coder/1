package space.visuals.client.modules.impl.render;

import com.google.gson.JsonObject;
import space.visuals.Zenith;
import space.visuals.client.hud.elements.component.NotifyComponent;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

/**
 * Модуль управляет уведомлениями о свапах предметов.
 * Содержит собственный NotifyComponent для свап-уведомлений,
 * отдельный от базовых уведомлений Interface.
 */
@ModuleAnnotation(name = "SwapNotifications", category = Category.RENDER, description = "Уведомления о свапах предметов")
public final class SwapNotifications extends Module {
    public static final SwapNotifications INSTANCE = new SwapNotifications();

    // Настройки — какие модули показывают уведомления
    public final BooleanSetting autoSwap     = new BooleanSetting("AutoSwap",     true);
    public final BooleanSetting swapPlus     = new BooleanSetting("Swap+",        true);
    public final BooleanSetting elytraHelper = new BooleanSetting("ElytraHelper", true);
    public final BooleanSetting serverHelper = new BooleanSetting("ServerHelper", true);
    public final BooleanSetting autoTotem    = new BooleanSetting("AutoTotem",    true);

    private NotifyComponent notifyComponent;

    private SwapNotifications() {}

    @Override
    public void onEnable() {
        // Создаём компонент при первом включении если ещё нет
        if (notifyComponent == null) {
            notifyComponent = new NotifyComponent(
                    "SwapNotify",
                    0.0f, 0.0f, 960.0f, 495.5f,
                    10.0f, 135.0f,
                    DraggableHudElement.Align.TOP_LEFT
            );
        }
        // Регистрируем компонент в NotifyManager как swap-компонент
        Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(notifyComponent);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(null);
        super.onDisable();
    }

    /** Вызывается из Interface для рендера компонента */
    public NotifyComponent getNotifyComponent() {
        return notifyComponent;
    }

    /** Инициализирует компонент (вызывается из Interface при создании) */
    public void initComponent(float windowWidth, float windowHeight) {
        if (notifyComponent == null) {
            notifyComponent = new NotifyComponent(
                    "SwapNotify",
                    0.0f, 0.0f, windowWidth, windowHeight,
                    10.0f, 135.0f,
                    DraggableHudElement.Align.TOP_LEFT
            );
        }
        if (isEnabled()) {
            Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(notifyComponent);
        }
    }

    @Override
    public JsonObject save() {
        JsonObject obj = super.save();
        if (notifyComponent != null) {
            obj.add("SwapNotifyPos", notifyComponent.save());
        }
        return obj;
    }

    @Override
    public void load(JsonObject obj) {
        super.load(obj);
        if (obj != null && obj.has("SwapNotifyPos") && notifyComponent != null) {
            notifyComponent.load(obj.getAsJsonObject("SwapNotifyPos"));
        }
    }
}
