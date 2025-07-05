package net.irisshaders.iris.mixin;

import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ResourceLocation.class)
public abstract class MixinResourceLocation implements MCResourceLocation {
}
