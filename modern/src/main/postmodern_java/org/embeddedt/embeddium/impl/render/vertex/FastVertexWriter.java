package org.embeddedt.embeddium.impl.render.vertex;

import org.joml.Matrix4fc;

/**
 * Implemented on {@link com.mojang.blaze3d.vertex.BufferBuilder} to write a whole vertex at once for vertex formats
 * vanilla only fills one element at a time. Each method returns false, writing nothing, if the builder's format
 * differs.
 */
public interface FastVertexWriter {
    /**
     * Writes a {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#PARTICLE} vertex.
     * @param color ARGB color
     * @param light packed light coordinates
     */
    boolean celeritas$putParticleVertex(float x, float y, float z, float u, float v, int color, int light);

    /**
     * Writes a {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#POSITION_COLOR_TEX_LIGHTMAP} vertex, transforming
     * its position by the given matrix.
     * @param color ARGB color
     * @param light packed light coordinates
     */
    boolean celeritas$putTextVertex(Matrix4fc pose, float x, float y, float z, int color, float u, float v, int light);
}
