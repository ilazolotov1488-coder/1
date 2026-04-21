package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventHudRender;
import space.visuals.base.waypoints.WaypointManager;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.utility.math.ProjectionUtil;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;
import space.visuals.base.font.Fonts;
import space.visuals.base.font.Font;

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(name = "Waypoints", category = Category.RENDER, description = "Отображает метки в мире")
public class WaypointsModule extends Module {

    public static final WaypointsModule INSTANCE = new WaypointsModule();

    // Анимации появления для каждой метки
    private final Map<String, Animation> fadeAnims = new HashMap<>();

    // Цвета
    private static final ColorRGBA BG       = new ColorRGBA(8, 9, 16, 200);
    private static final ColorRGBA BORDER   = new ColorRGBA(108, 99, 210, 180);
    private static final ColorRGBA BORDER_P = new ColorRGBA(255, 210, 50, 200);  // для игроков
    private static final ColorRGBA TXT      = new ColorRGBA(240, 240, 255, 255);
    private static final ColorRGBA DIST_CLR = new ColorRGBA(140, 130, 220, 255);
    private static final ColorRGBA GLOW     = new ColorRGBA(90, 80, 210, 255);
    private static final ColorRGBA GLOW_P   = new ColorRGBA(255, 180, 30, 255);

    @EventTarget
    public void onHudRender(EventHudRender event) {
        if (mc.player == null || mc.world == null) return;

        CustomDrawContext ctx = event.getContext();
        MatrixStack ms = ctx.getMatrices();

        for (WaypointManager.Waypoint waypoint : Zenith.getInstance().getWaypointManager().getWaypoints()) {
            Vec3d pos = waypoint.pos;
            Vec3d renderPos = pos.add(0.5, 1.0, 0.5);

            // Конвертируем 3D → 2D
            Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(renderPos);
            if (screen == null || screen.z <= 0 || screen.z >= 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;

            float distance = (float) mc.player.getPos().distanceTo(pos);

            // Масштаб зависит от дистанции
            float scale = MathHelper.clamp(1.0f - distance / 200.0f, 0.35f, 1.0f);

            // Анимация появления
            fadeAnims.computeIfAbsent(waypoint.name, k -> {
                Animation a = new Animation(300, 0, Easing.QUAD_IN_OUT);
                a.animateTo(1);
                return a;
            });
            float alpha = (float) fadeAnims.get(waypoint.name).update();

            boolean isPlayer = waypoint.playerUUID != null;
            ColorRGBA borderColor = isPlayer ? BORDER_P : BORDER;
            ColorRGBA glowColor   = isPlayer ? GLOW_P   : GLOW;

            // Форматируем текст
            Font nameFont = Fonts.MEDIUM.getFont(8f);
            Font distFont = Fonts.MEDIUM.getFont(6f);
            String distText = formatDistance(distance);
            float nameW = nameFont.width(waypoint.name);
            float distW = distFont.width(distText);
            float labelW = Math.max(nameW, distW) + 16f;
            float labelH = nameFont.height() + distFont.height() + 10f;

            ms.push();
            ms.translate(sx, sy, 0);
            ms.scale(scale, scale, 1);

            float lx = -labelW / 2f;
            float ly = -labelH - 6f;

            // Свечение
            try {
                DrawUtil.drawShadow(ms, lx - 6, ly - 6, labelW + 12, labelH + 12,
                    14f, BorderRadius.all(8), glowColor.mulAlpha(0.25f * alpha));
            } catch (Exception ignored) {}

            // Фон
            DrawUtil.drawRoundedRect(ms, lx, ly, labelW, labelH, BorderRadius.all(6), BG.mulAlpha(alpha));

            // Рамка
            DrawUtil.drawRoundedBorder(ms, lx, ly, labelW, labelH, -0.5f, BorderRadius.all(6),
                borderColor.mulAlpha(alpha));

            // Цветная полоска сверху
            DrawUtil.drawRoundedRect(ms, lx, ly, labelW, 2f, BorderRadius.all(1), borderColor.mulAlpha(alpha));

            // Текст названия
            ctx.drawText(nameFont, waypoint.name,
                lx + (labelW - nameW) / 2f,
                ly + 5f,
                TXT.mulAlpha(alpha));

            // Текст дистанции
            ctx.drawText(distFont, distText,
                lx + (labelW - distW) / 2f,
                ly + 5f + nameFont.height() + 2f,
                DIST_CLR.mulAlpha(alpha));

            // Стрелка вниз (указатель)
            float arrowX = sx;
            float arrowY = sy - 4f * scale;
            drawArrow(ctx, ms, 0f, -4f, 5f * scale, borderColor.mulAlpha(alpha));

            ms.pop();
        }

        // Чистим анимации удалённых меток
        fadeAnims.keySet().removeIf(name ->
            Zenith.getInstance().getWaypointManager().getWaypoints()
                .stream().noneMatch(w -> w.name.equals(name))
        );
    }

    private void drawArrow(CustomDrawContext ctx, MatrixStack ms, float x, float y, float size, ColorRGBA color) {
        // Маленький треугольник-указатель
        int rgb = color.getRGB();
        for (int i = 0; i < (int) size; i++) {
            float w = (size - i) * 2;
            ctx.drawRect(x - w / 2f, y + i, w, 1, color);
        }
    }

    private String formatDistance(float dist) {
        if (dist >= 1000) return String.format("%.1fkm", dist / 1000f);
        return String.format("%.0fm", dist);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        fadeAnims.clear();
    }
}
