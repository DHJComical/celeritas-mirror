package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
	public static final int STRIDE = 48;
	public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
		.addElement("a_PosId", 0, GlVertexAttributeFormat.FLOAT, 4, false, false)
		.addElement("a_Color", 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
		.addElement("a_TexCoord", 16, GlVertexAttributeFormat.FLOAT, 2, false, false)
		.addElement("a_LightCoord", 24, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
		.addElement("mc_midTexCoord",28, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
		.addElement("at_tangent", 32, GlVertexAttributeFormat.BYTE, 4, true, false)
		.addElement("iris_Normal", 36, GlVertexAttributeFormat.BYTE, 3, true, false)
		.addElement("mc_Entity", 40, GlVertexAttributeFormat.SHORT, 2, false, false)
		.addElement("at_midBlock", 44, GlVertexAttributeFormat.BYTE, 4, false, false)
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
	public ChunkVertexEncoder getEncoder() {
		return new XHFPTerrainVertex();
	}
}
