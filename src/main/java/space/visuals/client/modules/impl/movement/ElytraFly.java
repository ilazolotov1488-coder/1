package space.visuals.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "ElytraFly", category = Category.MOVEMENT, description = "Полёт HolyWorld")
public final class ElytraFly extends Module {
    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {}

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || !mc.player.isAlive()) return;
        mc.player.setVelocity(
            mc.player.getVelocity().x,
            mc.player.getVelocity().y + 0.06499999761581421,
            mc.player.getVelocity().z
        );
    }
}
