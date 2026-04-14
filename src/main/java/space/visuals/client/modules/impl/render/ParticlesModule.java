package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventAttack;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.base.events.impl.server.EventPacket;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.modules.api.setting.impl.NumberSetting;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleAnnotation(name = "Particles", category = Category.RENDER, description = "Частицы вокруг игрока")
public final class ParticlesModule extends Module {
    public static final ParticlesModule INSTANCE = new ParticlesModule();

    // ── Particle types ───────────────────────────────────────────────────────
    public enum ParticleType {
        CUBE   (null),
        DOLLAR (Zenith.id("textures/particles/bucks1.png")),
        HEART  (Zenith.id("textures/particles/heart1.png")),
        SNOW   (Zenith.id("textures/particles/show1.png")),
        STAR   (Zenith.id("textures/particles/star1.png")),
        SPARKLE(Zenith.id("textures/particles/sparkle.png"));

        public final Identifier texture;
        ParticleType(Identifier tex) { this.texture = tex; }

        public static ParticleType random() {
            ParticleType[] v = values();
            ParticleType r;
            do { r = v[new Random().nextInt(v.length)]; } while (r == CUBE);
            return r;
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────
    private final BooleanSetting spawnIdle  = new BooleanSetting("При бездействии", true);
    private final BooleanSetting spawnMove  = new BooleanSetting("При движении", true);
    private final BooleanSetting spawnCrit  = new BooleanSetting("При крите", true);
    private final BooleanSetting spawnThrow = new BooleanSetting("При броске", false);
    private final BooleanSetting spawnTotem = new BooleanSetting("При тотеме", true);
    private final BooleanSetting physic     = new BooleanSetting("Физика", false);

    private final ModeSetting particleMode = new ModeSetting("Тип", "Кубы", "Доллары", "Сердечки", "Снежинки", "Звезды", "Искры", "Рандом");
    private final ModeSetting colorMode    = new ModeSetting("Цвет", "Клиентский", "Радужный");

    // Режим движения idle
    private final ModeSetting idleMotion  = new ModeSetting("Движение (idle)", "Вверх", "Дождь");
    private final NumberSetting rainSpeed = new NumberSetting("Скорость дождя", 1.0f, 0.1f, 5.0f, 0.1f,
            () -> !idleMotion.is("Дождь") || !spawnIdle.isEnabled());

    private final NumberSetting idleCount  = new NumberSetting("Кол-во (idle)",  5f, 1f, 25f, 1f, () -> !spawnIdle.isEnabled());
    private final NumberSetting idleRange  = new NumberSetting("Дистанция",     16f, 4f, 32f, 1f, () -> !spawnIdle.isEnabled());
    private final NumberSetting idleLife   = new NumberSetting("Жизнь (idle)", 3500f, 500f, 5000f, 250f, () -> !spawnIdle.isEnabled());
    private final NumberSetting idleSize   = new NumberSetting("Размер (idle)",  0.5f, 0.0f, 1.0f, 0.1f, () -> !spawnIdle.isEnabled());

    private final NumberSetting critCount  = new NumberSetting("Кол-во (крит)",  5f, 1f, 50f, 1f, () -> !spawnCrit.isEnabled());
    private final NumberSetting critLife   = new NumberSetting("Жизнь (крит)", 3500f, 500f, 5000f, 250f, () -> !spawnCrit.isEnabled());
    private final NumberSetting critSize   = new NumberSetting("Размер (крит)",  0.5f, 0.0f, 1.0f, 0.1f, () -> !spawnCrit.isEnabled());

    private final NumberSetting moveCount  = new NumberSetting("Кол-во (движ)",  2f, 1f, 25f, 1f, () -> !spawnMove.isEnabled());
    private final NumberSetting moveLife   = new NumberSetting("Жизнь (движ)", 3500f, 500f, 5000f, 250f, () -> !spawnMove.isEnabled());
    private final NumberSetting moveSize   = new NumberSetting("Размер (движ)",  0.5f, 0.0f, 1.0f, 0.1f, () -> !spawnMove.isEnabled());

    // ── Particle lists ───────────────────────────────────────────────────────
    private final List<Particle>       idleParticles   = new CopyOnWriteArrayList<>();
    private final List<ParticleAttack> critParticles   = new CopyOnWriteArrayList<>();
    private final List<ParticleAttack> moveParticles   = new CopyOnWriteArrayList<>();
    private final List<ParticleAttack> thrownParticles = new CopyOnWriteArrayList<>();
    private final List<ParticleAttack> totemParticles  = new CopyOnWriteArrayList<>();

    private double lastX, lastY, lastZ;
    private boolean posInit = false;

    private ParticlesModule() {}

    @Override
    public void onDisable() {
        idleParticles.clear(); critParticles.clear();
        moveParticles.clear(); thrownParticles.clear(); totemParticles.clear();
        posInit = false;
        super.onDisable();
    }

    // ── Events ───────────────────────────────────────────────────────────────

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (spawnMove.isEnabled() && hasMoved() && !mc.options.getPerspective().isFirstPerson()) {
            Vec3d vel = mc.player.getVelocity();
            for (int i = 0; i < (int) moveCount.getCurrent(); i++) {
                spawnAttack(moveParticles,
                    new Vec3d(mc.player.getX() + rnd(-0.5, 0.5), mc.player.getY() + rnd(0, mc.player.getHeight()), mc.player.getZ() + rnd(-0.5, 0.5)),
                    new Vec3d(vel.x + rnd(-0.25, 0.25), rnd(-0.15, 0.15), vel.z + rnd(-0.25, 0.25)),
                    moveSize.getCurrent());
            }
        }

        if (spawnIdle.isEnabled()) {
            int r = (int) idleRange.getCurrent();
            boolean isRain = idleMotion.is("Дождь");
            for (int i = 0; i < (int) idleCount.getCurrent(); i++) {
                Vec3d add = mc.player.getPos().add(rnd(-r, r), 0, rnd(-r, r));
                BlockPos pos = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(add));
                double spawnY;
                Vec3d vel;
                if (isRain) {
                    // Дождь: спавним выше игрока, двигаемся вниз
                    spawnY = mc.player.getY() + rnd(8, 16);
                    double speed = rainSpeed.getCurrent();
                    vel = new Vec3d(rnd(-0.05, 0.05), -speed * 0.5, rnd(-0.05, 0.05));
                } else {
                    // Обычный: спавним в радиусе, двигаемся вверх
                    spawnY = mc.player.getY() + rnd(mc.player.getHeight(), r);
                    vel = new Vec3d(0, rnd(0, 0.5), 0);
                }
                spawnIdle(new Vec3d(pos.getX() + rnd(0, 1), spawnY, pos.getZ() + rnd(0, 1)), vel);
            }
        }

        if (spawnThrow.isEnabled()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EnderPearlEntity) && !(entity instanceof ArrowEntity)) continue;
                boolean moving = entity.prevX != entity.getX() || entity.prevY != entity.getY() || entity.prevZ != entity.getZ();
                if (!moving) continue;
                Vec3d pos = entity.getPos();
                Vec3d vel = entity.getVelocity();
                for (int i = 0; i < 2; i++) {
                    spawnAttack(thrownParticles,
                        new Vec3d(pos.x + rnd(-0.2, 0.2), pos.y + rnd(-0.2, 0.2), pos.z + rnd(-0.2, 0.2)),
                        new Vec3d(vel.x * 0.1 + rnd(-0.1, 0.1), vel.y * 0.1 + rnd(-0.1, 0.1), vel.z * 0.1 + rnd(-0.1, 0.1)),
                        0.3f);
                }
            }
        }

        long critLifeMs  = (long) critLife.getCurrent();
        long moveLifeMs  = (long) moveLife.getCurrent();
        long idleLifeMs  = (long) idleLife.getCurrent() + 500L;
        critParticles.removeIf(p   -> p.timer.getElapsedTime() > critLifeMs);
        moveParticles.removeIf(p   -> p.timer.getElapsedTime() > moveLifeMs);
        thrownParticles.removeIf(p -> p.timer.getElapsedTime() > 3500L);
        totemParticles.removeIf(p  -> p.timer.getElapsedTime() > 4000L);
        idleParticles.removeIf(p   -> p.timer.getElapsedTime() > idleLifeMs);
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (!spawnCrit.isEnabled()) return;
        if (event.getAction() != EventAttack.Action.PRE) return;
        if (mc.player == null || mc.player.fallDistance == 0) return;
        Entity target = event.getTarget();
        if (target == null) return;
        for (int i = 0; i < (int) critCount.getCurrent(); i++) {
            Vec3d vel = new Vec3d(rnd(-1, 1), rnd(-1, 0.25), rnd(-1, 1));
            spawnAttack(critParticles,
                new Vec3d(target.getX(), target.getY() + rnd(0, target.getHeight()), target.getZ()),
                vel, critSize.getCurrent());
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!event.isReceive()) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35) return;
        if (mc.world == null || !spawnTotem.isEnabled()) return;
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof LivingEntity)) return;
        createTotemEffect(entity.getX(), entity.getY() + entity.getHeight() / 2.0, entity.getZ());
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        MatrixStack worldMs = event.getMatrix();
        float pt = event.getPartialTicks();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        if (spawnIdle.isEnabled())  renderIdle(worldMs, pt);
        if (spawnCrit.isEnabled())  renderAttack(worldMs, critParticles,  500, (long) critLife.getCurrent(), pt);
        if (spawnMove.isEnabled() && !mc.options.getPerspective().isFirstPerson())
                                    renderAttack(worldMs, moveParticles,  500, (long) moveLife.getCurrent(), pt);
        if (spawnThrow.isEnabled()) renderAttack(worldMs, thrownParticles, 500, 3500L, pt);
        if (spawnTotem.isEnabled()) renderAttack(worldMs, totemParticles, 1000, 4000L, pt);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ── Render ───────────────────────────────────────────────────────────────

    private void renderIdle(MatrixStack worldMs, float pt) {
        long life = (long) idleLife.getCurrent();
        long fade = 500L;
        for (Particle p : idleParticles) {
            p.update(physic.isEnabled());
            long elapsed = p.timer.getElapsedTime();
            float alpha;
            if (elapsed < fade) alpha = (float) elapsed / fade;
            else if (elapsed > life) alpha = Math.max(0, 1f - (float)(elapsed - life) / fade);
            else alpha = 1f;
            float pulse = (float)((Math.sin((System.currentTimeMillis() - p.spawnTime) / 200.0) + 1.0) / 2.0);
            renderParticle(worldMs, p.position, p.prevPosition, p.type, p.size, withAlpha(p.color, alpha * pulse), p.rotate, p.rotateVec, p.prevRotateVec, elapsed, pt, false);
        }
    }

    private void renderAttack(MatrixStack worldMs, List<ParticleAttack> list, long fadeIn, long life, float pt) {
        for (ParticleAttack p : list) {
            p.update(physic.isEnabled());
            long elapsed = p.timer.getElapsedTime();
            float alpha;
            if (elapsed < fadeIn) alpha = (float) elapsed / fadeIn;
            else if (elapsed > life - 1000) alpha = Math.max(0, (life - elapsed) / 1000f);
            else alpha = 1f;
            renderParticle(worldMs, p.position, p.prevPosition, p.type, p.size, withAlpha(p.color, alpha), p.rotate, p.rotateVec, p.prevRotateVec, elapsed, pt, true);
        }
    }

    private void renderParticle(MatrixStack worldMs, Vec3d pos, Vec3d prevPos, ParticleType type, float size,
                                 int color, int rotateDeg, Vec3d rotVec, Vec3d prevRotVec, long elapsed, float pt, boolean isAttack) {
        Vec3d iPos = lerp(prevPos, pos, pt);

        if (type == ParticleType.CUBE) {
            Vec3d iRot = lerp(prevRotVec, rotVec, pt);
            double timeY = elapsed * 0.000000012;
            double timeX = elapsed * 0.000000012 * 0.6;
            Vec3d rot = new Vec3d(iRot.x + timeX, iRot.y + timeY, iRot.z + timeX * 0.5);
            renderCube(worldMs, iPos, rot, size * 2f, color);
            return;
        }

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float pitch = mc.gameRenderer.getCamera().getPitch();
        float yaw   = mc.gameRenderer.getCamera().getYaw();

        double dx = iPos.x - cam.x;
        double dy = iPos.y - cam.y;
        double dz = iPos.z - cam.z;

        MatrixStack ms = new MatrixStack();
        ms.push();
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180.0F));
        ms.translate(dx, dy, dz);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        if (type != ParticleType.STAR)  ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F));
        if (type == ParticleType.HEART) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateDeg));
        if (isAttack) ms.translate(0, -size, 0);
        else          ms.translate(0, -size, -size);

        drawTextureEntry(ms.peek(), type.texture, -size, -size, size * 2, size * 2, color);
        ms.pop();
    }

    private void drawTextureEntry(MatrixStack.Entry entry, Identifier id, float x, float y, float w, float h, int color) {
        AbstractTexture tex = mc.getTextureManager().getTexture(id);
        tex.setFilter(false, false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, tex.getGlId());

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(entry, x,     y + h, 0).texture(0, 0).color(r, g, b, a);
        buf.vertex(entry, x + w, y + h, 0).texture(0, 1).color(r, g, b, a);
        buf.vertex(entry, x + w, y,     0).texture(1, 1).color(r, g, b, a);
        buf.vertex(entry, x,     y,     0).texture(1, 0).color(r, g, b, a);
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private void renderCube(MatrixStack worldMs, Vec3d pos, Vec3d rot, float scale, int color) {
        float half = scale * 0.5f;
        Vec3d[] offsets = {
            new Vec3d(-half,-half,-half), new Vec3d(half,-half,-half),
            new Vec3d(half,half,-half),   new Vec3d(-half,half,-half),
            new Vec3d(-half,-half,half),  new Vec3d(half,-half,half),
            new Vec3d(half,half,half),    new Vec3d(-half,half,half)
        };
        Vec3d[] corners = new Vec3d[8];
        for (int i = 0; i < 8; i++) corners[i] = rotatePoint(offsets[i], rot).add(pos);

        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        int[][] diags = {{0,6},{1,7},{2,4},{3,5}};
        int diagColor = withAlpha(color, alphaOf(color) * 0.5f);

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(1.5f);

        worldMs.push();
        MatrixStack.Entry entry = worldMs.peek();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        for (int[] e : edges) {
            Vec3d a = corners[e[0]].subtract(cam);
            Vec3d b = corners[e[1]].subtract(cam);
            Vec3d n = b.subtract(a).normalize();
            buf.vertex(entry, (float)a.x, (float)a.y, (float)a.z).color(color).normal(entry, (float)n.x, (float)n.y, (float)n.z);
            buf.vertex(entry, (float)b.x, (float)b.y, (float)b.z).color(color).normal(entry, (float)n.x, (float)n.y, (float)n.z);
        }
        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.lineWidth(1.0f);
        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        for (int[] e : diags) {
            Vec3d a = corners[e[0]].subtract(cam);
            Vec3d b = corners[e[1]].subtract(cam);
            Vec3d n = b.subtract(a).normalize();
            buf.vertex(entry, (float)a.x, (float)a.y, (float)a.z).color(diagColor).normal(entry, (float)n.x, (float)n.y, (float)n.z);
            buf.vertex(entry, (float)b.x, (float)b.y, (float)b.z).color(diagColor).normal(entry, (float)n.x, (float)n.y, (float)n.z);
        }
        built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        worldMs.pop();
    }

    // ── Spawn helpers ────────────────────────────────────────────────────────

    private ParticleType getType() {
        return switch (particleMode.get()) {
            case "Доллары"  -> ParticleType.DOLLAR;
            case "Сердечки" -> ParticleType.HEART;
            case "Снежинки" -> ParticleType.SNOW;
            case "Звезды"   -> ParticleType.STAR;
            case "Искры"    -> ParticleType.SPARKLE;
            case "Рандом"   -> ParticleType.random();
            default         -> ParticleType.CUBE;
        };
    }

    private int makeColor(int index) {
        if (colorMode.is("Радужный")) {
            float hue = (index * 0.1f + System.currentTimeMillis() * 0.0003f) % 1.0f;
            return fromHSB(hue, 0.8f, 1.0f);
        }
        // Клиентский — берём тему Zenith
        space.visuals.utility.render.display.base.color.ColorRGBA c =
            Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
        return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private void spawnIdle(Vec3d pos, Vec3d vel) {
        float sz = 0.05f + (float) idleSize.getCurrent() * 0.2f;
        idleParticles.add(new Particle(getType(), pos.add(0, sz, 0), vel, idleParticles.size(), makeColor(idleParticles.size()), sz, randRot()));
    }

    private void spawnAttack(List<ParticleAttack> list, Vec3d pos, Vec3d vel, float sizeSetting) {
        float sz = 0.05f + sizeSetting * 0.2f;
        list.add(new ParticleAttack(getType(), pos.add(0, sz, 0), vel, list.size(), randRot(), makeColor(list.size()), sz));
    }

    private void createTotemEffect(double x, double y, double z) {
        for (int i = 0; i < 75; i++) {
            double ax = Math.random() * Math.PI * 2;
            double ay = Math.random() * Math.PI;
            double str = 0.5 + Math.random() * 0.5;
            Vec3d vel = new Vec3d(Math.sin(ax)*Math.sin(ay)*str, Math.cos(ay)*str, Math.cos(ax)*Math.sin(ay)*str);
            int color = Math.random() < 0.7 ? 0xFF00FF00 : 0xFFFFFF00;
            float sz = 0.05f + 0.25f * 0.2f;
            totemParticles.add(new ParticleAttack(getType(),
                new Vec3d(x + rnd(-0.3,0.3), y + rnd(-0.3,0.3), z + rnd(-0.3,0.3)),
                vel.multiply(4.0), totemParticles.size(), randRot(), color, sz));
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private boolean hasMoved() {
        if (mc.player == null) return false;
        if (!posInit) { lastX = mc.player.getX(); lastY = mc.player.getY(); lastZ = mc.player.getZ(); posInit = true; return false; }
        boolean moved = Math.abs(mc.player.getX()-lastX)>0.001 || Math.abs(mc.player.getY()-lastY)>0.001 || Math.abs(mc.player.getZ()-lastZ)>0.001;
        if (moved) { lastX = mc.player.getX(); lastY = mc.player.getY(); lastZ = mc.player.getZ(); }
        return moved;
    }

    private Vec3d lerp(Vec3d a, Vec3d b, float t) {
        return new Vec3d(a.x+(b.x-a.x)*t, a.y+(b.y-a.y)*t, a.z+(b.z-a.z)*t);
    }

    private Vec3d rotatePoint(Vec3d point, Vec3d rotation) {
        double cosY = Math.cos(rotation.y), sinY = Math.sin(rotation.y);
        double x = point.x*cosY - point.z*sinY, z = point.x*sinY + point.z*cosY;
        double cosX = Math.cos(rotation.x), sinX = Math.sin(rotation.x);
        double y = point.y*cosX - z*sinX; z = point.y*sinX + z*cosX;
        return new Vec3d(x, y, z);
    }

    private double rnd(double min, double max) { return min + (max-min)*Math.random(); }
    private int randRot() { return (int)(Math.round(rnd(0,360)/15.0)*15); }
    private int withAlpha(int argb, float alpha) {
        int a = Math.min(255, Math.max(0, (int)(alpha*255)));
        return (argb & 0x00FFFFFF) | (a << 24);
    }
    private float alphaOf(int argb) { return ((argb >> 24) & 0xFF) / 255f; }
    private int fromHSB(float h, float s, float v) {
        float[] rgb = new float[3];
        int i = (int)(h*6); float f = h*6-i, p = v*(1-s), q = v*(1-f*s), t = v*(1-(1-f)*s);
        switch(i%6){ case 0: rgb[0]=v;rgb[1]=t;rgb[2]=p;break; case 1: rgb[0]=q;rgb[1]=v;rgb[2]=p;break;
            case 2: rgb[0]=p;rgb[1]=v;rgb[2]=t;break; case 3: rgb[0]=p;rgb[1]=q;rgb[2]=v;break;
            case 4: rgb[0]=t;rgb[1]=p;rgb[2]=v;break; default: rgb[0]=v;rgb[1]=p;rgb[2]=q; }
        return (255<<24)|((int)(rgb[0]*255)<<16)|((int)(rgb[1]*255)<<8)|(int)(rgb[2]*255);
    }

    // ── Inner classes ────────────────────────────────────────────────────────

    static class Timer { private long ms = System.currentTimeMillis(); long getElapsedTime() { return System.currentTimeMillis()-ms; } }

    static class Particle {
        final ParticleType type; final int rotate, index, color; final float size;
        final long spawnTime = System.currentTimeMillis();
        final Timer timer = new Timer();
        Vec3d position, prevPosition, velocity, rotateVec, prevRotateVec, rotateMotion;
        Particle(ParticleType type, Vec3d pos, Vec3d vel, int index, int color, float size, int rotate) {
            this.type=type; this.rotate=rotate; this.index=index; this.color=color; this.size=size;
            this.position=pos; this.prevPosition=pos; this.velocity=vel.multiply(0.01);
            this.rotateVec=Vec3d.ZERO; this.prevRotateVec=Vec3d.ZERO;
            this.rotateMotion=new Vec3d(rnd(-1,1)*0.04, rnd(-1,1)*0.04, rnd(-1,1)*0.04);
        }
        void update(boolean phys) {
            prevPosition=position; prevRotateVec=rotateVec;
            if (phys) {
                if (solid(position.x,position.y,position.z+velocity.z)) velocity=new Vec3d(velocity.x,velocity.y,-velocity.z*0.8);
                if (solid(position.x,position.y+velocity.y,position.z))  velocity=new Vec3d(velocity.x*0.999,-velocity.y*0.6,velocity.z*0.999);
                if (solid(position.x+velocity.x,position.y,position.z))  velocity=new Vec3d(-velocity.x*0.8,velocity.y,velocity.z);
                velocity=velocity.multiply(0.999999).subtract(0,0.00005,0);
            }
            position=position.add(velocity);
            rotateVec=rotateVec.add(rotateMotion); rotateMotion=rotateMotion.multiply(0.98);
        }
        private static double rnd(double a, double b) { return a+(b-a)*Math.random(); }
        private boolean solid(double x, double y, double z) {
            BlockPos p=BlockPos.ofFloored(x,y,z);
            return net.minecraft.client.MinecraftClient.getInstance().world!=null &&
                   !net.minecraft.client.MinecraftClient.getInstance().world.getBlockState(p).getCollisionShape(net.minecraft.client.MinecraftClient.getInstance().world,p).isEmpty();
        }
    }

    static class ParticleAttack {
        final ParticleType type; final int rotate, index, color; final float size;
        final Timer timer = new Timer();
        Vec3d position, prevPosition, velocity, rotateVec, prevRotateVec, rotateMotion;
        ParticleAttack(ParticleType type, Vec3d pos, Vec3d vel, int index, int rotate, int color, float size) {
            this.type=type; this.rotate=rotate; this.index=index; this.color=color; this.size=size;
            this.position=pos; this.prevPosition=pos; this.velocity=vel.multiply(0.01);
            this.rotateVec=Vec3d.ZERO; this.prevRotateVec=Vec3d.ZERO;
            this.rotateMotion=new Vec3d(rnd(-1,1)*0.04, rnd(-1,1)*0.04, rnd(-1,1)*0.04);
        }
        void update(boolean phys) {
            prevPosition=position; prevRotateVec=rotateVec;
            if (phys) {
                if (solid(position.x,position.y,position.z+velocity.z)) velocity=new Vec3d(velocity.x,velocity.y,-velocity.z*0.8);
                if (solid(position.x,position.y+velocity.y,position.z))  velocity=new Vec3d(velocity.x*0.999,-velocity.y*0.6,velocity.z*0.999);
                if (solid(position.x+velocity.x,position.y,position.z))  velocity=new Vec3d(-velocity.x*0.8,velocity.y,velocity.z);
                velocity=velocity.multiply(0.999999).subtract(0,0.00005,0);
            }
            position=position.add(velocity);
            rotateVec=rotateVec.add(rotateMotion); rotateMotion=rotateMotion.multiply(0.98);
        }
        private static double rnd(double a, double b) { return a+(b-a)*Math.random(); }
        private boolean solid(double x, double y, double z) {
            BlockPos p=BlockPos.ofFloored(x,y,z);
            return net.minecraft.client.MinecraftClient.getInstance().world!=null &&
                   !net.minecraft.client.MinecraftClient.getInstance().world.getBlockState(p).getCollisionShape(net.minecraft.client.MinecraftClient.getInstance().world,p).isEmpty();
        }
    }
}
