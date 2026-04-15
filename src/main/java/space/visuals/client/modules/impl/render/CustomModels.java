package space.visuals.client.modules.impl.render;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(name = "CustomModels", category = Category.RENDER, description = "Заменяет модель игрока на кастомную 3D.")
public final class CustomModels extends Module {
    public static final CustomModels INSTANCE = new CustomModels();

    private final ModeSetting model = new ModeSetting("Модель", CustomModelType.names());

    private CustomModels() {}

    public CustomModelType getSelectedType() {
        return CustomModelType.fromDisplay(this.model.get());
    }

    public boolean shouldApplyTo(LivingEntity entity) {
        if (!this.isEnabled() || entity == null) return false;
        if (!(entity instanceof PlayerEntity player)) return false;
        return player == mc.player;
    }
}