package space.visuals.base.rotation.mods.config;


import space.visuals.base.rotation.mods.config.api.RotationConfig;
import space.visuals.base.rotation.mods.config.api.RotationModeType;

public class InstantRotationConfig extends RotationConfig {
    @Override
    public RotationModeType getType() {
        return RotationModeType.INSTANT;
    }
}
