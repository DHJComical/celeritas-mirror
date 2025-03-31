package net.irisshaders.iris.pathways;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.helpers.VertexBufferHelper;
import org.embeddedt.embeddium.api.vertex.format.common.TexturedVertex;
import org.lwjgl.system.MemoryStack;

/**
 * Renders a full-screen textured quad to the screen. Used in composite / deferred rendering.
 */
public class FullScreenQuadRenderer {
	public static final FullScreenQuadRenderer INSTANCE = new FullScreenQuadRenderer();

	private final VertexBuffer quad;

	private FullScreenQuadRenderer() {
        quad = BufferHelper.makeStaticBuffer(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX, consumer -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long ptr = stack.nmalloc(TexturedVertex.STRIDE * 4);
                TexturedVertex.put(ptr + 0 * TexturedVertex.STRIDE, 0, 0, 0, 0, 0);
                TexturedVertex.put(ptr + 1 * TexturedVertex.STRIDE, 1, 0, 0, 1, 0);
                TexturedVertex.put(ptr + 2 * TexturedVertex.STRIDE, 1, 1, 0, 1, 1);
                TexturedVertex.put(ptr + 3 * TexturedVertex.STRIDE, 0, 1, 0, 0, 1);
                consumer.push(stack, ptr, 4, TexturedVertex.FORMAT);
            }
        });
	}

	public void render() {
		begin();

		renderQuad();

		end();
	}

	public void begin() {
		((VertexBufferHelper) quad).saveBinding();
		RENDER_SYSTEM.disableDepthTest();
		BufferUploader.reset();
		quad.bind();
	}

	public void renderQuad() {
		IrisRenderSystem.overridePolygonMode();
		quad.draw();
		IrisRenderSystem.restorePolygonMode();
	}

	public void end() {
		// NB: No need to clear the buffer state by calling glDisableVertexAttribArray - this VAO will always
		// have the same format, and buffer state is only associated with a given VAO, so we can keep it bound.
		//
		// Using quad.getFormat().clearBufferState() causes some Intel drivers to freak out:
		// https://github.com/IrisShaders/Iris/issues/1214

		RENDER_SYSTEM.enableDepthTest();
		((VertexBufferHelper) quad).restoreBinding();
	}
}
