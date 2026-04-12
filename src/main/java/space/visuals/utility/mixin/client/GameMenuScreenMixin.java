package space.visuals.utility.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.Zenith;
import space.visuals.client.screens.CustomGameMenuScreen;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithCustom(CallbackInfo ci) {
        if (Zenith.useVanillaMenu) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.setScreen(new CustomGameMenuScreen());
        ci.cancel();
    }
}
