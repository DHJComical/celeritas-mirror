package net.irisshaders.iris.mixin;

import net.minecraft.server.packs.resources.ResourceProvider;
import org.embeddedt.embeddium.compat.mc.IResourceProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ResourceProvider.class)
public interface MixinResourceProvider extends IResourceProvider {
}
