package space.visuals.client.modules.impl.render.taksa;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TaksaBrain {

    private static final int SMOOTH_SPEED = 150;

    private Vec3d pos    = null;
    private Vec3d motion = Vec3d.ZERO;
    private float direction = (float)(Math.random() * 360);

    private SmoothValue sx = new SmoothValue(0, SMOOTH_SPEED);
    private SmoothValue sy = new SmoothValue(0, SMOOTH_SPEED);
    private SmoothValue sz = new SmoothValue(0, SMOOTH_SPEED);

    private float yaw;
    private float body;

    private SmoothValue bodySmooth  = new SmoothValue(0, SMOOTH_SPEED);
    private SmoothValue yawSmooth   = new SmoothValue(0, SMOOTH_SPEED);
    private SmoothValue pitchSmooth = new SmoothValue(0, SMOOTH_SPEED);

    private boolean lay;
    private long stayingStart = System.currentTimeMillis();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    private PlayerEntity entity;
    private LivingEntity attackTarget;
    private long attackTargetExpiry = 0;
    private static final long ATTACK_TARGET_TIMEOUT = 3000;

    public void setEntity(PlayerEntity e) { this.entity = e; }

    public void setAttackTarget(LivingEntity target) {
        this.attackTarget = target;
        this.attackTargetExpiry = System.currentTimeMillis() + ATTACK_TARGET_TIMEOUT;
    }

    public void tick() {
        if (entity == null) return;
        Vec3d playerPos = entity.getPos();

        if (pos == null || pos.distanceTo(playerPos) > 10) {
            pos = playerPos;
            sx = new SmoothValue((float)pos.x, SMOOTH_SPEED);
            sy = new SmoothValue((float)pos.y, SMOOTH_SPEED);
            sz = new SmoothValue((float)pos.z, SMOOTH_SPEED);
        }

        motion = motion.add(0, -0.2, 0);
        Vec3d newPos = pos.add(motion);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            BlockPos bp = BlockPos.ofFloored(newPos.x, newPos.y - 0.1, newPos.z);
            if (!mc.world.getBlockState(bp).isAir()) {
                double correctedY = bp.getY() + 1 + 0.1;
                newPos = new Vec3d(newPos.x, correctedY, newPos.z);
                motion = new Vec3d(motion.x, 0, motion.z);
            }
        }

        if (attackTarget != null && System.currentTimeMillis() > attackTargetExpiry) {
            attackTarget = null;
        }

        LivingEntity target = attackTarget;
        double dist = newPos.distanceTo(playerPos);

        if (target != null) {
            Vec3d targetPos = target.getPos();
            if (mc.world != null) {
                Vec3d dir = targetPos.subtract(newPos).normalize();
                BlockPos frontBp = BlockPos.ofFloored(newPos.x + dir.x * 0.5, newPos.y + 0.5, newPos.z + dir.z * 0.5);
                if (!mc.world.getBlockState(frontBp).isAir()) {
                    motion = new Vec3d(motion.x, 0.62f, motion.z);
                }
            }
            motion = motion.add(targetPos.subtract(newPos).normalize());
            Box taksaBox  = new Box(newPos.subtract(0.4, 0, 0.4), newPos.add(0.4, 0.4, 0.4));
            Box targetBox = target.getBoundingBox().expand(-0.1, 0, -0.1);
            if (taksaBox.intersects(targetBox)) {
                motion = new Vec3d(-motion.x, motion.y, -motion.z);
            }
        } else {
            if (dist > 2.0) {
                motion = motion.add(playerPos.subtract(newPos).normalize());
            }
        }

        handleRotation(newPos, target);
        pos = newPos;

        if (dist < 0.1 && target == null) {
            direction = (float)(Math.random() * 360);
            double xMot = -Math.sin(Math.toRadians(direction)) * 0.1;
            double zMot =  Math.cos(Math.toRadians(direction)) * 0.1;
            motion = motion.add(xMot, 0, zMot);
        }

        motion = motion.multiply(0.5);
        sx.set((float)pos.x);
        sy.set((float)pos.y);
        sz.set((float)pos.z);

        limbTick();

        float dx = sx.get() - (float)pos.x;
        float dz = sz.get() - (float)pos.z;
        boolean moving = Math.abs(dx) > 0.1f || Math.abs(dz) > 0.1f;
        if (moving) stayingStart = System.currentTimeMillis();
        lay = (System.currentTimeMillis() - stayingStart) > 1000;
    }

    private void handleRotation(Vec3d currentPos, LivingEntity target) {
        if (motion.x != 0 || motion.z != 0) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float)Math.toDegrees(angle) - 90;
            yaw %= 360;
            if (yaw < 0) yaw += 360;
        }

        Vec3d lookAt = (target != null) ? target.getPos() : entity.getEyePos();
        float[] rot = getRotation(currentPos, lookAt);
        float targetYawHead = rot[0];
        float targetPitch   = rot[1];

        float gradus  = lay ? 200 : 150;
        float gradus1 = lay ? 100 : 50;

        if (targetYawHead - yaw < -gradus || targetYawHead - yaw > gradus) {
            yaw = targetYawHead;
        }

        float shortestYawPath = ((((yaw - body) % 360) + 540) % 360) - 180;
        if (!lay) bodySmooth.set(body + shortestYawPath);

        yawSmooth.set(MathHelper.clamp(targetYawHead - yaw, -gradus1, gradus1));
        pitchSmooth.set(targetPitch);
        body = body + shortestYawPath;
    }

    private float[] getRotation(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double hDist = Math.sqrt(dx*dx + dz*dz);
        float yaw   = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, hDist));
        return new float[]{ yaw, pitch };
    }

    private void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double d0 = sx.get() - pos.x;
        double d2 = sz.get() - pos.z;
        float f = MathHelper.sqrt((float)(d0*d0 + d2*d2)) * 4f;
        if (f > 1f) f = 1f;
        limbSwingAmount += (f - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
    }

    public Vec3d getPos() {
        if (pos == null) return Vec3d.ZERO;
        return new Vec3d(sx.get(), sy.get(), sz.get());
    }

    public float getBody()  { return bodySmooth.get(); }
    public float getYaw()   { return yawSmooth.get(); }
    public float getPitch() { return pitchSmooth.get(); }
    public boolean isLay()  { return lay; }
}
