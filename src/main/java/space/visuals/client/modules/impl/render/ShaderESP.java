package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import space.visuals.Zenith;
import space.visuals.base.events.impl.render.EventRender3D;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.client.modules.api.setting.impl.ColorSetting;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.utility.render.display.base.color.ColorRGBA;

/**
 * ShaderESP — использует встроенный MC outline (Glowing effect pipeline).
 *
 * Работает через WorldRendererMixin который перехватывает:
 *  - canDrawEntityOutlines() → возвращает true если модуль включён
 *  - renderEntityOutlines() → добавляет нужные сущности в outline буфер
 *
 * Цвет outline берётся из настройки модуля через getOutlineColor().
 */
@ModuleAnnotation(name = "ShaderESP", category = Category.RENDER, description = "Подсвечивает сущности шейдером")
public final class ShaderESP extends Module {

    public static final ShaderESP INSTANCE = new ShaderESP();

    private final BooleanSetting players = new BooleanSetting("Игроки", true);
    private final BooleanSetting mobs    = new BooleanSetting("Мобы", false);
    private final BooleanSetting self    = new BooleanSetting("Себя", false);
    private final ModeSetting colorMode  = new ModeSetting("Цвет", "Клиентский", "Кастом");
    private final ColorSetting color     = new ColorSetting("Цвет", new ColorRGBA(100, 200, 255));

    private ShaderESP() {}

    /**
     * Вызывается из WorldRendererMixin — нужно ли рисовать outline для этой сущности.
     */
    public boolean shouldOutline(Entity entity) {
        if (!isEnabled()) return false;
        if (entity instanceof PlayerEntity player) {
            if (player == mc.player) return self.isEnabled();
            return players.isEnabled();
        }
        if (entity instanceof MobEntity) return mobs.isEnabled();
        return false;
    }

    /**
     * Цвет outline в формате ARGB.
     */
    public int getOutlineColor() {
        if (colorMode.is("Кастом")) return color.getColor().getRGB();
        return Zenith.getInstance().getThemeManager().getClientColor(0).getRGB();
    }

    // Пустой обработчик — нужен чтобы модуль регистрировался в EventManager
    @EventTarget
    public void onRender3D(EventRender3D event) {}
}
