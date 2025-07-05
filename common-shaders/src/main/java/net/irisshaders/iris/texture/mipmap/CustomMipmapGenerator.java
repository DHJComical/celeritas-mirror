package net.irisshaders.iris.texture.mipmap;

import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.jetbrains.annotations.Nullable;

public interface CustomMipmapGenerator {
    MCNativeImage[] generateMipLevels(MCNativeImage[] image, int mipLevel);

	interface Provider {
		@Nullable
		CustomMipmapGenerator getMipmapGenerator();
	}
}
