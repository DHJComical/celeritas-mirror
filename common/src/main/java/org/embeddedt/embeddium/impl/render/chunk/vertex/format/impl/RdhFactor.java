package org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

/**
 * The per-quad color correction that formats storing {@code a_RdhFactor} precompute, which makes the colors of a quad
 * split along its 0-2 diagonal vary close to bilinearly across it.
 */
final class RdhFactor {
    private RdhFactor() {}

    /**
     * {@return the factor of the given primitive as four signed bytes in ABGR order, or 0 if it is not a quad}
     */
    static int encode(ChunkVertexEncoder.Vertex[] vertices) {
        if (vertices.length != 4) {
            return 0;
        }

        int color0 = vertices[0].color;
        int color1 = vertices[1].color;
        int color2 = vertices[2].color;
        int color3 = vertices[3].color;
        // TODO can likely do SWAR tricks if performance is an issue
        return encodeChannel(color3, color1, color0, color2, 0)
                | encodeChannel(color3, color1, color0, color2, 8) << 8
                | encodeChannel(color3, color1, color0, color2, 16) << 16
                | encodeChannel(color3, color1, color0, color2, 24) << 24;
    }

    private static int encodeChannel(int positive0, int positive1, int negative0, int negative1, int shift) {
        int factor = ((positive0 >> shift) & 0xFF) + ((positive1 >> shift) & 0xFF)
                - ((negative0 >> shift) & 0xFF) - ((negative1 >> shift) & 0xFF);
        return Math.round(factor * (127.0F / 510.0F)) & 0xFF;
    }
}
