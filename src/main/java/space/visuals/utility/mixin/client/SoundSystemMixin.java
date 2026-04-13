package space.visuals.utility.mixin.client;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.utility.mixin.accessors.SoundSystemAccessor;
import space.visuals.utility.sounds.ClientSoundInstance;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Unique
    private static final ThreadLocal<SoundInstance> zenith$currentSound = new ThreadLocal<>();

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void zenith$captureSound(SoundInstance sound, CallbackInfo ci) {
        zenith$currentSound.set(sound);
        // Если наш звук — временно выставляем listener gain в 1.0
        if (sound instanceof ClientSoundInstance) {
            AL10.alListenerf(AL10.AL_GAIN, 1.0f);
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("RETURN"))
    private void zenith$clearSound(SoundInstance sound, CallbackInfo ci) {
        zenith$currentSound.remove();
        // Восстанавливаем listener gain из настроек MC
        if (sound instanceof ClientSoundInstance) {
            SoundListener listener = ((SoundSystemAccessor) this).getListener();
            if (listener != null) {
                AL10.alListenerf(AL10.AL_GAIN, listener.volume);
            }
        }
    }

    @Inject(
        method = "getAdjustedVolume(FLnet/minecraft/sound/SoundCategory;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void zenith$fixVolume(float volume, SoundCategory category, CallbackInfoReturnable<Float> cir) {
        SoundInstance sound = zenith$currentSound.get();
        if (sound instanceof ClientSoundInstance csi) {
            cir.setReturnValue(csi.getVolume());
        }
    }
}
