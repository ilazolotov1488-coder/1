package space.visuals.utility.mixin.accessors;

import net.minecraft.client.gl.ShaderLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderLoader.class)
public interface ShaderLoaderCacheAccessor {
    @Accessor("cache")
    ShaderLoader.Cache getCache();
}
