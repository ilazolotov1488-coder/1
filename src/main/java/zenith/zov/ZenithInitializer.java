package zenith.zov;

import net.fabricmc.api.ClientModInitializer;

public class ZenithInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[ZENITH] Initializer called!");
        Zenith.INSTANCE.init();
    }
}
