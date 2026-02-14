package org.embeddedt.embeddium.impl.gl.tessellation;

import org.taumc.celeritas.lwjgl.GL32;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;


public enum GlIndexType {
    UNSIGNED_BYTE(GL32.GL_UNSIGNED_BYTE, 1),
    UNSIGNED_SHORT(GL32.GL_UNSIGNED_SHORT, 2),
    UNSIGNED_INT(GL32.GL_UNSIGNED_INT, 4);

    private final int id;
    private final int stride;

    GlIndexType(int id, int stride) {
        this.id = id;
        this.stride = stride;
    }

    public int getFormatId() {
        return this.id;
    }

    public int getStride() {
        return this.stride;
    }
}
