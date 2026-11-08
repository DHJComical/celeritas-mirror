package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.HashMap;
import java.util.Map;

public interface ChunkVertexType {
    ChunkVertexLayout getLayout();

    /**
     * {@return the shader include that decodes this type's vertex inputs} {@code chunk_vertex.glsl} imports it as
     * {@link VertexFetchGenerator#FORMAT_IMPORT_NAME}. See {@code include/vertex_format/api.txt} for what it must define.
     */
    String getShaderLibrary();

    /**
     * {@return a newly constructed instance of a vertex encoder for the given vertex type}
     */
    ChunkVertexEncoder createEncoder();

    @MustBeInvokedByOverriders
    default Map<String, String> getDefines() {
        return new HashMap<>();
    }
}
