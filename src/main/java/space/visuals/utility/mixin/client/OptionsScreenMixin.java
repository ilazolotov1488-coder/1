package space.visuals.utility.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.Zenith;
import space.visuals.client.screens.CustomOptionsScreen;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithCustom(CallbackInfo ci) {
        if (Zenith.useVanillaMenu) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        // Получаем parent через reflection
        try {
            java.lang.reflect.Field f = Screen.class.getDeclaredField("parent");
            f.setAccessible(true);
            Screen parent = (Screen) f.get(this);
            mc.setScreen(new CustomOptionsScreen(parent));
        } catch (Exception e) {
            // Если не нашли поле - просто открываем без parent
            mc.setScreen(new CustomOptionsScreen(null));
        }
        ci.cancel();
    }
}
