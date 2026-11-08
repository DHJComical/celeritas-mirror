package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexLayout;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.QuadRelativeChunkVertex;

/**
 * Like HFPModelVertexType, but extended to support Iris.
 * <p>
 * The layout is {@link QuadRelativeChunkVertex}'s, with these fields in its extension bits:
 * <ul>
 *     <li>Bits 0-5: the block's position relative to the block containing the quad's origin, 2 bits per axis. The
 *     value is the origin's block minus the block's own, plus 1.</li>
 *     <li>Bit 6: whether {@code at_midBlock} has an offset.</li>
 *     <li>Bits 7-10: the block's light emission.</li>
 *     <li>Bit 11: the render type of {@code mc_Entity}.</li>
 *     <li>Bits 12-27: the block ID of {@code mc_Entity}, signed.</li>
 * </ul>
 */
public class XHFPModelVertexType implements ChunkVertexType {
	public static final QuadRelativeChunkVertex BASE_VERTEX_TYPE = ChunkMeshFormats.QUAD_RELATIVE;

	/**
	 * GLSL reading the extension bits, shifted down to bit 0.
	 */
	public static final String EXTENSION = "(" + QuadRelativeChunkVertex.QUAD_DATA + ".y >> " + QuadRelativeChunkVertex.EXTENSION_SHIFT + "u)";

	@Override
	public ChunkVertexLayout getLayout() {
		return BASE_VERTEX_TYPE.getLayout();
	}

	@Override
	public String getShaderLibrary() {
		return BASE_VERTEX_TYPE.getShaderLibrary();
	}

	@Override
	public ChunkVertexEncoder createEncoder() {
		return new XHFPTerrainVertex();
	}
}
