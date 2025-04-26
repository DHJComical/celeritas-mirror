package net.irisshaders.iris.texture.util;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

import net.irisshaders.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class TextureManipulationUtil {
	private static int colorFillFBO = -1;

	public static void fillWithColor(int textureId, int maxLevel, int rgba) {
		if (colorFillFBO == -1) {
			colorFillFBO = GL_STATE_MANAGER.glGenFramebuffers();
		}

		int previousFramebufferId = GL_STATE_MANAGER.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		float[] previousClearColor = new float[4];
		IrisRenderSystem.getFloatv(GL11.GL_COLOR_CLEAR_VALUE, previousClearColor);
		int previousTextureId = GL_STATE_MANAGER.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int[] previousViewport = new int[4];
		IrisRenderSystem.getIntegerv(GL11.GL_VIEWPORT, previousViewport);

		GL_STATE_MANAGER.glBindFramebuffer(GL30.GL_FRAMEBUFFER, colorFillFBO);
		GL_STATE_MANAGER.glClearColor(
			(rgba >> 24 & 0xFF) / 255.0f,
			(rgba >> 16 & 0xFF) / 255.0f,
			(rgba >> 8 & 0xFF) / 255.0f,
			(rgba & 0xFF) / 255.0f
		);
		GL_STATE_MANAGER.bindTexture(textureId);
		for (int level = 0; level <= maxLevel; ++level) {
			int width = GL_STATE_MANAGER.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_WIDTH);
			int height = GL_STATE_MANAGER.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_HEIGHT);
			GL_STATE_MANAGER.glViewport(0, 0, width, height);
			GL_STATE_MANAGER.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, level);
            GL_STATE_MANAGER.clear(GL11.GL_COLOR_BUFFER_BIT, MINECRAFT_SHIM.isOnOSX());
			GL_STATE_MANAGER.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, level);
		}

		GL_STATE_MANAGER.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebufferId);
		GL_STATE_MANAGER.glClearColor(previousClearColor[0], previousClearColor[1], previousClearColor[2], previousClearColor[3]);
		GL_STATE_MANAGER.bindTexture(previousTextureId);
		GL_STATE_MANAGER.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
	}
}
