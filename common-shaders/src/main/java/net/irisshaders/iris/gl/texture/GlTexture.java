package net.irisshaders.iris.gl.texture;

import java.nio.ByteBuffer;
import java.util.function.IntSupplier;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.shaderpack.texture.TextureFilteringData;
import org.embeddedt.embeddium.impl.gl.GlObject;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

public class GlTexture extends GlObject implements TextureAccess {
	private final TextureType target;

	public GlTexture(TextureType target, int sizeX, int sizeY, int sizeZ, int internalFormat, int format, int pixelType, byte[] pixels, TextureFilteringData filteringData) {
		this.setHandle(GL_STATE_MANAGER.glGenTextures());
		IrisRenderSystem.bindTextureForSetup(target.getGlType(), handle());

		TextureUploadHelper.resetTextureUploadState();

		ByteBuffer buffer = MemoryUtil.memAlloc(pixels.length);
		buffer.put(pixels);
		buffer.flip();
		target.apply(this.handle(), sizeX, sizeY, sizeZ, internalFormat, format, pixelType, buffer);
		MemoryUtil.memFree(buffer);

		int texture = this.handle();

		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MIN_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MAG_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_WRAP_S, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);

		if (sizeY > 0) {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_WRAP_T, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);
		}

		if (sizeZ > 0) {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL30C.GL_TEXTURE_WRAP_R, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);
		}

		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MAX_LEVEL, 0);
		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MIN_LOD, 0);
		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MAX_LOD, 0);
		IrisRenderSystem.texParameterf(texture, target.getGlType(), GL20C.GL_TEXTURE_LOD_BIAS, 0.0F);

		IrisRenderSystem.bindTextureForSetup(target.getGlType(), 0);

		this.target = target;
	}

	public TextureType getTarget() {
		return target;
	}

	public void bind(int unit) {
		IrisRenderSystem.bindTextureToUnit(target.getGlType(), unit, handle());
	}

	@Override
	public TextureType getType() {
		return target;
	}

	@Override
	public IntSupplier getTextureId() {
		return this::handle;
	}

	@Override
	protected void destroyInternal() {
		GL_STATE_MANAGER.glDeleteTextures(handle());
	}
}
