package space.visuals.client.modules.impl.movement;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoSprint", category = Category.MOVEMENT, description = "Автоматически включает спринт")
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();

    private AutoSprint() {}

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.options.sprintKey.setPressed(true);
    }
}
