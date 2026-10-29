package net.irisshaders.iris.targets.backed;

import com.mojang.blaze3d.platform.NativeImage;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.texture.VanillaTextures;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.IntSupplier;

public class NativeImageBackedCustomTexture extends DynamicTexture implements TextureAccess {
	public NativeImageBackedCustomTexture(CustomTextureData.PngData textureData) throws IOException {
		super(/*? if >=26.1 {*//*() -> "iris custom texture", *//*?}*/create(textureData.getContent()));

		// By default, images are unblurred and not clamped.
		int id = VanillaTextures.glId(this);
		int filter = textureData.getFilteringData().shouldBlur() ? GL11C.GL_LINEAR : GL11C.GL_NEAREST;
		int wrap = textureData.getFilteringData().shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL11C.GL_REPEAT;

		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, filter);
		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, filter);
		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, wrap);
		IrisRenderSystem.texParameteri(id, GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, wrap);
	}

	private static NativeImage create(byte[] content) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocateDirect(content.length);
		buffer.put(content);
		buffer.flip();

		return NativeImage.read(buffer);
	}

	//? if <26.1 {
	@Override
	public void upload() {
		NativeImage image = Objects.requireNonNull(getPixels());

		bind();
		image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), false, false, false, false);
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
