package net.irisshaders.iris.mixin.texture.pbr;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.irisshaders.iris.texture.pbr.PBRAtlasHolder;
import net.irisshaders.iris.texture.pbr.PBRAtlasTexture;
import net.irisshaders.iris.texture.pbr.TextureAtlasExtension;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class MixinTextureAtlas implements TextureAtlasExtension {
	@Unique
	private PBRAtlasHolder iris$pbrHolder;

	// Before any sprite is drawn into it.
	@Inject(method = "upload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;createTexture(III)V", shift = At.Shift.AFTER))
	private void iris$fillPBRAtlas(CallbackInfo ci) {
		if ((Object) this instanceof PBRAtlasTexture pbrAtlas) {
			pbrAtlas.fillWithDefault();
		}
	}

	// PBR atlases aren't atlases that mods know of.
	@WrapWithCondition(method = "upload", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;onTextureAtlasStitched(Lnet/minecraft/client/renderer/texture/TextureAtlas;)V"))
	private boolean iris$skipPBRStitchEvent(TextureAtlas atlas) {
		return !(atlas instanceof PBRAtlasTexture);
	}

	@Inject(method = "cycleAnimationFrames", at = @At("TAIL"))
	private void iris$cyclePBRAnimationFrames(CallbackInfo ci) {
		if (iris$pbrHolder != null) {
			iris$pbrHolder.cycleAnimationFrames();
		}
	}

	@Override
	public PBRAtlasHolder getPBRHolder() {
		return iris$pbrHolder;
	}

	@Override
	public PBRAtlasHolder getOrCreatePBRHolder() {
		if (iris$pbrHolder == null) {
			iris$pbrHolder = new PBRAtlasHolder();
		}

		return iris$pbrHolder;
	}
}
