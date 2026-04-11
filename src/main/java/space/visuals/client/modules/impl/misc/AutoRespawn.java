package space.visuals.client.modules.impl.misc;

import net.minecraft.client.gui.screen.DeathScreen;
import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoRespawn", category = Category.MISC, description = "Автоматически возрождается после смерти")
public final class AutoRespawn extends Module {
    public static final AutoRespawn INSTANCE = new AutoRespawn();
    
    private AutoRespawn() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen instanceof DeathScreen && mc.player.deathTime > 5) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
