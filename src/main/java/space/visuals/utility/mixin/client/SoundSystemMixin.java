package space.visuals.utility.mixin.client;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.utility.sounds.ClientSoundInstance;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Unique
    private SoundInstance zenith$currentSound;

    // Запоминаем sound при входе в play(SoundInstance)
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void zenith$captureSound(SoundInstance sound, CallbackInfo ci) {
        this.zenith$currentSound = sound;
    }

    // Перехватываем getAdjustedVolume(float, SoundCategory) — именно она вызывается в play()
    @Inject(
        method = "getAdjustedVolume(FLnet/minecraft/sound/SoundCategory;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void zenith$fixVolume(float volume, SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (zenith$currentSound instanceof ClientSoundInstance csi) {
            cir.setReturnValue(csi.getVolume());
            zenith$currentSound = null;
        }
    }
}
