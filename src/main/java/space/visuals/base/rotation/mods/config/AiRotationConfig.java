package space.visuals.base.rotation.mods.config;


import lombok.Builder;
import lombok.Getter;
import space.visuals.base.rotation.mods.config.api.RotationConfig;
import space.visuals.base.rotation.mods.config.api.RotationModeType;
import space.visuals.utility.math.IntRange;

@Getter
@Builder
public class AiRotationConfig extends RotationConfig {
    @Builder.Default
    private int tick = 3;
    @Builder.Default
    private InterpolationRotationConfig interpolationRotationConfig =new InterpolationRotationConfig(new IntRange(2,5),new IntRange(5,8),new IntRange(20,30),0.35f);

    @Override
    public RotationModeType getType() {
        return RotationModeType.AI;
    }
}
