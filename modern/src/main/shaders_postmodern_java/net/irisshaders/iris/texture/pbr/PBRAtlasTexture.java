package net.irisshaders.iris.texture.pbr;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The normal or specular counterpart of a texture atlas. Its sprites are placed where their base sprites are, and it
 * animates in step with the base atlas, which cycles its frames.
 */
public class PBRAtlasTexture extends TextureAtlas {
	private final TextureAtlas baseAtlas;
	private final PBRType type;

	public PBRAtlasTexture(TextureAtlas baseAtlas, PBRType type) {
		super(location(baseAtlas, type));
		this.baseAtlas = baseAtlas;
		this.type = type;
	}

	public static Identifier location(TextureAtlas baseAtlas, PBRType type) {
		return baseAtlas.location().withPath(path -> path.replace(".png", "") + type.getSuffix() + ".png");
	}

	public PBRType getType() {
		return type;
	}

	/**
	 * Fills the atlas with the type's default value, which is what sprites without PBR textures sample.
	 */
	public void fillWithDefault() {
		GpuTexture texture = getTexture();
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		int rgba = type.getDefaultValue();
		int argb = rgba >>> 8 | rgba << 24;

		for (int level = 0; level < texture.getMipLevels(); level++) {
			try (NativeImage image = new NativeImage(texture.getWidth(level), texture.getHeight(level), false)) {
				image.fillRect(0, 0, image.getWidth(), image.getHeight(), argb);
				encoder.writeToTexture(texture, image, level, 0, 0, 0, image.getWidth(), image.getHeight(), 0, 0);
			}
		}
	}

	@Override
	public void close() {
		PBRAtlasHolder holder = ((TextureAtlasExtension) baseAtlas).getPBRHolder();

		if (holder != null) {
			switch (type) {
				case NORMAL -> {
					if (holder.getNormalAtlas() == this) {
						holder.setNormalAtlas(null);
					}
				}
				case SPECULAR -> {
					if (holder.getSpecularAtlas() == this) {
						holder.setSpecularAtlas(null);
					}
				}
			}
		}

		super.close();
	}

	/**
	 * Moves an animation of a PBR sprite to where its base sprite's animation is, in ticks since its first frame.
	 */
	public static void syncAnimation(SpriteContents.AnimationState source, SpriteContents.AnimationState target) {
		List<SpriteContents.FrameInfo> sourceFrames = source.animationInfo.frames;
		int ticks = source.subFrame;

		for (int f = 0; f < source.frame; f++) {
			ticks += sourceFrames.get(f).time();
		}

		List<SpriteContents.FrameInfo> targetFrames = target.animationInfo.frames;
		int cycleTime = 0;

		for (SpriteContents.FrameInfo frame : targetFrames) {
			cycleTime += frame.time();
		}

		ticks %= cycleTime;
		int targetFrame = 0;

		while (ticks >= targetFrames.get(targetFrame).time()) {
			ticks -= targetFrames.get(targetFrame).time();
			targetFrame++;
		}

		target.frame = targetFrame;
		target.subFrame = ticks;
	}
}
