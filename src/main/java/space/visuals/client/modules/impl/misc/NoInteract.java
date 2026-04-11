package space.visuals.client.modules.impl.misc;

import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.Category;

@ModuleAnnotation(name = "NoInteract", category = Category.MISC, description = "Не дает открыть контейнера")
public final class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    
    private NoInteract() {
    }
}
