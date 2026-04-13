package space.visuals.utility.mixin.accessors;

import net.minecraft.client.sound.SoundListener;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {
    @Accessor("listener")
    SoundListener getListener();
}
