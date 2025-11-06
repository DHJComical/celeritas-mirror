package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferMapFlags;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.util.EnumBitField;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;

public enum BufferMapRangeFunctions {
    CORE {
        @Override
        public ByteBuffer mapBufferRange(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags) {
            return GL30C.glMapBufferRange(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, length, flags.getBitField());
        }
    },
    MAP_FULL_AND_SLICE {
        @Override
        public ByteBuffer mapBufferRange(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags) {
            if (flags.contains(GlBufferMapFlags.EXPLICIT_FLUSH)) {
                throw new UnsupportedOperationException("Explicit flush not supported for MAP_FULL_AND_SLICE strategy");
            }
            int access;
            if (flags.contains(GlBufferMapFlags.WRITE) && flags.contains(GlBufferMapFlags.READ)) {
                access = GL15C.GL_READ_WRITE;
            } else if (flags.contains(GlBufferMapFlags.WRITE)) {
                access = GL15C.GL_WRITE_ONLY;
            } else {
                access = GL15C.GL_READ_ONLY;
            }
            ByteBuffer buf = GL15C.glMapBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), access, null);
            // Avoid slicing the buffer if we can prove that the full buffer is being mapped
            if (buffer instanceof GlMutableBuffer mutBuffer && mutBuffer.getSize() == length && offset == 0) {
                return buf;
            }
            return buf.position(Math.toIntExact(offset))
                    .limit(Math.toIntExact(offset) + Math.toIntExact(length))
                    .slice();
        }
    };

    public abstract ByteBuffer mapBufferRange(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags);

    public static BufferMapRangeFunctions pickBest(RenderDevice device) {
        var caps = device.getCapabilities();
        if (caps.OpenGL30 || caps.GL_ARB_map_buffer_range) {
            return CORE;
        } else {
            return MAP_FULL_AND_SLICE;
        }
    }
}
