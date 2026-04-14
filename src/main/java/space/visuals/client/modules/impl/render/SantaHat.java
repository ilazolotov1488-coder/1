package space.visuals.client.modules.impl.render;

import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "SantaHat", category = Category.RENDER, description = "Шапка Санты на игроках")
public final class SantaHat extends Module {
    public static final SantaHat INSTANCE = new SantaHat();
    private SantaHat() {}
}
