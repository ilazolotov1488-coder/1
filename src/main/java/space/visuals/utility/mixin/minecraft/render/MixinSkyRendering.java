package space.visuals.utility.mixin.minecraft.render;

import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SkyRendering.class)
public class MixinSkyRendering {
    // SkyCosmos удалён - миксин оставлен для совместимости
}
