package space.visuals.base.notify;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import space.visuals.base.events.impl.other.EventModuleToggle;
import space.visuals.client.modules.api.Module;
import space.visuals.client.hud.elements.component.NotifyComponent;

public class NotifyManager {
    
    private static NotifyManager instance;
    private NotifyComponent notifyComponent;
    private NotifyComponent swapNotifyComponent;
    
    public NotifyManager() {
        EventManager.register(this);
    }
    
    public static NotifyManager getInstance() {
        if (instance == null) {
            instance = new NotifyManager();
        }
        return instance;
    }
    
    public void setNotifyComponent(NotifyComponent component) {
        this.notifyComponent = component;
    }

    public void setSwapNotifyComponent(NotifyComponent component) {
        this.swapNotifyComponent = component;
    }
    
    @EventTarget
    public void onModuleToggle(EventModuleToggle event) {
        if (notifyComponent != null) {
            notifyComponent.addNotification(event.getModule(), event.isEnabled());
        }
    }
    
    public void addNotification(Module module, boolean enabled) {
        if (notifyComponent != null) {
            notifyComponent.addNotification(module, enabled);
        }
    }

    public void addNotification(String icon, Text module) {
        if (notifyComponent != null) {
            notifyComponent.addTextNotification(icon, module);
        }
    }

    public void addSwapNotification(ItemStack stack) {
        // Используем swapNotifyComponent если задан, иначе основной
        NotifyComponent target = swapNotifyComponent != null ? swapNotifyComponent : notifyComponent;
        if (target != null) {
            target.addSwapNotification(stack);
        }
    }
}
