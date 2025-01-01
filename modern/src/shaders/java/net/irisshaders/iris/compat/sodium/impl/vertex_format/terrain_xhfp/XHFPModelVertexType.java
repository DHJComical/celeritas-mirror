package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshAttribute;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisChunkMeshAttributes;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
	public static final int STRIDE = 40;
	public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, STRIDE)
		.addElement(ChunkMeshAttribute.POSITION_MATERIAL_MESH, "a_PosId", 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false, true)
		.addElement(ChunkMeshAttribute.COLOR_SHADE, "a_Color", 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
		.addElement(ChunkMeshAttribute.BLOCK_TEXTURE, "a_TexCoord", 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
		.addElement(ChunkMeshAttribute.LIGHT_TEXTURE, "a_LightCoord", 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
		.addElement(IrisChunkMeshAttributes.MID_TEX_COORD, "mc_midTexCoord",20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
		.addElement(IrisChunkMeshAttributes.TANGENT, "at_tangent", 24, GlVertexAttributeFormat.BYTE, 4, true, false)
		.addElement(IrisChunkMeshAttributes.NORMAL, "iris_Normal", 28, GlVertexAttributeFormat.BYTE, 3, true, false)
		.addElement(IrisChunkMeshAttributes.BLOCK_ID, "mc_Entity", 32, GlVertexAttributeFormat.SHORT, 2, false, false)
		.addElement(IrisChunkMeshAttributes.MID_BLOCK, "at_midBlock", 36, GlVertexAttributeFormat.BYTE, 4, false, false)
		.build();

	private static final int POSITION_MAX_VALUE = 65536;
	private static final int TEXTURE_MAX_VALUE = 32768;

	private static final float MODEL_ORIGIN = 8.0f;
	private static final float MODEL_RANGE = 32.0f;
	private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;

	private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

	private static final float TEXTURE_SCALE = (1.0f / TEXTURE_MAX_VALUE);

	public static int encodeTexture(float u, float v) {
		return ((Math.round(u * TEXTURE_MAX_VALUE) & 0xFFFF) << 0) |
			((Math.round(v * TEXTURE_MAX_VALUE) & 0xFFFF) << 16);
	}

	static float decodeBlockTexture(short raw) {
		return (raw & 0xFFFF) * TEXTURE_SCALE;
	}

	static short encodePosition(float v) {
		return (short) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
	}

	static float decodePosition(short raw) {
		return (raw & 0xFFFF) * MODEL_SCALE - MODEL_ORIGIN;
	}

	@Override
	public float getTextureScale() {
		return TEXTURE_SCALE;
	}

	@Override
	public float getPositionScale() {
		return MODEL_SCALE;
	}

	@Override
	public float getPositionOffset() {
		return -MODEL_ORIGIN;
	}

	@Override
	public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
		return VERTEX_FORMAT;
	}

	@Override
	public ChunkVertexEncoder getEncoder() {
		return new XHFPTerrainVertex();
	}
}
