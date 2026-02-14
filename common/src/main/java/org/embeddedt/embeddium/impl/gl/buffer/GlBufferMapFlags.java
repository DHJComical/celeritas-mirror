package org.embeddedt.embeddium.impl.gl.buffer;

import org.taumc.celeritas.lwjgl.GL30;
import org.taumc.celeritas.lwjgl.GL33;
import org.taumc.celeritas.lwjgl.GL44;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.util.EnumBit;

public enum GlBufferMapFlags implements EnumBit {
    READ(GL30.GL_MAP_READ_BIT),
    WRITE(GL30.GL_MAP_WRITE_BIT),
    PERSISTENT(GL44.GL_MAP_PERSISTENT_BIT),
    INVALIDATE_BUFFER(GL30.GL_MAP_INVALIDATE_BUFFER_BIT),
    INVALIDATE_RANGE(GL30.GL_MAP_INVALIDATE_RANGE_BIT),
    EXPLICIT_FLUSH(GL30.GL_MAP_FLUSH_EXPLICIT_BIT),
    COHERENT(GL44.GL_MAP_COHERENT_BIT),
    UNSYNCHRONIZED(GL33.GL_MAP_UNSYNCHRONIZED_BIT);

    private final int bit;

    GlBufferMapFlags(int bit) {
        this.bit = bit;
    }

    @Override
    public int getBits() {
        return this.bit;
    }
}
