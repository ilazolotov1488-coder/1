package space.visuals.utility.game.player;


import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import space.visuals.Zenith;
import space.visuals.base.events.impl.player.EventUpdate;
import space.visuals.base.request.ScriptManager;
import space.visuals.utility.interfaces.IMinecraft;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public ScriptManager.ScriptTask script = new ScriptManager.ScriptTask();
    public boolean canMove = true;


    public void addTask(Runnable task) {
        if (mc.player == null) return;

        // Не добавляем если предыдущая задача ещё выполняется
        if (!Zenith.getInstance().getScriptManager().isFinished()) return;

        ScriptManager.ScriptTask newScript = new ScriptManager.ScriptTask();
        Zenith.getInstance().getScriptManager().addTask(newScript);

        newScript.schedule(EventUpdate.class, eventUpdate -> {
            if (mc.player != null) {
                // Сбрасываем модификаторы чтобы Ctrl/Shift не ломали свап
                mc.options.sneakKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
                task.run();
            }
            return true;
        });
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
        // Сбрасываем модификаторы
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }

    public boolean shouldSkipExecution() {
        if (mc.currentScreen == null) return false;
        
        if (PlayerIntersectionUtil.isChat(mc.currentScreen)) return false;
        if (mc.currentScreen instanceof SignEditScreen) return false;
        if (mc.currentScreen instanceof AnvilScreen) return false;
        if (mc.currentScreen instanceof AbstractCommandBlockScreen) return false;
        if (mc.currentScreen instanceof StructureBlockScreen) return false;
        // if (mc.currentScreen instanceof MenuScreen) return false;
        
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            int slotCount = mc.player.currentScreenHandler.slots.size();
            return slotCount >= 27;
        }
        
        return false;
    }
}
