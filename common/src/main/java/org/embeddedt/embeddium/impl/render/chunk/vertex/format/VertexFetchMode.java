package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;

/**
 * How the vertex shader obtains vertex data.
 */
public enum VertexFetchMode {
    /**
     * Vertex data is bound as vertex attributes.
     */
    ATTRIBUTES,
    /**
     * The vertex shader reads vertex data from a texture buffer over the region's geometry buffer.
     */
    PULLED;

    private static final boolean PREFER_PULLING = Boolean.getBoolean("celeritas.preferVertexPulling");

    public static VertexFetchMode select(ChunkVertexLayout layout) {
        if (!layout.supportsAttributeFetch()) {
            if (!isPullingSupported()) {
                throw new UnsupportedOperationException("Layout must be pulled by the vertex shader, which this device does not support: " + layout);
            }

            return PULLED;
        }

        return PREFER_PULLING && isPullingSupported() ? PULLED : ATTRIBUTES;
    }

    private static boolean isPullingSupported() {
        return RenderDevice.INSTANCE.getDeviceFunctions().textureBufferFunctions().isSupported();
    }
}
