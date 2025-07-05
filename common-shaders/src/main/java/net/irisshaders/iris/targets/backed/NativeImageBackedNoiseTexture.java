package net.irisshaders.iris.targets.backed;

import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import org.embeddedt.embeddium.compat.mc.CommonDynamicTexture;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;

import java.util.Objects;
import java.util.Random;
import java.util.function.IntSupplier;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class NativeImageBackedNoiseTexture extends CommonDynamicTexture implements TextureAccess {
	public NativeImageBackedNoiseTexture(int size) {
		super(create(size));
	}

	private static MCNativeImage create(int size) {
        MCNativeImage image = MINECRAFT_SHIM.createNativeImage(size, size, false);
		Random random = new Random(0);

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int color = random.nextInt() | (255 << 24);

				image.setPixelRGBA(x, y, color);
			}
		}

		return image;
	}

	@Override
	public void upload() {
		MCNativeImage image = Objects.requireNonNull(getPixels());

		bind();
		image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), true, false, false, false);
	}

	@Override
	public TextureType getType() {
		return TextureType.TEXTURE_2D;
	}

	@Override
	public IntSupplier getTextureId() {
		return this::getId;
	}
}
