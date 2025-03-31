package net.irisshaders.iris.gl.sampler;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;

import net.irisshaders.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL45C;

public class SamplerLimits {
	private static SamplerLimits instance;
	private final int maxTextureUnits;
	private final int maxDrawBuffers;
	private final int maxShaderStorageUnits;

	private SamplerLimits() {
		this.maxTextureUnits = GL_STATE_MANAGER.glGetInteger(GL20C.GL_MAX_TEXTURE_IMAGE_UNITS);
		this.maxDrawBuffers = GL_STATE_MANAGER.glGetInteger(GL20C.GL_MAX_DRAW_BUFFERS);
		this.maxShaderStorageUnits = IrisRenderSystem.supportsSSBO() ? GL_STATE_MANAGER.glGetInteger(GL45C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS) : 0;
	}

	public static SamplerLimits get() {
		if (instance == null) {
			instance = new SamplerLimits();
		}

		return instance;
	}

	public int getMaxTextureUnits() {
		return maxTextureUnits;
	}

	public int getMaxDrawBuffers() {
		return maxDrawBuffers;
	}

	public int getMaxShaderStorageUnits() {
		return maxShaderStorageUnits;
	}
}
