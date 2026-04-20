package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;

import space.visuals.base.events.impl.render.EventRenderScreen;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.modules.api.setting.impl.ModeSetting;
import space.visuals.client.screens.menu.MenuScreen;
import space.visuals.client.screens.newgui.NewClickGui;
import space.visuals.utility.render.display.base.UIContext;

@ModuleAnnotation(name = "Menu", category = Category.RENDER, description = "Меню чита")
public final class Menu extends Module {
    public static final Menu INSTANCE = new Menu();

    /** Режим GUI: Zenith (оригинальный) или Новый */
    public final ModeSetting guiMode = new ModeSetting("GUI Mode", "Zenith", "Новый");

    @Getter
    private NewClickGui newClickGui;

    private Menu() {
        this.setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.world == null) {
            this.setEnabled(false);
            return;
        }

        if (guiMode.is("Новый")) {
            // Новый GUI
            if (newClickGui == null) {
                newClickGui = new NewClickGui();
            }
            if (mc.currentScreen == newClickGui) return;
            mc.setScreen(newClickGui);
        } else {
            // Оригинальный Zenith GUI
            if (mc.currentScreen == Zenith.getInstance().getMenuScreen()) return;
            mc.setScreen(Zenith.getInstance().getMenuScreen());
        }

        EventManager.register(this);
        EventManager.call(new space.visuals.base.events.impl.other.EventModuleToggle(this, isEnabled()));
        try {
            if (mc.getSoundManager() != null) {
                float vol = space.visuals.client.modules.impl.misc.Sounds.INSTANCE.volumeGuiOpen.getCurrent();
                space.visuals.utility.sounds.ClientSounds.CLICKGUI_OPEN.play(vol);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDisable() {
        EventManager.unregister(this);
        EventManager.call(new space.visuals.base.events.impl.other.EventModuleToggle(this, isEnabled()));
    }

    @Override
    public void setKeyCode(int keyCode) {
        if (keyCode == -1) return;
        super.setKeyCode(keyCode);
    }

    @EventTarget
    public void render2d(EventRenderScreen eventRender2D) {
        UIContext uiContext = eventRender2D.getContext();

        if (guiMode.is("Новый")) {
            if (newClickGui != null) {
                newClickGui.renderTop(uiContext, uiContext.getMouseX(), uiContext.getMouseY());
                if (newClickGui.isFinish()) {
                    this.toggle();
                }
            }
        } else {
            Zenith.getInstance().getMenuScreen().renderTop(uiContext, uiContext.getMouseX(), uiContext.getMouseY());
            if (Zenith.getInstance().getMenuScreen().isFinish()) {
                this.toggle();
            }
        }
    }
}
