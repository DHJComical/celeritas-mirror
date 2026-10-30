package net.irisshaders.iris.mixin.texture.pbr;

import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public class MixinAbstractTexture {
	// Textures are also closed to be replaced, such as when they're reloaded.
	@Inject(method = "close", at = @At("HEAD"))
	private void iris$closePBRTextures(CallbackInfo ci) {
		PBRTextureManager.INSTANCE.onTextureClosed((AbstractTexture) (Object) this);
	}
}
