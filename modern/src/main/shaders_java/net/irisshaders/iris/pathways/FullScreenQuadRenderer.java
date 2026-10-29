package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.platform.GlStateManager;
//? if <26.1 {
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
//?}
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Renders a full-screen textured quad to the screen. Used in composite / deferred rendering.
 * <p>
 * The quad provides {@code Position} at attribute 0 and {@code UV0} at attribute 1, matching
 * {@link net.irisshaders.iris.gl.shader.ProgramCreator#DEFAULT_ATTRIBUTE_LOCATIONS}.
 */
public class FullScreenQuadRenderer {
	public static final FullScreenQuadRenderer INSTANCE = new FullScreenQuadRenderer();

	private static final int POSITION_LOCATION = 0;
	private static final int UV_LOCATION = 1;
	private static final int STRIDE = 5 * Float.BYTES;

	private final int vertexArray;
	private final int vertexBuffer;

	private FullScreenQuadRenderer() {
		vertexArray = GL30C.glGenVertexArrays();
		vertexBuffer = GL15C.glGenBuffers();

		GL30C.glBindVertexArray(vertexArray);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vertexBuffer);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			// x, y, z, u, v for each corner, in triangle strip order.
			FloatBuffer vertices = stack.floats(
				0, 0, 0, 0, 0,
				1, 0, 0, 1, 0,
				0, 1, 0, 0, 1,
				1, 1, 0, 1, 1
			);

			GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
		}

		GL20C.glEnableVertexAttribArray(POSITION_LOCATION);
		GL20C.glVertexAttribPointer(POSITION_LOCATION, 3, GL11C.GL_FLOAT, false, STRIDE, 0);
		GL20C.glEnableVertexAttribArray(UV_LOCATION);
		GL20C.glVertexAttribPointer(UV_LOCATION, 2, GL11C.GL_FLOAT, false, STRIDE, 3 * Float.BYTES);

		GL30C.glBindVertexArray(0);
		GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
	}

	public void render() {
		begin();

		renderQuad();

		end();
	}

	public void begin() {
		GlStateManager._disableDepthTest();
		GL30C.glBindVertexArray(vertexArray);
	}

	public void renderQuad() {
		IrisRenderSystem.overridePolygonMode();
		GL11C.glDrawArrays(GL11C.GL_TRIANGLE_STRIP, 0, 4);
		IrisRenderSystem.restorePolygonMode();
	}

	public void end() {
		GlStateManager._enableDepthTest();
		GL30C.glBindVertexArray(0);

		//? if <26.1 {
		// Vanilla caches its bound vertex array and immediate-mode buffer.
		VertexBuffer.unbind();
		BufferUploader.reset();
		//?}
	}
}
