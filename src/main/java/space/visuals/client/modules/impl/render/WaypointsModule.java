package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventRender2D;
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

    private final Map<String, Animation> fadeAnims = new HashMap<>();

    private static final Identifier MARKER_ICON = Zenith.id("icons/marker.png");
    private static final Identifier TARGET_ICON  = Zenith.id("icons/target2.png");

    private static final ColorRGBA BG       = new ColorRGBA(8,   9,  16, 210);
    private static final ColorRGBA BORDER   = new ColorRGBA(108, 99, 210, 200);
    private static final ColorRGBA BORDER_P = new ColorRGBA(255, 210, 50, 200);
    private static final ColorRGBA TXT      = new ColorRGBA(240, 240, 255, 255);
    private static final ColorRGBA DIST_CLR = new ColorRGBA(150, 140, 230, 255);
    private static final ColorRGBA GLOW     = new ColorRGBA(90,  80, 210, 255);
    private static final ColorRGBA GLOW_P   = new ColorRGBA(255, 180,  30, 255);

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.getEntityRenderDispatcher().camera == null) return;

        CustomDrawContext ctx = event.getContext();

        for (WaypointManager.Waypoint waypoint : Zenith.getInstance().getWaypointManager().getWaypoints()) {
            Vec3d pos = waypoint.pos;

            Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(pos.add(0.5, 1.2, 0.5));
            // z: 0..1 = перед камерой, >1 или <0 = за камерой
            if (screen.z <= 0 || screen.z >= 1) continue;

            float sx = (float) screen.x;
            float sy = (float) screen.y;
            float distance = (float) mc.player.getPos().distanceTo(pos);

            float scale = MathHelper.clamp(1.0f - distance / 300.0f, 0.4f, 1.0f);

            Animation anim = fadeAnims.computeIfAbsent(waypoint.name, k -> {
                Animation a = new Animation(350, 0, Easing.QUAD_IN_OUT);
                a.animateTo(1);
                return a;
            });
            float alpha = (float) anim.update();
            if (alpha < 0.01f) continue;

            boolean isPlayer = waypoint.playerUUID != null;
            ColorRGBA borderColor = isPlayer ? BORDER_P : BORDER;
            ColorRGBA glowColor   = isPlayer ? GLOW_P   : GLOW;
            Identifier icon       = isPlayer ? TARGET_ICON : MARKER_ICON;

            Font nameFont = Fonts.MEDIUM.getFont(8f);
            Font distFont = Fonts.MEDIUM.getFont(6f);
            String distText = formatDistance(distance);

            float nameW = nameFont.width(waypoint.name);
            float distW = distFont.width(distText);
            float iconSz = 10f;
            float labelW = Math.max(nameW, distW) + iconSz + 20f;
            float labelH = nameFont.height() + distFont.height() + 10f;

            // Позиция метки — центрируем над точкой
            float lx = sx - (labelW * scale) / 2f;
            float ly = sy - (labelH * scale) - 8f * scale;
            float rw = labelW * scale;
            float rh = labelH * scale;

            // Свечение
            try {
                DrawUtil.drawShadow(ctx.getMatrices(),
                    lx - 6 * scale, ly - 6 * scale,
                    rw + 12 * scale, rh + 12 * scale,
                    14f, BorderRadius.all(8),
                    glowColor.mulAlpha(0.20f * alpha));
            } catch (Exception ignored) {}

            // Фон
            DrawUtil.drawRoundedRect(ctx.getMatrices(), lx, ly, rw, rh,
                BorderRadius.all(6), BG.mulAlpha(alpha));

            // Рамка
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), lx, ly, rw, rh,
                -0.5f, BorderRadius.all(6), borderColor.mulAlpha(alpha));

            // Полоска сверху
            DrawUtil.drawRoundedRect(ctx.getMatrices(), lx, ly, rw, 2f * scale,
                BorderRadius.all(1), borderColor.mulAlpha(alpha));

            // Иконка
            float iconX = lx + 4f * scale;
            float iconY = ly + (rh - iconSz * scale) / 2f;
            DrawUtil.drawTexture(ctx.getMatrices(), icon,
                iconX, iconY, iconSz * scale, iconSz * scale,
                ColorRGBA.WHITE.mulAlpha(alpha));

            // Текст — через context.pushMatrix/popMatrix как в EntityESP
            float textX = lx + (iconSz + 8f) * scale;
            float textY = ly + 4f * scale;

            ctx.pushMatrix();
            ctx.getMatrices().translate(textX, textY, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.drawText(nameFont, waypoint.name, 0, 0, TXT.mulAlpha(alpha));
            ctx.drawText(distFont, distText, 0, nameFont.height() + 2f, DIST_CLR.mulAlpha(alpha));
            ctx.popMatrix();

            // Стрелка вниз
            float arrowHalfW = 4f * scale;
            drawTriangle(ctx,
                sx - arrowHalfW, ly + rh,
                sx + arrowHalfW, ly + rh,
                sx, sy,
                borderColor.mulAlpha(alpha * 0.8f));
        }

        // Чистим анимации удалённых меток
        fadeAnims.keySet().removeIf(name ->
            Zenith.getInstance().getWaypointManager().getWaypoints()
                .stream().noneMatch(w -> w.name.equals(name))
        );
    }

    private void drawTriangle(CustomDrawContext ctx,
                              float x1, float y1,
                              float x2, float y2,
                              float tx, float ty,
                              ColorRGBA color) {
        float baseY = Math.max(y1, ty);
        float tipY  = Math.min(y1, ty);
        float height = baseY - tipY;
        if (height < 1f) return;
        for (float y = tipY; y <= baseY; y++) {
            float t = (y - tipY) / height;
            float halfW = t * (x2 - x1) / 2f;
            ctx.drawRect(tx - halfW, y, halfW * 2f, 1f, color);
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
