package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

import java.util.HashSet;
import java.util.Set;

/**
 * ShaderESP — использует встроенный MC Glowing эффект для outline.
 *
 * Каждый тик добавляем нужным сущностям StatusEffects.GLOWING на клиенте.
 * Цвет outline берётся из WorldRendererMixin через getOutlineColor().
 */
@ModuleAnnotation(name = "ShaderESP", category = Category.RENDER, description = "Подсвечивает сущности шейдером")
public final class ShaderESP extends Module {

    public static final ShaderESP INSTANCE = new ShaderESP();

    private final BooleanSetting players = new BooleanSetting("Игроки", true);
    private final BooleanSetting mobs    = new BooleanSetting("Мобы", false);
    private final BooleanSetting self    = new BooleanSetting("Себя", false);
    private final ModeSetting colorMode  = new ModeSetting("Цвет", "Клиентский", "Кастом");
    private final ColorSetting color     = new ColorSetting("Цвет", new ColorRGBA(100, 200, 255));

    // Сущности которым мы добавили эффект — чтобы убрать при выключении
    private final Set<Integer> glowingEntities = new HashSet<>();

    private ShaderESP() {}

    @Override
    public void onDisable() {
        super.onDisable();
        removeGlowFromAll();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        Set<Integer> currentFrame = new HashSet<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!shouldOutline(entity)) continue;

            // Добавляем Glowing эффект на 2 тика (постоянно обновляем)
            living.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING, 2, 0, false, false, false
            ));
            currentFrame.add(entity.getId());
        }

        // Убираем эффект у тех кто больше не должен светиться
        for (int id : glowingEntities) {
            if (!currentFrame.contains(id)) {
                Entity entity = mc.world.getEntityById(id);
                if (entity instanceof LivingEntity living) {
                    living.removeStatusEffect(StatusEffects.GLOWING);
                }
            }
        }

        glowingEntities.clear();
        glowingEntities.addAll(currentFrame);
    }

    private void removeGlowFromAll() {
        if (mc.world == null) return;
        for (int id : glowingEntities) {
            Entity entity = mc.world.getEntityById(id);
            if (entity instanceof LivingEntity living) {
                living.removeStatusEffect(StatusEffects.GLOWING);
            }
        }
        glowingEntities.clear();
    }

    public boolean shouldOutline(Entity entity) {
        if (!isEnabled()) return false;
        if (entity instanceof PlayerEntity player) {
            if (player == mc.player) return self.isEnabled();
            return players.isEnabled();
        }
        if (entity instanceof MobEntity) return mobs.isEnabled();
        return false;
    }

    public int getOutlineColor() {
        if (colorMode.is("Кастом")) return color.getColor().getRGB();
        return Zenith.getInstance().getThemeManager().getClientColor(0).getRGB();
    }
}
