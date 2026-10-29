package net.irisshaders.iris.vertices;

import com.mojang.blaze3d.vertex.VertexFormatElement;
//? if <26.1
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public final class ExtendedDataHelper {
	// TODO: Resolve render types for normal blocks?
	public static final short BLOCK_RENDER_TYPE = -1;
	/**
	 * All fluids have a ShadersMod render type of 1, to match behavior of Minecraft 1.7 and earlier.
	 */
	public static final short FLUID_RENDER_TYPE = 1;

	public static int packMidBlock(float x, float y, float z) {
		return ((int) (x * 64) & 0xFF) | (((int) (y * 64) & 0xFF) << 8) | (((int) (z * 64) & 0xFF) << 16);
	}

	public static int computeMidBlock(float x, float y, float z, int localPosX, int localPosY, int localPosZ) {
		return packMidBlock(
			localPosX + 0.5f - x,
			localPosY + 0.5f - y,
			localPosZ + 0.5f - z
		);
	}

	/**
	 * The element offsets are passed in rather than resolved from a {@link VertexFormatDescription}: they are
	 * fixed for the lifetime of the format, and resolving them here would cost three map lookups per primitive.
	 */
	public static void fillExtendedData(BufferBuilderPolygonView polygon, Vector3f normal, long[] vertexPointers,
			int vertexAmount, int midTextureOffset, int normalOffset, int tangentOffset, boolean replaceNormal) {
		polygon.setup(0L, vertexPointers);

		float midU = 0.0f;
		float midV = 0.0f;
		for (int i = 0; i < vertexAmount; i++) {
			midU += polygon.u(i);
			midV += polygon.v(i);
		}
		midU /= vertexAmount;
		midV /= vertexAmount;

		if (vertexAmount == 3) {
			for (int i = 0; i < vertexAmount; i++) {
				long pointer = vertexPointers[i];
				int packedNormal = MemoryUtil.memGetInt(pointer + normalOffset);
				int tangent = NormalHelper.computeTangentSmooth(
						NormI8.unpackX(packedNormal), NormI8.unpackY(packedNormal), NormI8.unpackZ(packedNormal), polygon);
				MemoryUtil.memPutFloat(pointer + midTextureOffset, midU);
				MemoryUtil.memPutFloat(pointer + midTextureOffset + 4L, midV);
				MemoryUtil.memPutInt(pointer + tangentOffset, tangent);
			}
		} else {
			NormalHelper.computeFaceNormal(normal, polygon);
			int packedNormal = replaceNormal ? NormI8.pack(normal.x, normal.y, normal.z, 0.0f) : 0;
			int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, polygon);

			for (int i = 0; i < vertexAmount; i++) {
				long pointer = vertexPointers[i];
				MemoryUtil.memPutFloat(pointer + midTextureOffset, midU);
				MemoryUtil.memPutFloat(pointer + midTextureOffset + 4L, midV);
				if (replaceNormal) {
					MemoryUtil.memPutInt(pointer + normalOffset, packedNormal);
				}
				MemoryUtil.memPutInt(pointer + tangentOffset, tangent);
			}
		}
	}
}
