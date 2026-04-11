package space.visuals.base.rotation.mods;


import space.visuals.base.rotation.mods.api.RotationMode;
import space.visuals.utility.game.player.rotation.Rotation;
import space.visuals.utility.game.player.rotation.RotationDelta;

public class InstantRotationMode extends RotationMode {

    public Rotation process(Rotation target) {

        return rotationManager.getCurrentRotation().add(rotationManager.getCurrentRotation().rotationDeltaTo(target));
    }
}
