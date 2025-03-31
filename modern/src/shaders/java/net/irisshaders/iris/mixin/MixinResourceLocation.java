package net.irisshaders.iris.mixin;

import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.compat.mc.IResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ResourceLocation.class)
public abstract class MixinResourceLocation implements IResourceLocation {
}
