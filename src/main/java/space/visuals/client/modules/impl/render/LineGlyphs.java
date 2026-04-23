package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import space.visuals.Zenith;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "LineGlyphs", category = Category.RENDER, description = "Падающие линии-глифы в мире")
public final class LineGlyphs extends Module {

    public static final LineGlyphs INSTANCE = new LineGlyphs();

    private final BooleanSetting glowing    = new BooleanSetting("Свечение", true);
    private final BooleanSetting dashed     = new BooleanSetting("Пунктир", false);
    private final ModeSetting    colorMode  = new ModeSetting("Цвет", "Клиентский", "Кастом", "Двойной");
    private final ColorSetting   color1     = new ColorSetting("Цвет 1", new ColorRGBA(100, 200, 255));
    private final ColorSetting   color2     = new ColorSetting("Цвет 2", new ColorRGBA(255, 100, 200));

    private static final int   LINE_COUNT      = 120;
    private static final float FALL_SPEED      = 0.07f;
    private static final float LINE_LENGTH     = 17f;
    private static final float SEGMENT_LENGTH  = 2f;
    private static final float ZIGZAG_WIDTH    = 5f;

    private final List<FallingLine> lines  = new ArrayList<>();
    private final Random            random = new Random();

    private LineGlyphs() {}

    @Override
    public void onEnable() {
        super.onEnable();
        generateLines();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lines.clear();
    }

    private void generateLines() {
        lines.clear();
        if (mc.player == null) return;
        for (int i = 0; i < LINE_COUNT; i++) {
            lines.add(new FallingLine(random, mc.player.getPos()));
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getPos();

        // Считаем сколько нужно заспавнить
        int toSpawn = LINE_COUNT - lines.size();

        Iterator<FallingLine> it = lines.iterator();
        while (it.hasNext()) {
            FallingLine line = it.next();
            line.update(playerPos, FALL_SPEED, SEGMENT_LENGTH, ZIGZAG_WIDTH, LINE_LENGTH);
            if (line.shouldRespawn(playerPos)) {
                it.remove();
                toSpawn++;
            }
        }

        for (int i = 0; i < toSpawn; i++) {
            lines.add(new FallingLine(random, playerPos));
        }

        renderLines();
    }

    private void renderLines() {
        if (lines.isEmpty()) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        for (int li = 0; li < lines.size(); li++) {
            FallingLine line = lines.get(li);
            if (line.points.size() < 2) continue;

            int baseColor = getColor(li);

            for (int i = 0; i < line.points.size() - 1; i++) {
                Vec3d start = line.points.get(i);
                Vec3d end   = line.points.get(i + 1);

                if (start.distanceTo(camPos) > 60 || end.distanceTo(camPos) > 60) continue;

                float segAlpha = (float)(i + 1) / line.points.size();
                int segColor   = applyAlpha(baseColor, (int)(segAlpha * 255));

                float width = glowing.isEnabled() ? 2.5f : 1.5f;

                if (dashed.isEnabled()) {
                    drawDashedLine(start, end, segColor, width);
                    if (glowing.isEnabled()) {
                        drawDashedLine(start, end, applyAlpha(baseColor, (int)(segAlpha * 100)), 4.0f);
                    }
                } else {
                    Render3DUtil.drawLine(start, end, segColor, width, true);
                    if (glowing.isEnabled()) {
                        Render3DUtil.drawLine(start, end, applyAlpha(baseColor, (int)(segAlpha * 100)), 4.0f, true);
                    }
                }
            }
        }
    }

    private void drawDashedLine(Vec3d start, Vec3d end, int color, float width) {
        double dashLen  = 0.15;
        double gapLen   = 0.10;
        double pattern  = dashLen + gapLen;
        Vec3d  dir      = end.subtract(start);
        double len      = dir.length();
        Vec3d  norm     = dir.normalize();
        double dist     = 0;
        while (dist < len) {
            double dashEnd = Math.min(dist + dashLen, len);
            Render3DUtil.drawLine(
                start.add(norm.multiply(dist)),
                start.add(norm.multiply(dashEnd)),
                color, width, true);
            dist += pattern;
        }
    }

    private int getColor(int index) {
        return switch (colorMode.get()) {
            case "Кастом"   -> color1.getColor().getRGB();
            case "Двойной"  -> {
                float t = (index % 10) / 10f;
                yield color1.getColor().mix(color2.getColor(), t).getRGB();
            }
            default -> Zenith.getInstance().getThemeManager().getClientColor(index * 10).getRGB();
        };
    }

    private static int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.min(255, Math.max(0, alpha)) << 24);
    }

    // ── Внутренний класс ──────────────────────────────────────────────────────

    private static class FallingLine {
        final List<Vec3d> points     = new ArrayList<>();
        Vec3d  currentDirection;
        double distanceTraveled      = 0;
        double currentSegmentLength;
        final Random random          = new Random();

        FallingLine(Random rng, Vec3d playerPos) {
            // Спавним равномерно вокруг игрока в случайной точке сферы радиусом 8-14 блоков
            double radius = 8 + rng.nextDouble() * 6;
            double theta  = rng.nextDouble() * 2 * Math.PI;          // горизонтальный угол
            double phi    = Math.acos(2 * rng.nextDouble() - 1);      // вертикальный угол (равномерно по сфере)

            double x = playerPos.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = playerPos.y + 1 + rng.nextDouble() * 3 + radius * Math.cos(phi) * 0.4;
            double z = playerPos.z + radius * Math.sin(phi) * Math.sin(theta);

            points.add(new Vec3d(x, y, z));
            currentDirection     = randomDir();
            currentSegmentLength = 0.5 + rng.nextDouble() * 1.5;
        }

        void update(Vec3d playerPos, float speed, float segLen, float zigzag, float maxLen) {
            if (points.isEmpty()) return;
            Vec3d last = points.get(points.size() - 1);
            points.add(last.add(currentDirection.multiply(speed)));
            distanceTraveled += speed;

            if (distanceTraveled >= currentSegmentLength) {
                currentDirection     = randomDir();
                distanceTraveled     = 0;
                currentSegmentLength = 0.5 + random.nextDouble() * 1.5;
            }

            // Обрезаем хвост
            double total = 0;
            for (int i = points.size() - 1; i > 0; i--) {
                total += points.get(i).distanceTo(points.get(i - 1));
            }
            while (total > maxLen && points.size() > 2) {
                total -= points.get(0).distanceTo(points.get(1));
                points.remove(0);
            }
        }

        boolean shouldRespawn(Vec3d playerPos) {
            if (points.isEmpty()) return true;
            Vec3d last = points.get(points.size() - 1);
            // Респавним только если линия ушла слишком далеко от игрока
            return last.distanceTo(playerPos) > 20;
        }

        private Vec3d randomDir() {
            return switch (random.nextInt(6)) {
                case 0 -> new Vec3d( 1,  0,  0);
                case 1 -> new Vec3d(-1,  0,  0);
                case 2 -> new Vec3d( 0,  1,  0);
                case 3 -> new Vec3d( 0, -1,  0);
                case 4 -> new Vec3d( 0,  0,  1);
                default -> new Vec3d( 0,  0, -1);
            };
        }
    }
}
