package org.embeddedt.embeddium.impl.gl.tessellation;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.device.CommandList;

public abstract class GlAbstractTessellation implements GlTessellation {
    protected final TessellationBinding[] bindings;

    protected GlAbstractTessellation(TessellationBinding[] bindings) {
        this.bindings = bindings;
    }

    private static void glVertexAttribIPointer(int index, int size, int type, int stride, long ptr) {
        LWJGL.glVertexAttribIPointer(index, size, type, stride, ptr);
    }

    protected void bindAttributes(CommandList commandList) {
        for (TessellationBinding binding : this.bindings) {
            commandList.bindBuffer(binding.target(), binding.buffer());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                if (attrib.isIntType()) {
                    glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(),
                            attrib.getStride(), attrib.getPointer());
                } else {
                    LWJGL.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(), attrib.isNormalized(),
                            attrib.getStride(), attrib.getPointer());
                }
                LWJGL.glEnableVertexAttribArray(attrib.getIndex());
            }
        }
    }
}
