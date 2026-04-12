package space.visuals.utility.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.visuals.Zenith;
import space.visuals.client.screens.CustomTitleScreen;
import space.visuals.utility.render.display.base.BorderRadius;
import space.visuals.utility.render.display.base.color.ColorRGBA;
import space.visuals.utility.render.display.shader.DrawUtil;

@Mixin(value = TitleScreen.class, priority = 900)
public class VanillaTitleScreenMixin {

    private static final Identifier LOGO = Zenith.id("icons/logo_s.png");

    @Unique private float btnHoverAnim = 0f;
    @Unique private long initTime = 0;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        initTime = System.currentTimeMillis();
    }

    // Анимация появления — рисуем ПЕРЕД ванильным контентом (затемнение спадает)
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long elapsed = System.currentTimeMillis() - initTime;
        float fadeIn = Math.min(1f, elapsed / 700f);
        float eased = 1f - (1f - fadeIn) * (1f - fadeIn); // easeOut
        if (eased < 0.99f) {
            int alpha = (int)((1f - eased) * 230);
            ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(),
                new ColorRGBA(0, 0, 0, alpha).getRGB());
        }
    }

    // Кнопка — рисуем ПОСЛЕ ванильного контента
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        long elapsed = System.currentTimeMillis() - initTime;
        float fadeIn = Math.min(1f, elapsed / 500f);
        float eased = 1f - (1f - fadeIn) * (1f - fadeIn);

        float btnSz = 36;
        float btnX = mc.getWindow().getScaledWidth() - btnSz - 12;
        float btnY = mc.getWindow().getScaledHeight() - btnSz - 12;
        boolean hov = mouseX >= btnX && mouseX <= btnX+btnSz && mouseY >= btnY && mouseY <= btnY+btnSz;

        float targetHov = hov ? 1f : 0f;
        btnHoverAnim += (targetHov - btnHoverAnim) * 0.15f;

        int bgAlpha = (int)((10 + 15 * btnHoverAnim) * eased);
        int brAlpha = (int)((30 + 30 * btnHoverAnim) * eased);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, BorderRadius.all(10),
            new ColorRGBA(255,255,255, bgAlpha));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), btnX, btnY, btnSz, btnSz, -0.1f, BorderRadius.all(10),
            new ColorRGBA(255,255,255, brAlpha));

        float iconAlpha = (0.65f + 0.35f * btnHoverAnim) * eased;
        DrawUtil.drawRoundedTexture(ctx.getMatrices(), LOGO,
            btnX + 3, btnY + 3, btnSz - 6, btnSz - 6,
            BorderRadius.all(7), ColorRGBA.WHITE.mulAlpha(iconAlpha));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        float btnSz = 36;
        float btnX = mc.getWindow().getScaledWidth() - btnSz - 12;
        float btnY = mc.getWindow().getScaledHeight() - btnSz - 12;

        if (button == 0 && mouseX >= btnX && mouseX <= btnX+btnSz && mouseY >= btnY && mouseY <= btnY+btnSz) {
            Zenith.useVanillaMenu = false;
            mc.setScreen(new CustomTitleScreen());
            cir.setReturnValue(true);
        }
    }
}
