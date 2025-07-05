package org.embeddedt.embeddium.impl.gl.tessellation;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;

/**
 * An enumeration over the supported OpenGL primitive types.
 */
public enum GlPrimitiveType {
    POINTS(GL11C.GL_POINTS),

    LINES(GL11C.GL_LINES),
    LINE_STRIP(GL11C.GL_LINE_STRIP),
    LINE_LOOP(GL11C.GL_LINE_LOOP),

    TRIANGLES(GL11C.GL_TRIANGLES),
    TRIANGLE_STRIP(GL11C.GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GL11C.GL_TRIANGLE_FAN),

    LINES_ADJACENCY(GL32C.GL_LINES_ADJACENCY),
    LINE_STRIP_ADJACENCY(GL32C.GL_LINE_STRIP_ADJACENCY),
    TRIANGLES_ADJACENCY(GL32C.GL_TRIANGLES_ADJACENCY),
    TRIANGLE_STRIP_ADJACENCY(GL32C.GL_TRIANGLE_STRIP_ADJACENCY),

    PATCHES(GL40C.GL_PATCHES),

    QUADS(GL30C.GL_QUADS);

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
