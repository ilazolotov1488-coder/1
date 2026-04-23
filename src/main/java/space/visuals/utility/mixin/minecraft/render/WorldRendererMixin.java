package space.visuals.utility.mixin.minecraft.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.client.modules.impl.render.ShaderESP;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /**
     * Включаем outline pipeline когда ShaderESP активен.
     */
    @Inject(
        method = "canDrawEntityOutlines",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCanDrawEntityOutlines(CallbackInfoReturnable<Boolean> cir) {
        if (ShaderESP.INSTANCE.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Подменяем цвет outline для нужных сущностей.
     * Перехватываем isGlowing() — если сущность должна светиться через ShaderESP,
     * устанавливаем наш цвет в OutlineVertexConsumerProvider.
     */
    @Inject(
        method = "getOutlineColor(Lnet/minecraft/entity/Entity;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void onGetOutlineColor(Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (ShaderESP.INSTANCE.shouldOutline(entity)) {
            int c = ShaderESP.INSTANCE.getOutlineColor();
            // MC ожидает цвет в формате 0xRRGGBB (без альфы)
            cir.setReturnValue(c & 0xFFFFFF);
        }
    }
}
