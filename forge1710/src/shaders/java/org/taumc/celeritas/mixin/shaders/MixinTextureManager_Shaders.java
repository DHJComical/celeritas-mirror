package org.taumc.celeritas.mixin.shaders;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.embeddedt.embeddium.compat.mc.MCResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureManager.class)
public class MixinTextureManager_Shaders implements MCTextureManager {
    @Shadow
    public ITextureObject getTexture(ResourceLocation path) {
        throw new IllegalStateException("Mixin shadow method should not be called");
    }

    @Override
    public MCAbstractTexture getTexture(MCResourceLocation path) {
        return (MCAbstractTexture) getTexture((ResourceLocation) path);
    }
    // Interface Injection
}
