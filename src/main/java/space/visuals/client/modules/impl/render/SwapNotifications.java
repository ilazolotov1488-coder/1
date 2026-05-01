package space.visuals.client.modules.impl.render;

import com.google.gson.JsonObject;
import com.adl.nativeprotect.Native;
import space.visuals.Zenith;
import space.visuals.client.hud.elements.component.NotifyComponent;
import space.visuals.client.hud.elements.draggable.DraggableHudElement;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;

/**
 * Модуль "Уведомления" — единственный владелец NotifyComponent.
 * Управляет как базовыми уведомлениями (вкл/выкл модулей),
 * так и уведомлениями о свапах предметов.
 */
@ModuleAnnotation(name = "Notifications", category = Category.RENDER, description = "Уведомления о модулях и свапах предметов")
public final class SwapNotifications extends Module {
    public static final SwapNotifications INSTANCE = new SwapNotifications();

    // Настройки свап-уведомлений
    public final BooleanSetting autoSwap     = new BooleanSetting("AutoSwap",     true);
    public final BooleanSetting swapPlus     = new BooleanSetting("Swap+",        true);
    public final BooleanSetting elytraHelper = new BooleanSetting("ElytraHelper", true);
    public final BooleanSetting serverHelper = new BooleanSetting("ServerHelper", true);
    public final BooleanSetting autoTotem    = new BooleanSetting("AutoTotem",    true);

    // Оригинальные координаты из Interface
    private NotifyComponent notifyComponent;

    private SwapNotifications() {}

    @Native
    @Override
    public void onEnable() {
        if (notifyComponent == null) {
            notifyComponent = new NotifyComponent(
                    "Notify",
                    181.80615f, 135.5f, 960.0f, 495.5f,
                    157.03516f, -72.5f,
                    DraggableHudElement.Align.CENTER
            );
        }
        // Единый компонент — и для базовых уведомлений, и для свапов
        Zenith.getInstance().getNotifyManager().setNotifyComponent(notifyComponent);
        Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(notifyComponent);
        super.onEnable();
    }

    @Native
    @Override
    public void onDisable() {
        Zenith.getInstance().getNotifyManager().setNotifyComponent(null);
        Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(null);
        super.onDisable();
    }

    @Native
    public NotifyComponent getNotifyComponent() {
        return notifyComponent;
    }

    /** Инициализирует компонент при старте Interface */
    @Native
    public void initComponent(float windowWidth, float windowHeight) {
        if (notifyComponent == null) {
            notifyComponent = new NotifyComponent(
                    "Notify",
                    181.80615f, 135.5f, windowWidth, windowHeight,
                    157.03516f, -72.5f,
                    DraggableHudElement.Align.CENTER
            );
        }
        if (isEnabled()) {
            Zenith.getInstance().getNotifyManager().setNotifyComponent(notifyComponent);
            Zenith.getInstance().getNotifyManager().setSwapNotifyComponent(notifyComponent);
        }
    }

    @Native
    @Override
    public JsonObject save() {
        JsonObject obj = super.save();
        if (notifyComponent != null) {
            obj.add("NotifyPos", notifyComponent.save());
        }
        return obj;
    }

    @Native
    @Override
    public void load(JsonObject obj) {
        super.load(obj);
        if (obj != null && obj.has("NotifyPos") && notifyComponent != null) {
            notifyComponent.load(obj.getAsJsonObject("NotifyPos"));
        }
    }
}
