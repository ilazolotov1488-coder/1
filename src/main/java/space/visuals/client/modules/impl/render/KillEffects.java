package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import space.visuals.base.events.impl.player.EventAttack;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(name = "KillEffects", category = Category.RENDER, description = "Эффекты при убийстве игроков")
public final class KillEffects extends Module {
    public static final KillEffects INSTANCE = new KillEffects();

    private final ModeSetting effect = new ModeSetting("Эффект", "Молния", "Молния", "Взрыв", "Сердце", "Дым");

    private KillEffects() {}

    @EventTarget
    public void onAttack(EventAttack event) {
        if (event.getAction() != EventAttack.Action.POST) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (!(target instanceof PlayerEntity)) return;
        if (mc.world == null || mc.player == null) return;
        if (target.getHealth() > 0) return;

        spawnEffect(target);
    }

    private void spawnEffect(LivingEntity entity) {
        Vec3d pos = entity.getPos();
        BlockPos blockPos = entity.getBlockPos();

        switch (effect.get()) {
            case "Молния" -> {
                net.minecraft.entity.LightningEntity lightning =
                        new net.minecraft.entity.LightningEntity(
                                net.minecraft.entity.EntityType.LIGHTNING_BOLT, mc.world);
                lightning.setPosition(pos.x, pos.y, pos.z);
                lightning.setCosmetic(true);
                mc.world.addEntity(lightning);
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.WEATHER, 5f, 1f);
            }
            case "Взрыв" -> {
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticle(ParticleTypes.EXPLOSION,
                            pos.x + (Math.random() - 0.5) * 3,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 3,
                            (Math.random() - 0.5) * 0.2, Math.random() * 0.2, (Math.random() - 0.5) * 0.2);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.BLOCKS, 1f, 1f);
            }
            case "Сердце" -> {
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticle(ParticleTypes.HEART,
                            pos.x + (Math.random() - 0.5) * 2,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 2,
                            0, 0.1, 0);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.PLAYERS, 0.5f, 1.5f);
            }
            case "Дым" -> {
                for (int i = 0; i < 50; i++) {
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE,
                            pos.x + (Math.random() - 0.5) * 3,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 3,
                            (Math.random() - 0.5) * 0.05, Math.random() * 0.1, (Math.random() - 0.5) * 0.05);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS, 1f, 1f);
            }
        }
    }
}
