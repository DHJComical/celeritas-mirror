package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;
import java.util.OptionalInt;

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
	 * Vanilla's sky pipeline, drawing quads, so that shader packs draw the horizon with their sky program.
	 */
	public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withLocation(Identifier.fromNamespaceAndPath("iris", "pipeline/horizon"))
		.withVertexShader("core/sky")
		.withFragmentShader("core/sky")
		.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
		.build();

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

	@Nullable
	private GpuBuffer buffer;
	private int vertexCount;
	private int currentRenderDistance = -1;

	private void rebuildBuffer() {
		if (this.buffer != null) {
			this.buffer.close();
		}

		try (ByteBufferBuilder bytes = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			buildHorizon(currentRenderDistance * 16, builder);

			try (MeshData mesh = builder.buildOrThrow()) {
				this.vertexCount = mesh.drawState().vertexCount();
				this.buffer = RenderSystem.getDevice().createBuffer(() -> "Iris horizon vertex buffer", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
			}
		}
	}

	private void buildQuad(BufferBuilder consumer, double x1, double z1, double x2, double z2) {
		consumer.addVertex((float) x1, BOTTOM, (float) z1);
		consumer.addVertex((float) x1, TOP, (float) z1);
		consumer.addVertex((float) x2, TOP, (float) z2);
		consumer.addVertex((float) x2, BOTTOM, (float) z2);
	}

	private void buildHalf(BufferBuilder consumer, double adjacent, double opposite, boolean invert) {
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
	private void buildOctagonalPrism(BufferBuilder consumer, double adjacent, double opposite) {
		buildHalf(consumer, adjacent, opposite, false);
		buildHalf(consumer, adjacent, opposite, true);
	}

	private void buildRegularOctagonalPrism(BufferBuilder consumer, double radius) {
		buildOctagonalPrism(consumer, radius * COS_22_5, radius * SIN_22_5);
	}

	private void buildBottomPlane(BufferBuilder consumer, int radius) {
		for (int x = -radius; x <= radius; x += 64) {
			for (int z = -radius; z <= radius; z += 64) {
				consumer.addVertex(x + 64, BOTTOM, z);
				consumer.addVertex(x, BOTTOM, z);
				consumer.addVertex(x, BOTTOM, z + 64);
				consumer.addVertex(x + 64, BOTTOM, z + 64);
			}
		}
	}

	private void buildTopPlane(BufferBuilder consumer, int radius) {
		// You might be tempted to try to combine this with buildBottomPlane to avoid code duplication,
		// but that won't work since the winding order has to be reversed or else one of the planes will be
		// discarded by back face culling.
		for (int x = -radius; x <= radius; x += 64) {
			for (int z = -radius; z <= radius; z += 64) {
				consumer.addVertex(x + 64, TOP, z);
				consumer.addVertex(x + 64, TOP, z + 64);
				consumer.addVertex(x, TOP, z + 64);
				consumer.addVertex(x, TOP, z);
			}
		}
	}

	private void buildHorizon(int radius, BufferBuilder consumer) {
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

	public void renderHorizon() {
		int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();

		if (buffer == null || currentRenderDistance != renderDistance) {
			currentRenderDistance = renderDistance;
			rebuildBuffer();
		}

		Vector3d fogColor = CapturedRenderingState.INSTANCE.getFogColor();
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrix(),
			new Vector4f((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0F), new Vector3f(), new Matrix4f());
		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		int indexCount = vertexCount / 4 * 6;
		var target = Minecraft.getInstance().getMainRenderTarget();

		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
			.createRenderPass(() -> "Iris horizon", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.setVertexBuffer(0, buffer);
			pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
			pass.drawIndexed(0, 0, indexCount, 1);
		}
	}

	public void destroy() {
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
	}
}
