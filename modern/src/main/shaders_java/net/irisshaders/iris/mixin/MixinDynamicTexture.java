package net.irisshaders.iris.mixin;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.embeddedt.embeddium.compat.mc.MCDynamicTexture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DynamicTexture.class)
public abstract class MixinDynamicTexture implements MCDynamicTexture {
    // Interface Injection
}
