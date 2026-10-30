package org.embeddedt.embeddium.impl.render.vertex;

/**
 * Implemented on {@link com.mojang.blaze3d.vertex.BufferBuilder} to write a whole particle vertex at once.
 */
public interface ParticleVertexWriter {
    /**
     * Writes one vertex if this builder uses {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#PARTICLE}.
     * @param color ARGB color
     * @param light packed light coordinates
     * @return false if the format differs and nothing was written
     */
    boolean celeritas$putParticleVertex(float x, float y, float z, float u, float v, int color, int light);
}
