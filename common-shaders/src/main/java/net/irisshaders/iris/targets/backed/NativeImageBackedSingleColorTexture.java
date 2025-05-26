package net.irisshaders.iris.targets.backed;

import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.compat.mc.CommonDynamicTexture;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class NativeImageBackedSingleColorTexture extends CommonDynamicTexture {
	public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
		super(create(ColorABGR.pack(red, green, blue, alpha)));
	}

	public NativeImageBackedSingleColorTexture(int rgba) {
		this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
	}

	private static MCNativeImage create(int color) {
		MCNativeImage image = MINECRAFT_SHIM.createNativeImage(1, 1, false);

		image.setPixelRGBA(0, 0, color);

		return image;
	}
}
