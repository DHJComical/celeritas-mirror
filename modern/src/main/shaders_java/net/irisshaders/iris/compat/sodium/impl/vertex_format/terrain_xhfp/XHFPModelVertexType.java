package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexLayout;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

/**
 * Like HFPModelVertexType, but extended to support Iris.
 */
public class XHFPModelVertexType implements ChunkVertexType {
    public static final ChunkVertexType BASE_VERTEX_TYPE = ChunkMeshFormats.VANILLA_LIKE;

	public static final int HEADER_STRIDE = 8;
	public static final int VERTEX_STRIDE = 28;

	/**
	 * Packs the block's position within its section (4 bits per axis), whether {@code at_midBlock} has an offset
	 * (bit 12), and the block's light emission (bits 16 to 23).
	 */
	public static final String BLOCK_INFO = "celeritas_BlockInfo";

	public static final GlVertexFormat HEADER_FORMAT = GlVertexFormat.builder(HEADER_STRIDE)
		.addElement("mc_Entity", 0, GlVertexAttributeFormat.SHORT, 2, false, false)
		.addElement(BLOCK_INFO, 4, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
		.build();

	public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(VERTEX_STRIDE)
		.addElements(ChunkVertexLayout.requireAttributes(BASE_VERTEX_TYPE.getLayout()).getAttributes().stream().filter(a -> !a.getName().equals("a_RdhFactor")).toList())
		.build();

	public static final ChunkVertexLayout LAYOUT = ChunkVertexLayout.withHeader(VERTEX_FORMAT, HEADER_FORMAT, 4);

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
	public ChunkVertexLayout getLayout() {
		return LAYOUT;
	}

	@Override
	public ChunkVertexEncoder createEncoder() {
		return new XHFPTerrainVertex();
	}
}
