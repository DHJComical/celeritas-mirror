package net.irisshaders.iris.targets.backed;

import com.mojang.blaze3d.platform.NativeImage;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.texture.VanillaTextures;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11C;

import java.util.Objects;
import java.util.Random;
import java.util.function.IntSupplier;

public class NativeImageBackedNoiseTexture extends DynamicTexture implements TextureAccess {
	public NativeImageBackedNoiseTexture(int size) {
		super(/*? if >=26.1 {*//*() -> "iris noise texture", *//*?}*/create(size));

		int id = VanillaTextures.glId(this);
		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
	}

	private static NativeImage create(int size) {
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
		Random random = new Random(0);

		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int color = random.nextInt() | (255 << 24);

				image.setPixelRGBA(x, y, color);
			}
		}

		return image;
	}

	//? if <26.1 {
	@Override
	public void upload() {
		NativeImage image = Objects.requireNonNull(getPixels());

		bind();
		image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), true, false, false, false);
	}
	//?}

	@Override
	public TextureType getType() {
		return TextureType.TEXTURE_2D;
	}

	@Override
	public IntSupplier getTextureId() {
		return () -> VanillaTextures.glId(this);
	}
}
