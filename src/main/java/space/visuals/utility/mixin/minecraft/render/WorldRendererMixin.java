package space.visuals.utility.mixin.minecraft.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.client.modules.impl.render.ShaderESP;

import java.util.List;

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
     * Перед рендером каждой сущности устанавливаем цвет outline.
     * Используем mc.getBufferBuilders().getOutlineVertexConsumers() — стандартный путь в 1.21.4.
     */
    @Inject(
        method = "renderEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        ),
        require = 0
    )
    private void onBeforeRenderEntity(
        MatrixStack matrices,
        VertexConsumerProvider.Immediate immediate,
        Camera camera,
        RenderTickCounter tickCounter,
        List<Entity> entities,
        CallbackInfo ci,
        @Local Entity entity
    ) {
        if (!ShaderESP.INSTANCE.isEnabled()) return;
        if (!ShaderESP.INSTANCE.shouldOutline(entity)) return;

        int c = ShaderESP.INSTANCE.getOutlineColor();
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8)  & 0xFF;
        int b = c         & 0xFF;

        MinecraftClient.getInstance()
            .getBufferBuilders()
            .getOutlineVertexConsumers()
            .setColor(r, g, b, 255);
    }
}
