package space.visuals.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.base.events.impl.player.EventMoveInput;
import space.visuals.client.screens.menu.MenuScreen;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Shadow @Final private GameOptions settings;

    @Unique
    private float abobaGetMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;playerInput:Lnet/minecraft/util/PlayerInput;", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    public void injectInputEvent(CallbackInfo ci) {
        PlayerInput input = this.playerInput;

        // Если открыто меню — читаем клавиши напрямую через GLFW,
        // минуя блокировку Minecraft (который возвращает false для всех клавиш при открытом экране)
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof MenuScreen) {
            long handle = mc.getWindow().getHandle();
            boolean forward  = isGlfwKeyDown(handle, settings.forwardKey.getDefaultKey().getCode());
            boolean backward = isGlfwKeyDown(handle, settings.backKey.getDefaultKey().getCode());
            boolean left     = isGlfwKeyDown(handle, settings.leftKey.getDefaultKey().getCode());
            boolean right    = isGlfwKeyDown(handle, settings.rightKey.getDefaultKey().getCode());
            boolean jump     = isGlfwKeyDown(handle, settings.jumpKey.getDefaultKey().getCode());
            boolean sneak    = isGlfwKeyDown(handle, settings.sneakKey.getDefaultKey().getCode());
            boolean sprint   = isGlfwKeyDown(handle, settings.sprintKey.getDefaultKey().getCode());

            input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        }

        EventMoveInput event = new EventMoveInput(input,
                abobaGetMovementMultiplier(input.forward(), input.backward()),
                abobaGetMovementMultiplier(input.left(), input.right())
        );
        EventManager.call(event);

        if (event.isCancelled()) return;

        this.movementForward  = event.getForward();
        this.movementSideways = event.getStrafe();
        this.playerInput = new PlayerInput(
                this.movementForward > 0,
                this.movementForward < 0,
                this.movementSideways > 0,
                this.movementSideways < 0,
                input.jump(),
                input.sneak(),
                input.sprint()
        );
        ci.cancel();
    }

    /** Читает состояние клавиши напрямую через GLFW (работает даже при открытом экране) */
    @Unique
    private static boolean isGlfwKeyDown(long handle, int keyCode) {
        if (keyCode < 0) return false; // mouse button or unknown
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }
}
