package space.visuals.utility.mixin.minecraft.render;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.utility.interfaces.IMinecraft;

/**
 * Глобальный culling всех сущностей на уровне Minecraft:
 * - Не рендерим сущностей дальше 30 блоков
 * - Не рендерим сущностей вне поля зрения (frustum culling)
 * - Не рендерим инвизибл сущностей (опционально)
 *
 * Это работает для ВСЕХ энтити: игроков, мобов, предметов, стрел и т.д.
 * Ландшафт и постройки не затрагиваются — это только EntityRenderer.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererCullingMixin<T extends Entity, S extends EntityRenderState> implements IMinecraft {

    private static final float MAX_DISTANCE_SQ = 30f * 30f; // 900

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cullDistantAndOffscreenEntities(S state, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, int light,
                                                  CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        // 1. Проверка дистанции — самая быстрая проверка
        if (state.squaredDistanceToCamera > MAX_DISTANCE_SQ) {
            ci.cancel();
            return;
        }

        // 2. Frustum culling — не рендерим то что вне поля зрения
        Frustum frustum = mc.worldRenderer.frustum;
        if (frustum != null) {
            // Создаём bounding box на основе позиции и размеров из state
            double halfWidth = Math.max(state.width / 2.0, 0.5);
            double height = Math.max(state.height, 1.0);
            
            Box entityBox = new Box(
                state.x - halfWidth, state.y, state.z - halfWidth,
                state.x + halfWidth, state.y + height, state.z + halfWidth
            );
            
            if (!frustum.isVisible(entityBox)) {
                ci.cancel();
                return;
            }
        }

        // 3. Инвизибилити проверка (для игроков)
        // Если это PlayerEntityRenderState и сущность инвизибл — не рендерим
        if (state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState playerState) {
            if (playerState.invisible) {
                ci.cancel();
                return;
            }
        }
    }
}
