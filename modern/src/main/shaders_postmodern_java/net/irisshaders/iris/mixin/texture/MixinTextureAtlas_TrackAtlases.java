package net.irisshaders.iris.mixin.texture;

import net.irisshaders.iris.texture.AtlasTextures;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas_TrackAtlases {
	@Inject(method = "createTexture", at = @At("RETURN"))
	private void iris$trackAtlas(int width, int height, int mipLevel, CallbackInfo ci) {
		AtlasTextures.add(((TextureAtlas) (Object) this).getTexture());
	}
}
