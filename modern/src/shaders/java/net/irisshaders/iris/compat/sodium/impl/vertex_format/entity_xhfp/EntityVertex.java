package net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormalHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class EntityVertex {
	public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance().get(IrisVertexFormats.ENTITY);
	public static final int STRIDE = IrisVertexFormats.ENTITY.getVertexSize();

	private static final int OFFSET_POSITION = 0;
	private static final int OFFSET_COLOR = 12;
	private static final int OFFSET_TEXTURE = 16;
	private static final int OFFSET_MID_TEXTURE = 42;
	private static final int OFFSET_OVERLAY = 24;
	private static final int OFFSET_LIGHT = 28;
	private static final int OFFSET_NORMAL = 32;
	private static final int OFFSET_TANGENT = 50;

	private static final Vector3f lastNormal = new Vector3f();
	private static final QuadViewEntity.QuadViewEntityUnsafe quadView = new QuadViewEntity.QuadViewEntityUnsafe();

	public static void write(long ptr,
							 float x, float y, float z, int color, float u, float v, float midU, float midV, int light, int overlay, int normal, int tangent) {
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION, x);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

		MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE, u);
		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 4, v);

		MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);

		MemoryUtil.memPutInt(ptr + OFFSET_OVERLAY, overlay);

		MemoryUtil.memPutInt(ptr + OFFSET_NORMAL, normal);
		MemoryUtil.memPutInt(ptr + OFFSET_TANGENT, tangent);

		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE, midU);
		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE + 4, midV);

		MemoryUtil.memPutShort(ptr + 36, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 38, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutShort(ptr + 40, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

	}

	public static void write2(long ptr,
							  float x, float y, float z, int color, float u, float v, float midU, float midV, int light, int overlay, int normal) {
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION, x);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

		MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE, u);
		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 4, v);

		MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);

		MemoryUtil.memPutInt(ptr + OFFSET_OVERLAY, overlay);

		MemoryUtil.memPutInt(ptr + OFFSET_NORMAL, normal);

		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE, midU);
		MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE + 4, midV);

		MemoryUtil.memPutShort(ptr + 36, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 38, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutShort(ptr + 40, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

	}

	// TODO: Publicize this method in Embeddium so it doesn't need to be copied
	private static int multARGBInts(int colorA, int colorB) {
		// Most common case: Either quad coloring or tint-based coloring, but not both
		if (colorA == -1) {
			return colorB;
		} else if (colorB == -1) {
			return colorA;
		}
		// General case (rare): Both colorings, actually perform the multiplication
		int a = (int)((ColorARGB.unpackAlpha(colorA)/255.0f) * (ColorARGB.unpackAlpha(colorB)/255.0f) * 255.0f);
		int b = (int)((ColorARGB.unpackBlue(colorA)/255.0f) * (ColorARGB.unpackBlue(colorB)/255.0f) * 255.0f);
		int g = (int)((ColorARGB.unpackGreen(colorA)/255.0f) * (ColorARGB.unpackGreen(colorB)/255.0f) * 255.0f);
		int r = (int)((ColorARGB.unpackRed(colorA)/255.0f) * (ColorARGB.unpackRed(colorB)/255.0f) * 255.0f);
		return ColorARGB.pack(r, g, b, a);
	}

	public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int light, int overlay, int color) {
		Matrix3f matNormal = matrices.normal();
		Matrix4f matPosition = matrices.pose();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			long buffer = stack.nmalloc(4 * STRIDE);
			long ptr = buffer;

			float nxt = 0, nyt = 0, nzt = 0;

			float midU = ((quad.getTexU(0) + quad.getTexU(1) + quad.getTexU(2) + quad.getTexU(3)) * 0.25f);
			float midV = ((quad.getTexV(0) + quad.getTexV(1) + quad.getTexV(2) + quad.getTexV(3)) * 0.25f);

			for (int i = 0; i < 4; i++) {
				// The position vector
				float x = quad.getX(i);
				float y = quad.getY(i);
				float z = quad.getZ(i);

				// The transformed position vector
				float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
				float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
				float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

				// The packed normal vector
				var n = quad.getForgeNormal(i);

				float nx, ny, nz;

				if((n & 0xFFFFFF) == 0) { // TODO review
					var n0 = quad.getLightFace().step();

					nx = n0.x;
					ny = n0.y;
					nz = n0.z;
				} else {
					// The normal vector
					nx = NormI8.unpackX(n);
					ny = NormI8.unpackY(n);
					nz = NormI8.unpackZ(n);
				}

				// The transformed normal vector
				nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
				nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
				nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);

				// The packed transformed normal vector
				var nt = NormI8.pack(nxt, nyt, nzt);

				write2(ptr, xt, yt, zt, multARGBInts(quad.getColor(i), color), quad.getTexU(i), quad.getTexV(i), midU, midV, ModelQuadUtil.mergeBakedLight(quad.getLight(i), quad.getVanillaLightEmission(), light), overlay, nt);
				ptr += STRIDE;
			}

			endQuad(ptr - STRIDE, nxt, nyt, nzt);

			writer.push(stack, buffer, 4, FORMAT);
		}
	}

	public static void endQuad(long ptr, float normalX, float normalY, float normalZ) {
		quadView.setup(ptr, STRIDE);

		int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, quadView);

		for (long vertex = 0; vertex < 4; vertex++) {
			MemoryUtil.memPutInt(ptr + 50 - STRIDE * vertex, tangent);
		}
	}
}
