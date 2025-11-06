package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.system.MemoryUtil;

public enum BufferCopyFunctions {
    CORE {
        @Override
        public void copyBufferSubData(CommandList commandList, GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes) {
            commandList.bindBuffer(GlBufferTarget.COPY_READ_BUFFER, src);
            commandList.bindBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst);
            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, bytes);
        }
    },
    PIXEL_PACK {
        @Override
        public void copyBufferSubData(CommandList commandList, GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes) {
            if (src.getActiveMapping() != null || dst.getActiveMapping() != null) {
                throw new IllegalStateException("Cannot use PIXEL_PACK copy strategy on mapped buffers");
            }
            commandList.bindBuffer(GlBufferTarget.PIXEL_PACK_BUFFER, src);
            commandList.bindBuffer(GlBufferTarget.PIXEL_UNPACK_BUFFER, dst);
            long srcBufPtr = GL15.nglMapBuffer(GlBufferTarget.PIXEL_PACK_BUFFER.getTargetParameter(), GL15.GL_READ_ONLY);
            if (srcBufPtr == 0L) {
                throw new IllegalStateException("Source buffer could not be mapped");
            }
            long dstBufPtr = GL15.nglMapBuffer(GlBufferTarget.PIXEL_UNPACK_BUFFER.getTargetParameter(), GL15.GL_WRITE_ONLY);
            if (dstBufPtr == 0L) {
                throw new IllegalStateException("Destination buffer could not be mapped");
            }
            MemoryUtil.memCopy(srcBufPtr + readOffset, dstBufPtr + writeOffset, bytes);
            GL15.glUnmapBuffer(GlBufferTarget.PIXEL_PACK_BUFFER.getTargetParameter());
            GL15.glUnmapBuffer(GlBufferTarget.PIXEL_UNPACK_BUFFER.getTargetParameter());
            commandList.bindBuffer(GlBufferTarget.PIXEL_PACK_BUFFER, null);
            commandList.bindBuffer(GlBufferTarget.PIXEL_UNPACK_BUFFER, null);
        }
    };

    public abstract void copyBufferSubData(CommandList commandList, GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes);

    public static BufferCopyFunctions pickBest(RenderDevice device) {
        var caps = device.getCapabilities();
        if (caps.OpenGL31 || caps.GL_ARB_copy_buffer) {
            return CORE;
        } else {
            return PIXEL_PACK;
        }
    }
}
