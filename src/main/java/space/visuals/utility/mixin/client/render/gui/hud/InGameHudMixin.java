package space.visuals.utility.mixin.client.render.gui.hud;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.base.animations.base.Animation;
import space.visuals.base.animations.base.Easing;
import space.visuals.base.events.impl.render.EventRender2D;
import space.visuals.client.modules.impl.combat.SwapPlus;
import space.visuals.client.modules.impl.render.AnimationModule;
import space.visuals.client.modules.impl.render.Crosshair;
import space.visuals.client.modules.impl.render.Interface;
import space.visuals.utility.render.display.base.CustomDrawContext;
import space.visuals.utility.render.display.shader.DrawUtil;
import space.visuals.client.modules.api.Module;

import static space.visuals.utility.interfaces.IMinecraft.mc;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Unique private final Animation tabAnim = new Animation(250, 0, Easing.QUAD_OUT);
    @Unique private boolean tabWasVisible = false;
    @Unique private boolean tabPushed = false;

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Рисуем блюр только если Interface включен, блюр активен,
        // и НЕ открыт инвентарь с включённой анимацией
        Interface interfaceModule = Interface.INSTANCE;
        if (interfaceModule.isEnabled() && interfaceModule.isBlur()) {
            boolean inventoryOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
            boolean newGuiOpen = mc.currentScreen instanceof space.visuals.client.screens.newgui.NewClickGui;
            boolean animHidesBlur = AnimationModule.INSTANCE.isEnabled() && AnimationModule.INSTANCE.animateInventory.isEnabled();
            if (!(inventoryOpen && animHidesBlur) && !newGuiOpen) {
                DrawUtil.blurProgram.draw();
            }
        }
        CustomDrawContext customDrawContext = new CustomDrawContext(mc.getBufferBuilders().getEntityVertexConsumers());
        EventManager.call(new EventRender2D(customDrawContext, tickCounter.getTickDelta(false)));
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void removeVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            Module crosshairModule = Crosshair.INSTANCE;
            if (crosshairModule.isEnabled() || SwapPlus.INSTANCE.isWheelOpen()) {
                ci.cancel();
            }
        } catch (Exception e) {
            // PIZDEC
        }
    }


    @Inject(method = "renderMainHud", at = @At(value = "HEAD"), cancellable = true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderExperienceLevel", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderPlayerList", at = @At(value = "HEAD"), cancellable = true)
    private void inject(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Interface interfaceModule = Interface.INSTANCE;
        if (interfaceModule.isEnabled() && interfaceModule.isEnableTab()) {
            ci.cancel();
            return;
        }
        // TAB анимация
        if (AnimationModule.INSTANCE.isEnabled() && AnimationModule.INSTANCE.animateTabList.isEnabled()) {
            boolean visible = mc.options.playerListKey.isPressed();
            if (!tabWasVisible && visible) tabAnim.reset(0);
            tabWasVisible = visible;
            float scale = tabAnim.update(visible ? 1 : 0);
            if (scale <= 0f) { ci.cancel(); tabPushed = false; return; }
            if (scale < 1f) {
                int w = mc.getWindow().getScaledWidth();
                int h = mc.getWindow().getScaledHeight();
                MatrixStack ms = context.getMatrices();
                ms.push();
                // Масштабируем из центра экрана по обеим осям
                ms.translate(w / 2f, h / 2f, 0);
                ms.scale(scale, scale, 1f);
                ms.translate(-w / 2f, -h / 2f, 0);
                tabPushed = true;
            }
        }
    }

    @Inject(method = "renderPlayerList", at = @At(value = "RETURN"))
    private void injectPost(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!tabPushed) return;
        context.getMatrices().pop();
        tabPushed = false;
    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "renderScoreboardSidebar*", at = @At(value = "HEAD"), cancellable = true)
    private void injectRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableScoreBar()) {
                ci.cancel();
            }
    }


    @ModifyVariable(
            method = "renderStatusBars",
            at = @At(value = "STORE"),
            ordinal = 3
    )
    private int modifyM(int original, DrawContext context) {
        if (mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
                return context.getScaledWindowWidth() / 2 + 90 + 36;
            }
        }
        return original;
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaStatusBars(DrawContext context, CallbackInfo ci) {
        if (mc.interactionManager != null && mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            Interface interfaceModule = Interface.INSTANCE;
            if (interfaceModule.isEnabled() && interfaceModule.isEnableStatusBars()) {
                ci.cancel();
            }
        }
    }


}
