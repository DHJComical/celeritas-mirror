package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
    public static final ChunkVertexType BASE_VERTEX_TYPE = ChunkMeshFormats.VANILLA_LIKE;

	public static final int STRIDE = 48;
	public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
		.addAllElements(BASE_VERTEX_TYPE.getVertexFormat())
		.addElement("mc_midTexCoord", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
		.addElement("at_tangent", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, true, false)
		.addElement("iris_Normal", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 3, true, false)
		.addElement("mc_Entity", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.SHORT, 2, false, false)
		.addElement("at_midBlock", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, false, false)
		.build();

    private static final int TEXTURE_MAX_VALUE = 32768;

    public static int encodeTexture(float u, float v) {
        return ((Math.round(u * TEXTURE_MAX_VALUE) & 0xFFFF) << 0) |
                ((Math.round(v * TEXTURE_MAX_VALUE) & 0xFFFF) << 16);
    }


    @Override
	public float getTextureScale() {
		return 1f;
	}

	@Override
	public float getPositionScale() {
		return 1f;
	}

	@Override
	public float getPositionOffset() {
		return 0;
	}

	@Override
	public GlVertexFormat getVertexFormat() {
		return VERTEX_FORMAT;
	}

	@Override
	public ChunkVertexEncoder createEncoder() {
		return new XHFPTerrainVertex();
	}
}
