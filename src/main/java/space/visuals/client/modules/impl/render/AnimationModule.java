package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventCamera;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.BooleanSetting;
import space.visuals.utility.game.player.rotation.Rotation;

import static space.visuals.utility.interfaces.IMinecraft.mc;

@ModuleAnnotation(name = "Animation", category = Category.RENDER, description = "Анимации открытия инвентаря и TAB")
public final class AnimationModule extends Module {
    public static final AnimationModule INSTANCE = new AnimationModule();

    public final BooleanSetting animateInventory   = new BooleanSetting("Инвентарь", true);
    public final BooleanSetting animateTabList     = new BooleanSetting("TAB список", true);
    public final BooleanSetting animatePerspective = new BooleanSetting("Смена перспективы", true);

    // Анимация дистанции камеры: плавное отдаление от персонажа
    // Стартует с 4f (done=true), при смене перспективы reset(0.5f) → update(4f)
    private final Animation perspDistAnim = new Animation(350, 4f, Easing.SINE_IN_OUT);
    private Perspective lastPerspective = null;

    private AnimationModule() {}

    @EventTarget
    public void onCamera(EventCamera e) {
        if (!animatePerspective.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        Perspective current = mc.options.getPerspective();
        boolean isThirdPerson = !current.isFirstPerson();

        // Смена перспективы — запускаем анимацию
        if (lastPerspective != null && lastPerspective != current) {
            if (isThirdPerson) {
                // reset(0.5f) устанавливает value=startValue=targetValue=0.5, done=true
                // затем update(4f) видит newValue != targetValue → запускает анимацию
                perspDistAnim.reset(0.5f);
            } else {
                perspDistAnim.reset(4f);
            }
        }
        lastPerspective = current;

        if (!isThirdPerson) return;

        // update(4f) — каждый кадр двигаем к 4
        float dist = perspDistAnim.update(4f);

        // Анимация завершена — не вмешиваемся, ванильная дистанция
        if (perspDistAnim.isDone()) return;

        // Применяем анимированную дистанцию
        if (!e.isCancelled()) {
            e.setAngle(new Rotation(mc.player.getYaw(), mc.player.getPitch()));
            e.cancel();
        }
        e.setDistance(dist);
    }
}
