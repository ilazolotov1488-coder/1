package space.visuals.utility.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.Interface;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Shadow @Final
    private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, CallbackInfo ci) {
        if (!Interface.INSTANCE.isEnabled()) return;
        if (!Interface.INSTANCE.isEnableDynamicIsland()) return;

        boolean hasPvp = bossBars.values().stream().anyMatch(bar -> {
            String name = bar.getName().getString().toLowerCase();
            return name.contains("pvp") || name.contains("пвп");
        });

        if (hasPvp) {
            ci.cancel();
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(0f, 48f, 0f);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, CallbackInfo ci) {
        if (!Interface.INSTANCE.isEnabled()) return;
        if (!Interface.INSTANCE.isEnableDynamicIsland()) return;

        boolean hasPvp = bossBars.values().stream().anyMatch(bar -> {
            String name = bar.getName().getString().toLowerCase();
            return name.contains("pvp") || name.contains("пвп");
        });

        if (!hasPvp) {
            context.getMatrices().pop();
        }
    }
}
