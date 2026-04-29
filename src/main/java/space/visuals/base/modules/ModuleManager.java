package space.visuals.base.modules;

import by.saskkeee.annotations.CompileToNative;
import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;


import space.visuals.base.events.impl.input.EventKey;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.impl.combat.*;
import space.visuals.client.modules.impl.misc.*;
import space.visuals.client.modules.impl.movement.*;
import space.visuals.client.modules.impl.player.AutoTool;
import space.visuals.client.modules.impl.render.*;
import space.visuals.utility.interfaces.IMinecraft;


import java.util.*;

@Getter
public final class ModuleManager implements IMinecraft {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        init();
        EventManager.register(this);
    }

    private void init() {
        registerCombat();
        registerMovement();
        registerRender();
        registerPlayer();
        registerMisc();
    }

    private void registerCombat() {
        registerModule(AutoSwap.INSTANCE);
        registerModule(AutoTotem.INSTANCE);
        registerModule(SwapPlus.INSTANCE);
        registerModule(ItemRadius.INSTANCE);
        registerModule(AutoTool.INSTANCE);
        registerModule(ShiftTap.INSTANCE);
        registerModule(KTLeave.INSTANCE);
    }

    private void registerMovement() {
        registerModule(AutoSprint.INSTANCE);
        registerModule(Taksa.INSTANCE);
    }

    private void registerRender() {
        registerModule(Interface.INSTANCE);
        registerModule(Menu.INSTANCE);
        registerModule(SwapNotifications.INSTANCE);
        registerModule(NoRender.INSTANCE);
        registerModule(Predictions.INSTANCE);
        registerModule(SwingAnimation.INSTANCE);
        registerModule(Crosshair.INSTANCE);
        registerModule(ViewModel.INSTANCE);
        registerModule(WorldTweaks.INSTANCE);
        registerModule(EntityESP.INSTANCE);
        registerModule(BlockOverlay.INSTANCE);
        // registerModule(TargetESP.INSTANCE); // скрыт, заменён на TargetESP2
        registerModule(AnimationModule.INSTANCE);
        registerModule(KillEffects.INSTANCE);
        registerModule(Trails.INSTANCE);
        registerModule(ParticlesModule.INSTANCE);
        registerModule(CustomModels.INSTANCE);
        registerModule(JumpCircle.INSTANCE);
        registerModule(ShaderHands.INSTANCE);
        registerModule(NoFluid.INSTANCE);
        registerModule(HitParticles.INSTANCE);
        registerModule(WaypointsModule.INSTANCE);
        registerModule(LineGlyphs.INSTANCE);
        registerModule(Cosmetics.INSTANCE);
        registerModule(SoulESP.INSTANCE);
        registerModule(FireFly.INSTANCE);
        registerModule(TargetESP2.INSTANCE);
        registerModule(FriendMarkers.INSTANCE);
        
        // Включаем модули по умолчанию ПОСЛЕ регистрации всех модулей
        // Используем setToggled(true) чтобы принудительно включить без toggle звука
        if (!Interface.INSTANCE.isEnabled()) {
            Interface.INSTANCE.setToggled(true);
        }
        if (!SwapNotifications.INSTANCE.isEnabled()) {
            SwapNotifications.INSTANCE.setToggled(true);
        }
        // Для Waypoints всегда вызываем onEnable чтобы зарегистрировать события
        if (!WaypointsModule.INSTANCE.isEnabled()) {
            WaypointsModule.INSTANCE.setToggled(true);
        } else {
            // Если модуль уже enabled (из конфига), но события не зарегистрированы
            WaypointsModule.INSTANCE.onEnable();
        }
    }

    private void registerPlayer() {
        // AutoTool moved to Combat
    }

    private void registerMisc() {
        registerModule(ServerHelper.INSTANCE);
        registerModule(ElytraHelper.INSTANCE);
        registerModule(ItemScroller.INSTANCE);
        registerModule(ClickAction.INSTANCE);
        registerModule(CameraTweaks.INSTANCE);
        registerModule(AutoAuth.INSTANCE);
        registerModule(AutoDuels.INSTANCE);
        registerModule(AHHelper.INSTANCE);
        registerModule(AutoAccept.INSTANCE);
        registerModule(AutoRespawn.INSTANCE);
        registerModule(NameProtect.INSTANCE);
        registerModule(Sounds.INSTANCE);
        registerModule(AutoJoin.INSTANCE);
        registerModule(FastExp.INSTANCE);
        registerModule(FakePlayer.INSTANCE);
    }

    private void registerModule(Module module) {
        if (module == null) {
            System.err.println("[ModuleManager] Tried to register null module!");
            return;
        }
        System.out.println("[ModuleManager] Registering: " + module.getName() + " [" + module.getCategory() + "]");
        modules.add(module);
    }


    public Module getModule(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Set<Module> getActiveModules() {
        Set<Module> active = new HashSet<>();
        for (Module module : modules) {
            if (module.isEnabled()) active.add(module);
        }
        return active;
    }


    @EventTarget
    public void onKey(EventKey event) {

        if (mc.currentScreen != null || event.getAction() != GLFW.GLFW_PRESS) return;

        for (Module module : modules) {
            if (module.getKeyCode() == event.getKeyCode()
                    && module.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN) {
                module.toggle();
            }
        }
    }
}
