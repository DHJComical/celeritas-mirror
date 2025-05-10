package net.irisshaders.iris.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.texture.SpriteContentsExtension;
import net.irisshaders.iris.texture.mipmap.CustomMipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

@Mixin(SpriteContents.class)
public class MixinSpriteContents implements SpriteContentsExtension {
	@Unique
	@Nullable
	private SpriteContents.Ticker createdTicker;

	@Redirect(method = "increaseMipLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/MipmapGenerator;generateMipLevels([Lcom/mojang/blaze3d/platform/NativeImage;I)[Lcom/mojang/blaze3d/platform/NativeImage;"))
	private NativeImage[] iris$redirectMipmapGeneration(NativeImage[] nativeImages, int mipLevel) {
		if (this instanceof CustomMipmapGenerator.Provider provider) {
			CustomMipmapGenerator generator = provider.getMipmapGenerator();
			if (generator != null) {
				try {
                    return (NativeImage[])(Object)generator.generateMipLevels((MCNativeImage[])(Object)nativeImages, mipLevel);
				} catch (Exception e) {
					IRIS_LOGGER.error("ERROR MIPMAPPING", e);
				}
			}
		}
		return MipmapGenerator.generateMipLevels(nativeImages, mipLevel);
	}

	@Inject(method = "createTicker()Lnet/minecraft/client/renderer/texture/SpriteTicker;", at = @At("RETURN"))
	private void onReturnCreateTicker(CallbackInfoReturnable<SpriteTicker> cir) {
		SpriteTicker ticker = cir.getReturnValue();
		if (ticker instanceof SpriteContents.Ticker innerTicker) {
			createdTicker = innerTicker;
		}
	}

	@Override
	@Nullable
	public SpriteContents.Ticker getCreatedTicker() {
		return createdTicker;
	}
}
