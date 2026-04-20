package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
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

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(name = "KillEffects", category = Category.RENDER, description = "Эффекты при убийстве")
public final class KillEffects extends Module {
    public static final KillEffects INSTANCE = new KillEffects();

    private final ModeSetting effect = new ModeSetting("Эффект", "Душа", "Молния", "Портал");

    private final Map<Integer, Float> healthBefore = new HashMap<>();

    private KillEffects() {}

    @EventTarget
    public void onAttackPre(EventAttack event) {
        if (event.getAction() != EventAttack.Action.PRE) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        healthBefore.put(target.getId(), target.getHealth());
    }

    @EventTarget
    public void onAttackPost(EventAttack event) {
        if (event.getAction() != EventAttack.Action.POST) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (mc.world == null || mc.player == null) return;

        Float before = healthBefore.remove(target.getId());
        if (target.getHealth() > 0 && (before == null || before > 0)) return;

        spawnEffect(target);
    }

    private void spawnEffect(LivingEntity entity) {
        Vec3d pos = entity.getPos();
        BlockPos blockPos = entity.getBlockPos();
        double cx = pos.x, cy = pos.y + entity.getHeight() / 2.0, cz = pos.z;

        switch (effect.get()) {

            // ── 1. ДУША ─────────────────────────────────────────────────
            // Из центра существа вылетает сноп частиц душ вверх,
            // расходясь в стороны как выдох последнего вздоха
            case "Душа" -> {
                // Основной столб душ — летит вверх
                for (int i = 0; i < 25; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double spread = Math.random() * 0.4;
                    mc.world.addParticle(ParticleTypes.SOUL,
                            cx + Math.cos(angle) * spread,
                            cy + Math.random() * 0.5,
                            cz + Math.sin(angle) * spread,
                            Math.cos(angle) * 0.05,
                            0.15 + Math.random() * 0.2,
                            Math.sin(angle) * 0.05);
                }
                // Кольцо огня душ у земли
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            cx + Math.cos(angle) * 0.6,
                            pos.y + 0.1,
                            cz + Math.sin(angle) * 0.6,
                            Math.cos(angle) * 0.03,
                            0.08,
                            Math.sin(angle) * 0.03);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.BLOCK_SOUL_SAND_PLACE,
                        SoundCategory.BLOCKS, 1.5f, 0.7f);
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_WITHER_AMBIENT,
                        SoundCategory.HOSTILE, 0.4f, 1.8f);
            }

            // ── 2. МОЛНИЯ ────────────────────────────────────────────────
            // Визуальная молния + звук + вспышка частиц вокруг
            case "Молния" -> {
                net.minecraft.entity.LightningEntity lightning =
                        new net.minecraft.entity.LightningEntity(
                                net.minecraft.entity.EntityType.LIGHTNING_BOLT, mc.world);
                lightning.setPosition(cx, pos.y, cz);
                lightning.setCosmetic(true);
                mc.world.addEntity(lightning);

                // Искры разлетаются от точки удара
                for (int i = 0; i < 40; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double speed = 0.1 + Math.random() * 0.3;
                    mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            cx + (Math.random() - 0.5) * 0.5,
                            pos.y + Math.random() * entity.getHeight(),
                            cz + (Math.random() - 0.5) * 0.5,
                            Math.cos(angle) * speed,
                            0.1 + Math.random() * 0.2,
                            Math.sin(angle) * speed);
                }
                // Вспышка дыма
                for (int i = 0; i < 10; i++) {
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE,
                            cx + (Math.random() - 0.5) * 1.5,
                            pos.y + Math.random() * 2,
                            cz + (Math.random() - 0.5) * 1.5,
                            0, 0.05, 0);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.WEATHER, 4f, 1f);
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                        SoundCategory.WEATHER, 2f, 1f);
            }

            // ── 3. ПОРТАЛ ────────────────────────────────────────────────
            // Существо "засасывается" в портал — частицы крутятся по спирали
            // внутрь к центру, потом вспышка портальных частиц
            case "Портал" -> {
                // Спираль частиц, закручивающихся к центру
                for (int i = 0; i < 60; i++) {
                    double t = i / 60.0;
                    double angle = t * Math.PI * 6; // 3 оборота
                    double radius = 1.5 * (1.0 - t); // сужается к центру
                    double height = t * entity.getHeight();
                    mc.world.addParticle(ParticleTypes.PORTAL,
                            cx + Math.cos(angle) * radius,
                            pos.y + height,
                            cz + Math.sin(angle) * radius,
                            -Math.cos(angle) * 0.1,
                            0.05,
                            -Math.sin(angle) * 0.1);
                }
                // Вспышка в центре
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticle(ParticleTypes.REVERSE_PORTAL,
                            cx + (Math.random() - 0.5) * 0.5,
                            cy + (Math.random() - 0.5) * 0.5,
                            cz + (Math.random() - 0.5) * 0.5,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3);
                }
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.BLOCK_PORTAL_TRAVEL,
                        SoundCategory.BLOCKS, 0.3f, 1.5f);
                mc.world.playSound(mc.player, blockPos,
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.HOSTILE, 1f, 0.8f);
            }
        }
    }
}
