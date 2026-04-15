package space.visuals.utility.render.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Кэш для связи LivingEntityRenderState -> LivingEntity.
 * Нужен для CustomModels и других фич, которым нужна реальная сущность во время рендера.
 */
public final class RenderStateEntityCache {
    private static final Map<LivingEntityRenderState, LivingEntity> STATE_ENTITY =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RenderStateEntityCache() {}

    public static void put(LivingEntityRenderState state, LivingEntity entity) {
        STATE_ENTITY.put(state, entity);
    }

    public static LivingEntity get(LivingEntityRenderState state) {
        return STATE_ENTITY.get(state);
    }
}
