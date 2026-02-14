package org.embeddedt.embeddium.impl.gl.buffer;

import org.taumc.celeritas.lwjgl.GL20;
import org.taumc.celeritas.lwjgl.GL21;
import org.taumc.celeritas.lwjgl.GL31;
import org.taumc.celeritas.lwjgl.GL43;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;


public enum GlBufferTarget {
    ARRAY_BUFFER(GL20.GL_ARRAY_BUFFER, GL20.GL_ARRAY_BUFFER_BINDING),
    ELEMENT_BUFFER(GL20.GL_ELEMENT_ARRAY_BUFFER, GL20.GL_ELEMENT_ARRAY_BUFFER_BINDING),
    COPY_READ_BUFFER(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_READ_BUFFER),
    COPY_WRITE_BUFFER(GL31.GL_COPY_WRITE_BUFFER, GL31.GL_COPY_WRITE_BUFFER),
    DRAW_INDIRECT_BUFFER(GL43.GL_DRAW_INDIRECT_BUFFER, GL43.GL_DRAW_INDIRECT_BUFFER_BINDING),
    PIXEL_PACK_BUFFER(GL21.GL_PIXEL_PACK_BUFFER, GL21.GL_PIXEL_PACK_BUFFER_BINDING),
    PIXEL_UNPACK_BUFFER(GL21.GL_PIXEL_UNPACK_BUFFER, GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

    public static final GlBufferTarget[] VALUES = GlBufferTarget.values();
    public static final int COUNT = VALUES.length;

    private final int target;
    private final int binding;

    GlBufferTarget(int target, int binding) {
        this.target = target;
        this.binding = binding;
    }

    public int getTargetParameter() {
        return this.target;
    }

    public int getBindingParameter() {
        return this.binding;
    }
}
