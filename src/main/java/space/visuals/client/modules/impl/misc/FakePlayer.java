package space.visuals.client.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.impl.render.SoulESP;

import com.adl.nativeprotect.Native;
import java.util.UUID;

@ModuleAnnotation(name = "FakePlayer", category = Category.MISC, description = "Спавнит фейкового игрока на месте")
public final class FakePlayer extends Module {
    public static final FakePlayer INSTANCE = new FakePlayer();

    private static final float MAX_HEALTH = 20f;
    private static final float BASE_DMG   = 4f;

    private OtherClientPlayerEntity fakePlayer;
    private float health = MAX_HEALTH;

    private FakePlayer() {}

    @Native
    public OtherClientPlayerEntity getFakePlayer() { return fakePlayer; }

    /** Вызывается из mixin при атаке */
    @Native
    public void onAttack() {
        if (fakePlayer == null || mc.player == null || mc.world == null) return;

        boolean isCrit = mc.player.fallDistance > 0
                && !mc.player.isOnGround()
                && !mc.player.isSubmergedInWater();

        boolean isSplash = mc.player.isSubmergedInWater() || mc.player.isSprinting();

        float dmg = BASE_DMG;
        if (isCrit) dmg *= 1.5f;   // крит +50%
        if (isSplash) dmg *= 0.8f; // сплеш -20%

        // Звук удара
        mc.world.playSound(mc.player, fakePlayer.getBlockPos(),
                isCrit ? SoundEvents.ENTITY_PLAYER_ATTACK_CRIT : SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Партиклы крита
        if (isCrit) {
            for (int i = 0; i < 8; i++) {
                double vx = (mc.world.random.nextDouble() - 0.5) * 1.5;
                double vy = mc.world.random.nextDouble() * 1.5;
                double vz = (mc.world.random.nextDouble() - 0.5) * 1.5;
                mc.world.addParticle(ParticleTypes.CRIT,
                        fakePlayer.getX(), fakePlayer.getY() + fakePlayer.getHeight() * 0.7,
                        fakePlayer.getZ(), vx, vy, vz);
            }
        }

        // Партиклы сплеша
        if (isSplash) {
            for (int i = 0; i < 5; i++) {
                double vx = (mc.world.random.nextDouble() - 0.5) * 1.0;
                double vy = mc.world.random.nextDouble() * 0.5;
                double vz = (mc.world.random.nextDouble() - 0.5) * 1.0;
                mc.world.addParticle(ParticleTypes.SPLASH,
                        fakePlayer.getX(), fakePlayer.getY() + 0.5,
                        fakePlayer.getZ(), vx, vy, vz);
            }
        }

        applyDamage(dmg);
    }

    @Native
    private void applyDamage(float amount) {
        if (fakePlayer == null || mc.world == null) return;
        health = Math.max(0f, health - amount);

        // Синхронизируем getHealth() фейк-игрока
        fakePlayer.setHealth(health);
        fakePlayer.hurtTime = 10;
        fakePlayer.maxHurtTime = 10;

        if (health <= 0f) {
            health = MAX_HEALTH;
            fakePlayer.setHealth(MAX_HEALTH);

            double x = fakePlayer.getX();
            double y = fakePlayer.getY() + fakePlayer.getHeight() / 2.0;
            double z = fakePlayer.getZ();

            // Партиклы тотема
            for (int i = 0; i < 30; i++) {
                double vx = (mc.world.random.nextDouble() - 0.5) * 2.0;
                double vy = mc.world.random.nextDouble() * 2.0;
                double vz = (mc.world.random.nextDouble() - 0.5) * 2.0;
                mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, vx, vy, vz);
            }

            // Звук тотема
            mc.world.playSound(mc.player, fakePlayer.getBlockPos(),
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // SoulESP эффект
            if (SoulESP.INSTANCE.isEnabled()) {
                SoulESP.INSTANCE.spawnGhostAt(fakePlayer.getPos(),
                        fakePlayer.getBodyYaw(), fakePlayer.getPitch());
            }
        }
    }

    @Native
    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) return;
        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.player.getName().getString());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyFrom(mc.player);
        fakePlayer.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        mc.world.addEntity(fakePlayer);
        health = MAX_HEALTH;
        fakePlayer.setHealth(MAX_HEALTH);
    }

    @Native
    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
        super.onDisable();
    }
}
