package org.embeddedt.embeddium.impl.gl.functions;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

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
                int count = MemoryUtil.memGetInt(pCount + off);
                if (count > 0) {
                    ARBDrawElementsBaseVertex.nglDrawElementsBaseVertex(mode, count, type,
                            MemoryUtil.memGetAddress(pIndices + ((long)i * Pointer.POINTER_SIZE)),
                            MemoryUtil.memGetInt(pBaseVertex + off));
                }
            }
        }
    },
    CORE {
        @Override
        public void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex) {
            GL32C.nglMultiDrawElementsBaseVertex(mode, pCount, type, pIndices, size, pBaseVertex);
        }
    };

    public static MultidrawFunctions pickBest(RenderDevice device) {
        GLCapabilities capabilities = device.getCapabilities();

        if (capabilities.OpenGL32) {
            return CORE;
        } else if (capabilities.GL_ARB_draw_elements_base_vertex) {
            return FALLBACK;
        } else {
            return NONE;
        }
    }

    public abstract void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int size, long pBaseVertex);
}
