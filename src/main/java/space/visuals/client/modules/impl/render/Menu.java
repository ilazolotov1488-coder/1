package space.visuals.client.modules.impl.render;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.glfw.GLFW;
import space.visuals.Zenith;

import space.visuals.base.events.impl.input.EventSetScreen;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.events.impl.render.EventRender2D;
import space.visuals.base.events.impl.render.EventRenderScreen;
import space.visuals.client.modules.api.Category;
import space.visuals.client.modules.api.Module;
import space.visuals.client.modules.api.ModuleAnnotation;
import space.visuals.client.screens.menu.MenuScreen;
import space.visuals.client.screens.menu.settings.impl.MenuSliderSetting;
import space.visuals.utility.render.display.base.UIContext;

import java.awt.event.KeyEvent;

@ModuleAnnotation(name = "Menu", category = Category.RENDER, description = "Меню чита")
public final class Menu extends Module {
    public static final Menu INSTANCE = new Menu();

    private Menu() {
        this.setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.world == null) {
            this.setEnabled(false);
            return;
        }

        if (mc.currentScreen == Zenith.getInstance().getMenuScreen()) return;

        mc.setScreen(Zenith.getInstance().getMenuScreen());

        // только звук открытия гуи, без toggle-звука из super
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
        if(keyCode == -1) return;
        super.setKeyCode(keyCode);
    }

    @EventTarget
    public void render2d(EventRenderScreen eventRender2D){
        UIContext uiContext =eventRender2D.getContext();
        Zenith.getInstance().getMenuScreen().renderTop(uiContext,uiContext.getMouseX(),uiContext.getMouseY());
        if(Zenith.getInstance().getMenuScreen().isFinish()){
            this.toggle();
        }
    }

}
