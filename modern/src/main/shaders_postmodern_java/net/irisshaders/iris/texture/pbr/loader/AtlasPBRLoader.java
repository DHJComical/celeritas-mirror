package net.irisshaders.iris.texture.pbr.loader;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import net.irisshaders.iris.texture.format.TextureFormat;
import net.irisshaders.iris.texture.format.TextureFormatLoader;
import net.irisshaders.iris.texture.mipmap.ChannelMipmapGenerator;
import net.irisshaders.iris.texture.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.texture.mipmap.LinearBlendFunction;
import net.irisshaders.iris.texture.pbr.PBRAtlasHolder;
import net.irisshaders.iris.texture.pbr.PBRAtlasTexture;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.TextureAtlasExtension;
import net.irisshaders.iris.texture.util.ImageManipulationUtil;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AtlasPBRLoader implements PBRTextureLoader<TextureAtlas> {
	public static final ChannelMipmapGenerator LINEAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(
		LinearBlendFunction.INSTANCE,
		LinearBlendFunction.INSTANCE,
		LinearBlendFunction.INSTANCE,
		LinearBlendFunction.INSTANCE
	);

	@Override
	public void load(TextureAtlas atlas, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer) {
		PBRAtlasTexture normalAtlas = loadAtlas(atlas, resourceManager, PBRType.NORMAL);
		PBRAtlasTexture specularAtlas = loadAtlas(atlas, resourceManager, PBRType.SPECULAR);
		PBRAtlasHolder holder = ((TextureAtlasExtension) atlas).getOrCreatePBRHolder();

		if (normalAtlas != null) {
			holder.setNormalAtlas(normalAtlas);
			pbrTextureConsumer.acceptNormalTexture(normalAtlas);
		}

		if (specularAtlas != null) {
			holder.setSpecularAtlas(specularAtlas);
			pbrTextureConsumer.acceptSpecularTexture(specularAtlas);
		}
	}

	@Nullable
	private static PBRAtlasTexture loadAtlas(TextureAtlas atlas, ResourceManager resourceManager, PBRType type) {
		GpuTexture texture = atlas.getTexture();
		int width = texture.getWidth(0);
		int height = texture.getHeight(0);
		int mipLevel = texture.getMipLevels() - 1;
		Identifier location = PBRAtlasTexture.location(atlas, type);
		Map<Identifier, TextureAtlasSprite> sprites = new HashMap<>();

		for (TextureAtlasSprite sprite : atlas.getTextures().values()) {
			TextureAtlasSprite pbrSprite = createPBRSprite(sprite, resourceManager, location, width, height, mipLevel, type);

			if (pbrSprite != null) {
				sprites.put(pbrSprite.contents().name(), pbrSprite);
			}
		}

		if (sprites.isEmpty()) {
			return null;
		}

		// Vanilla's atlases must have a missing sprite. It's drawn where the base atlas's is, so that it covers nothing else.
		TextureAtlasSprite baseMissing = atlas.missingSprite();
		TextureAtlasSprite missing = createDefaultSprite(baseMissing, location, width, height, mipLevel, type);
		sprites.put(missing.contents().name(), missing);

		PBRAtlasTexture pbrAtlas = new PBRAtlasTexture(atlas, type);

		try {
			pbrAtlas.upload(new SpriteLoader.Preparations(width, height, mipLevel, missing, sprites, CompletableFuture.completedFuture(null)));
		} catch (Throwable t) {
			CeleritasShaders.logger().error("Failed to create the {} atlas of {}", type, atlas.location(), t);
			pbrAtlas.close();
			return null;
		}

		return pbrAtlas;
	}

	private static TextureAtlasSprite createDefaultSprite(TextureAtlasSprite baseSprite, Identifier atlasLocation, int atlasWidth,
														  int atlasHeight, int mipLevel, PBRType type) {
		int width = baseSprite.contents().width();
		int height = baseSprite.contents().height();
		NativeImage image = new NativeImage(width, height, false);
		int rgba = type.getDefaultValue();
		image.fillRect(0, 0, width, height, rgba >>> 8 | rgba << 24);

		PBRSpriteContents contents = new PBRSpriteContents(MissingTextureAtlasSprite.getLocation(), new FrameSize(width, height), image,
			Optional.empty(), type, baseSprite.contents());
		contents.increaseMipLevel(mipLevel);

		return new PBRTextureAtlasSprite(atlasLocation, contents, atlasWidth, atlasHeight, baseSprite.getX(), baseSprite.getY(), baseSprite.padding);
	}

	@Nullable
	private static TextureAtlasSprite createPBRSprite(TextureAtlasSprite sprite, ResourceManager resourceManager, Identifier atlasLocation,
													  int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType) {
		Identifier spriteName = sprite.contents().name();
		Identifier pbrImageLocation = getPBRImageLocation(spriteName, pbrType);

		Optional<Resource> optionalResource = resourceManager.getResource(pbrImageLocation);

		if (optionalResource.isEmpty()) {
			return null;
		}

		if (PBRType.hasDirectionalSiblings(pbrImageLocation, resourceManager)) {
			// Looks like a cardinal-direction texture set (e.g. "_n"/"_s"/"_e"/"_w" for block faces),
			// not an actual PBR map. Don't treat it as one.
			return null;
		}

		Resource resource = optionalResource.get();
		Optional<AnimationMetadataSection> animation;

		try {
			animation = resource.metadata().getSection(AnimationMetadataSection.TYPE);
		} catch (Exception e) {
			CeleritasShaders.logger().error("Unable to parse metadata from {}", pbrImageLocation, e);
			return null;
		}

		NativeImage nativeImage;

		try (InputStream stream = resource.open()) {
			nativeImage = NativeImage.read(stream);
		} catch (IOException e) {
			CeleritasShaders.logger().error("Using missing texture, unable to load {}", pbrImageLocation, e);
			return null;
		}

		int imageWidth = nativeImage.getWidth();
		int imageHeight = nativeImage.getHeight();

		if (animation.isEmpty() && sprite.contents().isAnimated()) {
			animation = baseAnimation(sprite, resourceManager, imageWidth, imageHeight);
		}

		FrameSize frameSize = animation.map(a -> a.calculateFrameSize(imageWidth, imageHeight)).orElse(new FrameSize(imageWidth, imageHeight));
		int frameWidth = frameSize.width();
		int frameHeight = frameSize.height();

		if (!Mth.isMultipleOf(imageWidth, frameWidth) || !Mth.isMultipleOf(imageHeight, frameHeight)) {
			CeleritasShaders.logger().error("Image {} size {},{} is not multiple of frame size {},{}", pbrImageLocation, imageWidth, imageHeight, frameWidth, frameHeight);
			nativeImage.close();
			return null;
		}

		int targetFrameWidth = sprite.contents().width();
		int targetFrameHeight = sprite.contents().height();

		if (frameWidth != targetFrameWidth || frameHeight != targetFrameHeight) {
			int targetImageWidth = imageWidth / frameWidth * targetFrameWidth;
			int targetImageHeight = imageHeight / frameHeight * targetFrameHeight;

			NativeImage scaledImage;
			if (targetImageWidth % imageWidth == 0 && targetImageHeight % imageHeight == 0) {
				scaledImage = ImageManipulationUtil.scaleNearestNeighbor(nativeImage, targetImageWidth, targetImageHeight);
			} else {
				scaledImage = ImageManipulationUtil.scaleBilinear(nativeImage, targetImageWidth, targetImageHeight);
			}

			nativeImage.close();
			nativeImage = scaledImage;
			frameWidth = targetFrameWidth;
			frameHeight = targetFrameHeight;
			int scaledWidth = frameWidth;
			int scaledHeight = frameHeight;
			animation = animation.map(a -> new AnimationMetadataSection(a.frames(), a.frameWidth().map(w -> scaledWidth),
				a.frameHeight().map(h -> scaledHeight), a.defaultFrameTime(), a.interpolatedFrames()));
		}

		Identifier pbrSpriteName = spriteName.withPath(spriteName.getPath() + pbrType.getSuffix());
		PBRSpriteContents contents = new PBRSpriteContents(pbrSpriteName, new FrameSize(frameWidth, frameHeight), nativeImage, animation, pbrType, sprite.contents());
		contents.increaseMipLevel(mipLevel);

		return new PBRTextureAtlasSprite(atlasLocation, contents, atlasWidth, atlasHeight, sprite.getX(), sprite.getY(), sprite.padding);
	}

	/**
	 * Packs often leave out the animation metadata of animated PBR images, whose frames are laid out like their base
	 * sprite's. These animate like the base sprite, if they're proportioned like it.
	 */
	private static Optional<AnimationMetadataSection> baseAnimation(TextureAtlasSprite sprite, ResourceManager resourceManager,
																	 int imageWidth, int imageHeight) {
		NativeImage baseImage = sprite.contents().getOriginalImage();
		int baseWidth = baseImage.getWidth();
		int baseHeight = baseImage.getHeight();

		if ((long) imageWidth * baseHeight != (long) imageHeight * baseWidth || imageWidth % baseWidth != 0 && baseWidth % imageWidth != 0) {
			return Optional.empty();
		}

		Optional<AnimationMetadataSection> baseAnimation;

		try {
			baseAnimation = resourceManager.getResource(getPBRImageLocation(sprite.contents().name(), null))
				.flatMap(resource -> {
					try {
						return resource.metadata().getSection(AnimationMetadataSection.TYPE);
					} catch (IOException e) {
						return Optional.empty();
					}
				});
		} catch (Exception e) {
			return Optional.empty();
		}

		// Frame sizes are in the base image's pixels.
		int frameWidth = sprite.contents().width() * imageWidth / baseWidth;
		int frameHeight = sprite.contents().height() * imageHeight / baseHeight;

		return baseAnimation.map(a -> new AnimationMetadataSection(a.frames(), Optional.of(frameWidth), Optional.of(frameHeight),
			a.defaultFrameTime(), a.interpolatedFrames()));
	}

	private static Identifier getPBRImageLocation(Identifier spriteName, @Nullable PBRType pbrType) {
		String path = pbrType != null ? pbrType.appendSuffix(spriteName.getPath()) : spriteName.getPath();
		// Temporary fix for CIT Resewn. CIT Resewn has sprites that are not in the textures/ folder, so a custom check must be used here to avoid that assumption.
		if (path.startsWith("optifine/cit/")) {
			return spriteName.withPath(path + ".png");
		}
		return spriteName.withPath("textures/" + path + ".png");
	}

	/**
	 * The contents of a PBR sprite, which animate in step with its base sprite's.
	 */
	public static class PBRSpriteContents extends SpriteContents implements CustomMipmapGenerator.Provider {
		private final PBRType pbrType;
		private final SpriteContents baseContents;

		public PBRSpriteContents(Identifier name, FrameSize size, NativeImage image, Optional<AnimationMetadataSection> animation,
								 PBRType pbrType, SpriteContents baseContents) {
			super(name, size, image, animation, List.of(), Optional.empty());
			this.pbrType = pbrType;
			this.baseContents = baseContents;
		}

		public SpriteContents getBaseContents() {
			return baseContents;
		}

		@Override
		public CustomMipmapGenerator getMipmapGenerator() {
			TextureFormat format = TextureFormatLoader.getFormat();

			if (format != null) {
				CustomMipmapGenerator generator = format.getMipmapGenerator(pbrType);

				if (generator != null) {
					return generator;
				}
			}

			return LINEAR_MIPMAP_GENERATOR;
		}
	}

	private static class PBRTextureAtlasSprite extends TextureAtlasSprite {
		PBRTextureAtlasSprite(Identifier atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, int padding) {
			super(atlasLocation, contents, atlasWidth, atlasHeight, x, y, padding);
		}
	}
}
