package org.embeddedt.embeddium.impl.render.chunk.shader;

public enum ChunkShaderTextureSlot {
    BLOCK,
    LIGHT,
    ATLAS_EDGE_MASK,
    VERTEX_DATA;

    public static final ChunkShaderTextureSlot[] VALUES = ChunkShaderTextureSlot.values();
}
