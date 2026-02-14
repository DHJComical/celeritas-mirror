package org.embeddedt.embeddium.impl.gl.buffer;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import java.nio.ByteBuffer;

public class GlBufferMapping {
    private final GlBuffer buffer;
    private final ByteBuffer map;

    protected boolean disposed;

    public GlBufferMapping(GlBuffer buffer, ByteBuffer map) {
        this.buffer = buffer;
        this.map = map;
    }

    public void write(ByteBuffer data, int writeOffset) {
        LWJGL.memCopy(LWJGL.memAddress(data), LWJGL.memAddress(this.map, writeOffset), data.remaining());
    }

    public GlBuffer getBufferObject() {
        return this.buffer;
    }

    public void dispose() {
        this.disposed = true;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public ByteBuffer getMemoryBuffer() {
        return this.map;
    }
}
