package org.embeddedt.embeddium.impl.gl.functions;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.buffer.GlBufferStorageFlags;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.util.EnumBitField;
import org.taumc.celeritas.lwjgl.GLExtension;

public enum BufferStorageFunctions {
    NONE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            throw new UnsupportedOperationException();
        }
    },
    CORE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            LWJGL.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }
    },
    ARB {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            LWJGL.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }
    };

    public static BufferStorageFunctions pickBest(RenderDevice device) {
        if (LWJGL.isOpenGLVersionSupported(4, 4)) {
            return CORE;
        } else if (LWJGL.isExtensionSupported(GLExtension.ARB_buffer_storage)) {
            return ARB;
        } else {
            return NONE;
        }
    }


    public abstract void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags);
}
