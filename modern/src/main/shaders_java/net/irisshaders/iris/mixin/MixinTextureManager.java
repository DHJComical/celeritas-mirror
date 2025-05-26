package net.irisshaders.iris.mixin;

import net.minecraft.client.renderer.texture.TextureManager;
import org.embeddedt.embeddium.compat.mc.MCTextureManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager implements MCTextureManager {
    // Interface Injection
}
