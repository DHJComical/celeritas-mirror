package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.common.PositionVertex;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

/**
 * Renders the sky horizon. Vanilla Minecraft simply uses the "clear color" for its horizon, and then draws a plane
 * above the player. This class extends the sky rendering so that an octagonal prism is drawn around the player instead,
 * allowing shaders to perform more advanced sky rendering.
 * <p>
 * However, the horizon rendering is designed so that when sky shaders are not being used, it looks almost exactly the
 * same as vanilla sky rendering, except a few almost entirely imperceptible differences where the walls
 * of the octagonal prism intersect the top plane.
 */
public class HorizonRenderer {
	/**
	 * The Y coordinate of the top skybox plane. Acts as the upper bound for the horizon prism, since the prism lies
	 * between the bottom and top skybox planes.
	 */
	private static final float TOP = 16.0F;

	/**
	 * The Y coordinate of the bottom skybox plane. Acts as the lower bound for the horizon prism, since the prism lies
	 * between the bottom and top skybox planes.
	 */
	private static final float BOTTOM = -16.0F;

	/**
	 * Cosine of 22.5 degrees.
	 */
	private static final double COS_22_5 = Math.cos(Math.toRadians(22.5));

	/**
	 * Sine of 22.5 degrees.
	 */
	private static final double SIN_22_5 = Math.sin(Math.toRadians(22.5));
	private VertexBuffer buffer;
	private int currentRenderDistance;

	public HorizonRenderer() {
		currentRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();

		rebuildBuffer();
	}

	private void rebuildBuffer() {
		if (this.buffer != null) {
			this.buffer.close();
		}

        this.buffer = BufferHelper.makeStaticBuffer(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION, writer -> {
            buildHorizon(currentRenderDistance * 16, writer);
        });
	}

	private void buildQuad(VertexBufferWriter consumer, double x1, double z1, double x2, double z2) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertex = stack.nmalloc(4 * PositionVertex.STRIDE);
            PositionVertex.put(vertex + 0 * PositionVertex.STRIDE, (float)x1, BOTTOM, (float)z1);
            PositionVertex.put(vertex + 1 * PositionVertex.STRIDE, (float)x1, TOP, (float)z1);
            PositionVertex.put(vertex + 2 * PositionVertex.STRIDE, (float)x2, TOP, (float)z2);
            PositionVertex.put(vertex + 3 * PositionVertex.STRIDE, (float)x2, BOTTOM, (float)z2);
            consumer.push(stack, vertex, 4, PositionVertex.FORMAT);
        }
	}

	private void buildHalf(VertexBufferWriter consumer, double adjacent, double opposite, boolean invert) {
		if (invert) {
			adjacent = -adjacent;
			opposite = -opposite;
		}

		// NB: Make sure that these vertices are being specified in counterclockwise order!
		// Otherwise back face culling will remove your quads, and you'll be wondering why there's a hole in your horizon.
		// Don't poke holes in the horizon. Specify vertices in counterclockwise order.

		// +X,-Z face
		buildQuad(consumer, adjacent, -opposite, opposite, -adjacent);
		// +X face
		buildQuad(consumer, adjacent, opposite, adjacent, -opposite);
		// +X,+Z face
		buildQuad(consumer, opposite, adjacent, adjacent, opposite);
		// +Z face
		buildQuad(consumer, -opposite, adjacent, opposite, adjacent);
	}

	/**
	 * @param adjacent the adjacent side length of the a triangle with a hypotenuse extending from the center of the
	 *                 octagon to a given vertex on the perimeter.
	 * @param opposite the opposite side length of the a triangle with a hypotenuse extending from the center of the
	 *                 octagon to a given vertex on the perimeter.
	 */
	private void buildOctagonalPrism(VertexBufferWriter consumer, double adjacent, double opposite) {
		buildHalf(consumer, adjacent, opposite, false);
		buildHalf(consumer, adjacent, opposite, true);
	}

	private void buildRegularOctagonalPrism(VertexBufferWriter consumer, double radius) {
		buildOctagonalPrism(consumer, radius * COS_22_5, radius * SIN_22_5);
	}

	private void buildBottomPlane(VertexBufferWriter consumer, int radius) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertex = stack.nmalloc(4 * PositionVertex.STRIDE);

            for (int x = -radius; x <= radius; x += 64) {
                for (int z = -radius; z <= radius; z += 64) {
                    PositionVertex.put(vertex + 0 * PositionVertex.STRIDE, (float)(x + 64), BOTTOM, (float)z);
                    PositionVertex.put(vertex + 1 * PositionVertex.STRIDE, (float)x, BOTTOM, (float)z);
                    PositionVertex.put(vertex + 2 * PositionVertex.STRIDE, (float)x, BOTTOM, (float)(z + 64));
                    PositionVertex.put(vertex + 3 * PositionVertex.STRIDE, (float)(x + 64), BOTTOM, (float)(z + 64));
                    consumer.push(stack, vertex, 4, PositionVertex.FORMAT);
                }
            }
        }
	}

	private void buildTopPlane(VertexBufferWriter consumer, int radius) {
		// You might be tempted to try to combine this with buildBottomPlane to avoid code duplication,
		// but that won't work since the winding order has to be reversed or else one of the planes will be
		// discarded by back face culling.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertex = stack.nmalloc(4 * PositionVertex.STRIDE);

            for (int x = -radius; x <= radius; x += 64) {
                for (int z = -radius; z <= radius; z += 64) {
                    PositionVertex.put(vertex + 0 * PositionVertex.STRIDE, (float)(x + 64), TOP, (float)z);
                    PositionVertex.put(vertex + 1 * PositionVertex.STRIDE, (float)(x + 64), TOP, (float)(z + 64));
                    PositionVertex.put(vertex + 2 * PositionVertex.STRIDE, (float)x, TOP, (float)(z + 64));
                    PositionVertex.put(vertex + 3 * PositionVertex.STRIDE, (float)x, TOP, (float)z);
                    consumer.push(stack, vertex, 4, PositionVertex.FORMAT);
                }
            }
        }
	}

	private void buildHorizon(int radius, VertexBufferWriter consumer) {
		if (radius > 256) {
			// Prevent the prism from getting too large, this causes issues on some shader packs that modify the vanilla
			// sky if we don't do this.
			radius = 256;
		}

		buildRegularOctagonalPrism(consumer, radius);

		// Replicate the vanilla top plane since we can't assume that it'll be rendered.
		// TODO: Remove vanilla top plane
		buildTopPlane(consumer, 384);

		// Always make the bottom plane have a radius of 384, to match the top plane.
		buildBottomPlane(consumer, 384);
	}

	public void renderHorizon(Matrix4f modelView, Matrix4f projection, ShaderInstance shader) {
		if (currentRenderDistance != Minecraft.getInstance().options.getEffectiveRenderDistance()) {
			currentRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
			rebuildBuffer();
		}

		buffer.bind();
		buffer.drawWithShader(modelView, projection, shader);
		VertexBuffer.unbind();
	}

	public void destroy() {
		buffer.close();
	}
}
