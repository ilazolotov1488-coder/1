package space.visuals;

import net.fabricmc.api.ClientModInitializer;
import space.visuals.utility.sounds.SoundRegistry;

public class ZenithInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[ZENITH] Initializer called!");
        SoundRegistry.register();
        Zenith.INSTANCE.init();
    }
}
