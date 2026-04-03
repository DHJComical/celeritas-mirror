package org.embeddedt.embeddium.impl.mixin.core.debug;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.texture.NameableTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
    @Inject(method = "register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V", at = @At("RETURN"))
    private void setName(ResourceLocation location, AbstractTexture texture, CallbackInfo ci) {
        if (texture instanceof NameableTexture nameable) {
            nameable.celeritas$setName(location.toString());
        }
    }
}
