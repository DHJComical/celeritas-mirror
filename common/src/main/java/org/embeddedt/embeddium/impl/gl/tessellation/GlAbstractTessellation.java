package org.embeddedt.embeddium.impl.gl.tessellation;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.lwjgl.opengl.EXTGPUShader4;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public abstract class GlAbstractTessellation implements GlTessellation {
    private static final boolean coreIPointerSupported, extIPointerSupported;

    static {
        var caps = GL.getCapabilities();
        coreIPointerSupported = caps.OpenGL30;
        extIPointerSupported = caps.GL_EXT_gpu_shader4;
    }

    protected final TessellationBinding[] bindings;

    protected GlAbstractTessellation(TessellationBinding[] bindings) {
        this.bindings = bindings;
    }

    private static void glVertexAttribIPointer(int index, int size, int type, int stride, long ptr) {
        if (coreIPointerSupported) {
            GL30C.glVertexAttribIPointer(index, size, type, stride, ptr);
        } else if (extIPointerSupported) {
            EXTGPUShader4.glVertexAttribIPointerEXT(index, size, type, stride, ptr);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected void bindAttributes(CommandList commandList) {
        for (TessellationBinding binding : this.bindings) {
            commandList.bindBuffer(binding.target(), binding.buffer());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                if (attrib.isIntType()) {
                    glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(),
                            attrib.getStride(), attrib.getPointer());
                } else {
                    GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(), attrib.isNormalized(),
                            attrib.getStride(), attrib.getPointer());
                }
                GL20C.glEnableVertexAttribArray(attrib.getIndex());
            }
        }
    }
}
