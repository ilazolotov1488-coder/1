package space.visuals.client.modules.impl.render;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import space.visuals.Zenith;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventRender2D;
import space.visuals.base.waypoints.WaypointManager;
import space.visuals.base.theme.Theme;
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

    @Native(critical = true)
    @CompileToNative
    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.getEntityRenderDispatcher().camera == null) return;

        CustomDrawContext ctx = event.getContext();
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        
        if (Zenith.getInstance().getWaypointManager().getWaypoints().isEmpty()) return;

        for (WaypointManager.Waypoint waypoint : Zenith.getInstance().getWaypointManager().getWaypoints()) {
            Vec3d renderPos = waypoint.pos.add(0.5, 0.5, 0.5);
            
            Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(renderPos);
            
            // Проверяем что метка перед камерой (z между 0 и 1)
            // z < 0 или z > 1 означает что метка за камерой
            if (screen.z < 0 || screen.z > 1) continue;

            float distance = (float) mc.player.getPos().distanceTo(waypoint.pos);

            // Scale в зависимости от дистанции
            // Близко (0-30 блоков): scale 0.6-0.7 (средние, хорошо видно)
            // Средне (30-100 блоков): scale 0.7-0.85 (больше)
            // Далеко (100+ блоков): scale 0.85-1.0 (максимальные)
            float scale;
            if (distance < 30) {
                scale = 0.6f + (distance / 30f) * 0.1f; // 0.6 -> 0.7
            } else if (distance < 100) {
                scale = 0.7f + ((distance - 30f) / 70f) * 0.15f; // 0.7 -> 0.85
            } else {
                scale = 0.85f + Math.min((distance - 100f) / 200f, 0.15f); // 0.85 -> 1.0 (макс)
            }

            Animation anim = fadeAnims.computeIfAbsent(waypoint.name, k -> {
                Animation a = new Animation(350, 0, Easing.QUAD_IN_OUT);
                a.animateTo(1);
                return a;
            });
            float alpha = (float) anim.update();
            if (alpha < 0.01f) continue;

            Font font = Fonts.MEDIUM.getFont(6f); // Уменьшил с 7f до 6f
            String distText = formatDistance(distance);
            
            float nameWidth = font.width(waypoint.name);
            float distWidth = font.width(distText);
            float padding = 3f; // Уменьшил с 4f до 3f
            float width = nameWidth + distWidth + padding * 3;
            float height = font.height() + padding * 2;
            
            float x = (float)screen.x - width / 2f;
            float y = (float)screen.y - height - 10f;
            
            ctx.pushMatrix();
            ctx.getMatrices().translate(0, 0, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            
            // Пересчитываем позицию с учётом масштаба
            float scaledX = ((float)screen.x - width * scale / 2f) / scale;
            float scaledY = ((float)screen.y - height * scale - 10f) / scale;

            // Blur (если включен в Interface)
            if (Interface.INSTANCE.isBlur()) {
                DrawUtil.drawBlurHud(ctx.getMatrices(), scaledX, scaledY, width, height, 22, BorderRadius.all(3), ColorRGBA.WHITE);
            }
            
            // Glow (если включен в Interface)
            if (Interface.INSTANCE.isGlow()) {
                DrawUtil.drawGlow(ctx.getMatrices(), scaledX, scaledY, width, height, Interface.INSTANCE.getGlowRadius());
            }

            // Фон
            ctx.drawRoundedRect(scaledX, scaledY, width, height, BorderRadius.all(3), theme.getForegroundColor().mulAlpha(alpha));
            
            // Рамка
            ctx.drawRoundedBorder(scaledX, scaledY, width, height, 0.1f, BorderRadius.all(3), theme.getForegroundStroke().mulAlpha(alpha));
            
            // Треугольники по углам (если включены в Interface)
            DrawUtil.drawRoundedCorner(ctx.getMatrices(), scaledX, scaledY, width, height, 0.1f, 6f, theme.getColor().mulAlpha(alpha), BorderRadius.all(3));

            // Текст: имя слева
            ctx.drawText(font, waypoint.name, scaledX + padding, scaledY + padding, theme.getWhite().mulAlpha(alpha));
            
            // Текст: дистанция справа
            ctx.drawText(font, distText, scaledX + width - distWidth - padding, scaledY + padding, theme.getColor().mulAlpha(alpha));

            ctx.popMatrix();
        }

        // Чистим анимации удалённых меток
        fadeAnims.keySet().removeIf(name ->
            Zenith.getInstance().getWaypointManager().getWaypoints()
                .stream().noneMatch(w -> w.name.equals(name))
        );
    }

    private String formatDistance(float dist) {
        if (dist >= 1000) return String.format("%.1fkm", dist / 1000f);
        return String.format("%.0fm", dist);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[WaypointsModule] onEnable() called - registering events");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        fadeAnims.clear();
        System.out.println("[WaypointsModule] onDisable() called - unregistering events");
    }
}
