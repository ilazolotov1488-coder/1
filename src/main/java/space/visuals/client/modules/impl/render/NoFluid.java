package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "NoFluid", category = Category.RENDER, description = "Убирает водный и лавовый overlay и туман.")
public final class NoFluid extends Module {
    public static final NoFluid INSTANCE = new NoFluid();

    private NoFluid() {}

    public boolean shouldRemoveFluidFog()     { return isEnabled(); }
    public boolean shouldRemoveOverlay()      { return isEnabled(); }
}
