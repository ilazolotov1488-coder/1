package space.visuals.utility.mixin.client;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.visuals.client.modules.impl.render.NoRender;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void removeBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveBreakParticles()) ci.cancel();
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void removeBreakingParticles(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (NoRender.INSTANCE.isRemoveBreakParticles()) ci.cancel();
    }
}
