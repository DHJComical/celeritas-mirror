package org.embeddedt.embeddium.impl.gl.tessellation;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;

import java.util.Objects;

public record TessellationBinding(GlBufferTarget target,
                                  GlBuffer buffer,
                                  GlVertexAttributeBinding[] attributeBindings) {
    public static TessellationBinding forVertexBuffer(GlBuffer buffer, GlVertexAttributeBinding[] attributes) {
        Objects.requireNonNull(attributes);
        return new TessellationBinding(GlBufferTarget.ARRAY_BUFFER, buffer, attributes);
    }

    public static TessellationBinding forVertexBuffer(GlBuffer buffer, GlVertexFormat format) {
        return forVertexBuffer(buffer, format, 0, 0);
    }

    public static TessellationBinding forVertexBuffer(GlBuffer buffer, GlVertexFormat format, int firstAttributeIndex, int divisor) {
        Objects.requireNonNull(format);

        var attributes = format.getAttributes();
        var bindings = new GlVertexAttributeBinding[attributes.size()];
        int index = 0;

        for (var attribute : attributes) {
            bindings[index] = new GlVertexAttributeBinding(firstAttributeIndex + index, attribute, divisor);
            index++;
        }

        return forVertexBuffer(buffer, bindings);
    }

    public static TessellationBinding forElementBuffer(GlBuffer buffer) {
        return new TessellationBinding(GlBufferTarget.ELEMENT_BUFFER, buffer, new GlVertexAttributeBinding[0]);
    }
}
