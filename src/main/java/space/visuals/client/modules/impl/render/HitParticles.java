package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.MultiBooleanSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@ModuleAnnotation(name = "HitParticles", category = Category.RENDER, description = "Частицы при ударе.")
public final class HitParticles extends Module {
    public static final HitParticles INSTANCE = new HitParticles();

    // ── Типы частиц ───────────────────────────────────────────────────────
    private static final ParticleType HEART     = new ParticleType("Сердечки",  Identifier.of("space", "hud/particles/heart.png"));
    private static final ParticleType ZALUPA    = new ParticleType("Залупа",    Identifier.of("space", "hud/particles/thor.png"));
    private static final ParticleType LIGHTNING = new ParticleType("Молния",    Identifier.of("space", "hud/particles/lightning.png"));
    private static final ParticleType SNOW      = new ParticleType("Снежинки",  Identifier.of("space", "hud/particles/snowflake.png"));
    private static final ParticleType ORB       = new ParticleType("Орбизы",    Identifier.of("space", "hud/particles/orb.png"));
    private static final ParticleType CROWN     = new ParticleType("Короны",    Identifier.of("space", "hud/particles/crown.png"));
    private static final ParticleType DOLLAR    = new ParticleType("Доллар",    Identifier.of("space", "hud/particles/dollar.png"));
    private static final ParticleType SKULL     = new ParticleType("Скелеты",   Identifier.of("space", "hud/particles/skull.png"));
    private static final ParticleType STAR      = new ParticleType("Звезда",    Identifier.of("space", "hud/particles/star.png"));
    private static final ParticleType FIREFLY   = new ParticleType("Огоньки",   Identifier.of("space", "hud/particles/firefly.png"));
    private static final List<ParticleType> TYPES = List.of(HEART, ZALUPA, LIGHTNING, SNOW, ORB, CROWN, DOLLAR, SKULL, STAR, FIREFLY);

    // ── Настройки ─────────────────────────────────────────────────────────
    private final BooleanSetting hitEnabled = new BooleanSetting("От ударов", true);
    private final ModeSetting    targetMode = new ModeSetting("Цели", "Игроки", "Мобы", "Все");
    private final MultiBooleanSetting hitTypes = new MultiBooleanSetting("Типы (удар)",
            MultiBooleanSetting.Value.of("Сердечки", true),
            MultiBooleanSetting.Value.of("Залупа",   false),
            MultiBooleanSetting.Value.of("Молния",   false),
            MultiBooleanSetting.Value.of("Снежинки", false),
            MultiBooleanSetting.Value.of("Орбизы",   false),
            MultiBooleanSetting.Value.of("Короны",   false),
            MultiBooleanSetting.Value.of("Доллар",   false),
            MultiBooleanSetting.Value.of("Скелеты",  false),
            MultiBooleanSetting.Value.of("Звезда",   false),
            MultiBooleanSetting.Value.of("Огоньки",  false));
    private final ModeSetting    physics  = new ModeSetting("Физика", "Реалистичная", "Без коллизий", "Без физики");
    private final NumberSetting  count    = new NumberSetting("Количество",   20f, 1f, 200f, 1f);
    private final NumberSetting  size     = new NumberSetting("Размер",       0.2f, 0.2f, 4f, 0.1f);
    private final NumberSetting  lifetime = new NumberSetting("Время жизни",  1500f, 200f, 8000f, 50f);
    private final NumberSetting  spread   = new NumberSetting("Сила разлёта", 0.25f, 0.05f, 1.5f, 0.05f);
    private final ColorSetting   color    = new ColorSetting("Цвет", Zenith.getInstance().getThemeManager().getCurrentTheme().getColor());

    private final List<Particle> particles = new ArrayList<>();
    private boolean lastAttackPressed = false;
    private Object  lastWorld = null;

    private HitParticles() {}

    @Override
    public void onEnable()  { particles.clear(); lastAttackPressed = false; lastWorld = null; super.onEnable(); }
    @Override
    public void onDisable() { particles.clear(); lastWorld = null; super.onDisable(); }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) { particles.clear(); lastWorld = null; lastAttackPressed = false; return; }
        if (lastWorld != mc.world) { particles.clear(); lastWorld = mc.world; }

        boolean currentAttack = mc.options.attackKey.isPressed();
        if (hitEnabled.isEnabled() && currentAttack && !lastAttackPressed) {
            LivingEntity target = getTarget();
            if (target != null) {
                List<ParticleType> types = getEnabledTypes(hitTypes);
                spawnBurst(getHitPosition(target), types);
                if (target instanceof PlayerEntity player) spawnHandBursts(player, types);
            }
        }
        lastAttackPressed = currentAttack;

        long now = System.currentTimeMillis();
        long lifeMs = Math.max(1L, (long) lifetime.getCurrent());
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            tickParticle(p);
            if (now - p.spawnTime >= lifeMs) particles.remove(i);
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        long now   = System.currentTimeMillis();
        long lifeMs = Math.max(1L, (long) lifetime.getCurrent());

        LinkedHashMap<Identifier, List<Particle>> grouped = new LinkedHashMap<>();
        for (Particle p : particles)
            grouped.computeIfAbsent(p.type.texture, k -> new ArrayList<>()).add(p);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        for (Map.Entry<Identifier, List<Particle>> entry : grouped.entrySet()) {
            RenderSystem.setShaderTexture(0, entry.getKey());
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (Particle p : entry.getValue()) renderParticle(buffer, event, p, lifeMs, now);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderParticle(BufferBuilder buffer, EventRender3D event, Particle p, long lifeMs, long now) {
        long ageMs = now - p.spawnTime;
        float progress = MathHelper.clamp((float) ageMs / lifeMs, 0f, 1f);
        if (progress >= 1f) return;

        float alpha = 1f - progress;
        float scale = size.getCurrent() * (0.35f + (1f - progress) * 0.65f);
        if (scale <= 0f) return;

        float tickDelta = event.getPartialTicks();
        Vec3d camPos = mc.getEntityRenderDispatcher().camera.getPos();
        Vec3d pos    = p.renderPos(tickDelta);
        float rotation = p.rotation + (float) ageMs * p.rotationSpeed;

        ColorRGBA drawColor = color.getColor(alpha);
        int colorInt = drawColor.getRGB();

        // Вычисляем billboard матрицу без push/pop — напрямую через translate+rotate
        double dx = pos.x - camPos.x;
        double dy = pos.y - camPos.y;
        double dz = pos.z - camPos.z;

        float camYaw   = mc.getEntityRenderDispatcher().camera.getYaw();
        float camPitch = mc.getEntityRenderDispatcher().camera.getPitch();

        event.getMatrix().push();
        event.getMatrix().translate(dx, dy, dz);
        event.getMatrix().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        event.getMatrix().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
        event.getMatrix().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        event.getMatrix().scale(-scale, -scale, scale);
        Matrix4f matrix = event.getMatrix().peek().getPositionMatrix();
        buffer.vertex(matrix, -0.5f, -0.5f, 0f).texture(0f, 1f).color(colorInt);
        buffer.vertex(matrix,  0.5f, -0.5f, 0f).texture(1f, 1f).color(colorInt);
        buffer.vertex(matrix,  0.5f,  0.5f, 0f).texture(1f, 0f).color(colorInt);
        buffer.vertex(matrix, -0.5f,  0.5f, 0f).texture(0f, 0f).color(colorInt);
        event.getMatrix().pop();
    }

    private void tickParticle(Particle p) {
        p.tick();
        String mode = physics.get();
        if ("Без физики".equals(mode)) {
            p.position = p.position.add(p.velocity);
            p.velocity = p.velocity.multiply(0.99, 0.99, 0.99);
            return;
        }
        p.velocity = p.velocity.multiply(0.98, 0.98, 0.98).add(0, -0.03, 0);
        if ("Без коллизий".equals(mode)) { p.position = p.position.add(p.velocity); return; }

        Vec3d next = p.position.add(p.velocity);
        BlockPos bp = BlockPos.ofFloored(next.x, next.y, next.z);
        if (!mc.world.getBlockState(bp).isAir()) {
            if (!mc.world.getBlockState(BlockPos.ofFloored(p.position.x + p.velocity.x, p.position.y, p.position.z)).isAir())
                p.velocity = new Vec3d(0, p.velocity.y, p.velocity.z);
            if (!mc.world.getBlockState(BlockPos.ofFloored(p.position.x, p.position.y + p.velocity.y, p.position.z)).isAir())
                p.velocity = new Vec3d(p.velocity.x, -p.velocity.y * 0.6, p.velocity.z);
            if (!mc.world.getBlockState(BlockPos.ofFloored(p.position.x, p.position.y, p.position.z + p.velocity.z)).isAir())
                p.velocity = new Vec3d(p.velocity.x, p.velocity.y, 0);
            p.position = p.position.add(p.velocity);
        } else {
            p.position = next;
        }
    }

    private void spawnBurst(Vec3d origin, List<ParticleType> types) {
        spawnBurst(origin, types, Math.max(1, Math.round(count.getCurrent())), spread.getCurrent());
    }

    private void spawnBurst(Vec3d origin, List<ParticleType> types, int amount, double power) {
        if (origin == null || types.isEmpty() || amount <= 0) return;
        for (int i = 0; i < amount; i++) {
            ParticleType type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
            Vec3d pos = origin.add(getRandom(-0.2, 0.2), getRandom(-0.1, 0.2), getRandom(-0.2, 0.2));
            particles.add(new Particle(type, pos, randomVelocity(power)));
        }
    }

    private Vec3d randomVelocity(double power) {
        double x = getRandom(-power, power);
        double z = getRandom(-power, power);
        double y = "Без физики".equals(physics.get()) ? getRandom(power * 0.6, power * 1.2) : getRandom(power * 0.2, power);
        return new Vec3d(x, y, z);
    }

    private void spawnHandBursts(PlayerEntity player, List<ParticleType> types) {
        if (types.isEmpty()) return;
        int amount = Math.max(1, Math.round(count.getCurrent() * 0.4f));
        double power = spread.getCurrent();
        spawnBurst(getHandPosition(player, true),  types, amount, power);
        spawnBurst(getHandPosition(player, false), types, amount, power);
    }

    private Vec3d getHandPosition(PlayerEntity player, boolean leftHand) {
        float yawRad = (float) Math.toRadians(player.getYaw());
        double sin = MathHelper.sin(yawRad), cos = MathHelper.cos(yawRad);
        Vec3d base    = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.65, player.getZ());
        Vec3d forward = new Vec3d(-sin, 0, cos).multiply(0.15);
        Vec3d side    = new Vec3d(cos, 0, sin).multiply(leftHand ? -0.35 : 0.35);
        return base.add(forward).add(side);
    }

    private List<ParticleType> getEnabledTypes(MultiBooleanSetting setting) {
        List<ParticleType> selected = new ArrayList<>();
        for (ParticleType type : TYPES)
            if (setting.isEnable(type.name)) selected.add(type);
        return selected;
    }

    private LivingEntity getTarget() {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return null;
        Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (entity instanceof LivingEntity living && living.isAlive() && isTargetAllowed(entity)) return living;
        return null;
    }

    private Vec3d getHitPosition(LivingEntity target) {
        if (mc.crosshairTarget instanceof EntityHitResult ehr) return ehr.getPos();
        return new Vec3d(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ());
    }

    private boolean isTargetAllowed(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (isNakedInvisible(player)) return false;
            return targetMode.is("Игроки") || targetMode.is("Все");
        }
        return entity instanceof LivingEntity && (targetMode.is("Мобы") || targetMode.is("Все"));
    }

    private boolean isNakedInvisible(PlayerEntity player) {
        if (!player.isInvisible()) return false;
        for (ItemStack s : player.getArmorItems()) if (!s.isEmpty()) return false;
        return true;
    }

    private static double getRandom(double min, double max) {
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    // ── Внутренние классы ─────────────────────────────────────────────────
    private static final class Particle {
        final ParticleType type;
        final long spawnTime;
        final float rotation, rotationSpeed;
        Vec3d position, prevPosition, velocity;

        Particle(ParticleType type, Vec3d position, Vec3d velocity) {
            this.type = type;
            this.position = position;
            this.prevPosition = position;
            this.velocity = velocity;
            this.spawnTime = System.currentTimeMillis();
            this.rotation = ThreadLocalRandom.current().nextFloat() * 360f;
            this.rotationSpeed = (float) (ThreadLocalRandom.current().nextDouble() * 0.3 - 0.15);
        }

        void tick() { prevPosition = position; }

        Vec3d renderPos(float tickDelta) {
            return new Vec3d(
                    MathHelper.lerp(tickDelta, prevPosition.x, position.x),
                    MathHelper.lerp(tickDelta, prevPosition.y, position.y),
                    MathHelper.lerp(tickDelta, prevPosition.z, position.z));
        }
    }

    private static final class ParticleType {
        final String name;
        final Identifier texture;
        ParticleType(String name, Identifier texture) { this.name = name; this.texture = texture; }
    }
}
