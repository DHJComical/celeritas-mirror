package org.embeddedt.embeddium.impl.render.vertex.buffer;

import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;

import java.nio.ByteBuffer;

public interface ExtendedBufferBuilder extends VertexBufferWriter {
    ByteBuffer sodium$getBuffer();
    int sodium$getElementOffset();
    void sodium$moveToNextVertex();
    VertexFormatDescription sodium$getFormatDescription();
    boolean sodium$usingFixedColor();
    SodiumBufferBuilder sodium$getDelegate();
}
