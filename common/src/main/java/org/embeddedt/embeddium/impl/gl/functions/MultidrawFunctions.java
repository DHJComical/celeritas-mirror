package org.embeddedt.embeddium.impl.gl.functions;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.taumc.celeritas.lwjgl.GLExtension;
import org.taumc.celeritas.lwjgl.LWJGLServiceProvider;

public enum MultidrawFunctions {
    NONE {
        @Override
        public void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex) {
            throw new UnsupportedOperationException("Platform does not support DrawElementsBaseVertex");
        }
    },
    FALLBACK {
        @Override
        public void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex) {
            for (int i = 0; i < size; i++) {
                long off = i * 4L;
                int count = LWJGL.memGetInt(pCount + off);
                if (count > 0) {
                    LWJGL.glDrawElementsBaseVertex(mode, count, type,
                            LWJGL.memGetAddress(pIndices + ((long)i * LWJGLServiceProvider.POINTER_SIZE)),
                            LWJGL.memGetInt(pBaseVertex + off));
                }
            }
        }
    },
    CORE {
        @Override
        public void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex) {
            LWJGL.glMultiDrawElementsBaseVertex(mode, pCount, type, pIndices, size, pBaseVertex);
        }
    };

    public static MultidrawFunctions pickBest(RenderDevice device) {
        if (LWJGL.isOpenGLVersionSupported(3, 2)) {
            return CORE;
        } else if (LWJGL.isExtensionSupported(GLExtension.ARB_draw_elements_base_vertex)) {
            return FALLBACK;
        } else {
            return NONE;
        }
    }

    public abstract void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex);
}
