package space.visuals.base.rotation;



import space.visuals.base.rotation.mods.config.api.RotationConfig;
import space.visuals.utility.game.player.rotation.Rotation;

import java.util.function.Supplier;


public record RotationTarget(Rotation targetRotation, Supplier<Rotation> rotation, RotationConfig rotationConfigBack) {
}
