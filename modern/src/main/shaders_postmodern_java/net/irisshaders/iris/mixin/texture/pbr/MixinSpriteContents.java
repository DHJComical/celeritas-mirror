package net.irisshaders.iris.mixin.texture.pbr;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import net.irisshaders.iris.texture.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.texture.pbr.AnimatedSpriteContents;
import net.irisshaders.iris.texture.pbr.PBRAtlasTexture;
import net.irisshaders.iris.texture.pbr.loader.AtlasPBRLoader;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.taumc.celeritas.shaders.CeleritasShaders;

@Mixin(SpriteContents.class)
public class MixinSpriteContents implements AnimatedSpriteContents {
	@Unique
	@Nullable
	private SpriteContents.AnimationState iris$animationState;

	@WrapOperation(method = "increaseMipLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/MipmapGenerator;generateMipLevels(Lnet/minecraft/resources/Identifier;[Lcom/mojang/blaze3d/platform/NativeImage;ILnet/minecraft/client/renderer/texture/MipmapStrategy;FLcom/mojang/blaze3d/platform/Transparency;)[Lcom/mojang/blaze3d/platform/NativeImage;"))
	private NativeImage[] iris$generateCustomMipmaps(Identifier name, NativeImage[] images, int mipLevel, MipmapStrategy strategy,
													 float alphaCutoffBias, Transparency transparency, Operation<NativeImage[]> original) {
		if (this instanceof CustomMipmapGenerator.Provider provider) {
			CustomMipmapGenerator generator = provider.getMipmapGenerator();

			if (generator != null) {
				try {
					return generator.generateMipLevels(images, mipLevel);
				} catch (Exception e) {
					CeleritasShaders.logger().error("Failed to generate mipmaps for {}", name, e);
				}
			}
		}

		return original.call(name, images, mipLevel, strategy, alphaCutoffBias, transparency);
	}

	// Before the atlas draws the first frame, so that it draws the synced one.
	@Inject(method = "createAnimationState", at = @At("RETURN"))
	private void iris$trackAnimation(CallbackInfoReturnable<SpriteContents.AnimationState> cir) {
		SpriteContents.AnimationState state = cir.getReturnValue();
		iris$animationState = state;

		if (state != null && (Object) this instanceof AtlasPBRLoader.PBRSpriteContents pbrContents) {
			SpriteContents.AnimationState baseState = ((AnimatedSpriteContents) pbrContents.getBaseContents()).iris$getAnimationState();

			if (baseState != null) {
				PBRAtlasTexture.syncAnimation(baseState, state);
			}
		}
	}

	@Override
	@Nullable
	public SpriteContents.AnimationState iris$getAnimationState() {
		return iris$animationState;
	}
}
