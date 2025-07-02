package net.irisshaders.iris.mixin;

import net.minecraft.server.packs.resources.ResourceManager;
import org.embeddedt.embeddium.compat.mc.MCResourceManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ResourceManager.class)
public abstract interface MixinResourceManager extends MCResourceManager {
    // Interface injection
}
