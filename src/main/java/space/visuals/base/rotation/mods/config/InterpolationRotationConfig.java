package space.visuals.base.rotation.mods.config;


import lombok.AllArgsConstructor;
import lombok.Getter;
import space.visuals.base.rotation.mods.config.api.RotationConfig;
import space.visuals.base.rotation.mods.config.api.RotationModeType;
import space.visuals.utility.math.IntRange;

@Getter
@AllArgsConstructor
public class InterpolationRotationConfig extends RotationConfig {

    private final IntRange horizontalSpeedSetting;
    private final IntRange verticalSpeedSetting  ;
    private final IntRange directionChangeFactor ;
    private final float midPoint ;

    @Override
    public RotationModeType getType() {
        return RotationModeType.INTERPOLATION;
    }
}
