package net.irisshaders.iris.mixin.texture.pbr;

import net.irisshaders.iris.texture.format.TextureFormatLoader;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class MixinTextureManager {
	@Shadow
	@Final
	private ResourceManager resourceManager;

	// Once the reloaded textures are applied.
	@Inject(method = "lambda$reload$2", at = @At("TAIL"))
	private void iris$reloadPBR(CallbackInfo ci) {
		TextureFormatLoader.reload(resourceManager);
		PBRTextureManager.INSTANCE.clear();
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void iris$closePBR(CallbackInfo ci) {
		PBRTextureManager.INSTANCE.close();
	}
}
