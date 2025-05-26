package org.taumc.celeritas.mixin.shaders;

import net.minecraft.client.resources.IResourceManager;
import org.embeddedt.embeddium.compat.mc.MCResourceManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IResourceManager.class)
public interface MixinIResourceManager_Shaders extends MCResourceManager {
    // Interface injection
}
