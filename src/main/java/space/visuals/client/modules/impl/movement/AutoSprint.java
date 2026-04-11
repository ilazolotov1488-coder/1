package space.visuals.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.utility.game.player.PlayerIntersectionUtil;

import java.util.List;

@ModuleAnnotation(name = "AutoSprint", category = Category.MOVEMENT, description = "Автоматически включает спринт")
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private AutoSprint() {
    }
    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.options.sprintKey.setPressed(true);
    }
}
