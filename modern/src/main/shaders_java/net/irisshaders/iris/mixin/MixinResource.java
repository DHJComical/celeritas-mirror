package net.irisshaders.iris.mixin;

import net.minecraft.server.packs.resources.Resource;
import org.embeddedt.embeddium.compat.mc.IResource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Resource.class)
public abstract class MixinResource implements IResource {
}
