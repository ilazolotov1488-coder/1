package space.visuals.client.modules.impl.render.motionblur;

import net.fabricmc.loader.api.FabricLoader;

public class IrisCompat {
    private static Boolean irisLoaded = null;

    public static boolean isIrisLoaded() {
        if (irisLoaded == null) {
            irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        }
        return irisLoaded;
    }

    public static boolean areShadersEnabled() {
        if (!isIrisLoaded()) return false;
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);
            Object config = irisApiClass.getMethod("getConfig").invoke(irisApiInstance);
            Class<?> configClass = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig");
            return (boolean) configClass.getMethod("areShadersEnabled").invoke(config);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isIrisActive() {
        return isIrisLoaded() && areShadersEnabled();
    }
}
