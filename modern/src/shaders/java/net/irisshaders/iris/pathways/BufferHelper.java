package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.vertex.*;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;

import java.util.function.Consumer;

public class BufferHelper {
    public static VertexBuffer makeStaticBuffer(VertexFormat.Mode mode, VertexFormat format, Consumer<VertexBufferWriter> writerConsumer) {
        //? if <1.21 {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // Build the horizon quads into a buffer
        buffer.begin(mode, format);

        writerConsumer.accept((VertexBufferWriter)buffer);

        BufferBuilder.RenderedBuffer renderedBuffer = buffer.end();
        //?} else {
        /*var buffer = Tesselator.getInstance().begin(mode, format);

        writerConsumer.accept((VertexBufferWriter)buffer);

        var renderedBuffer = buffer.buildOrThrow();
        *///?}

        var uploaded = new VertexBuffer(VertexBuffer.Usage.STATIC);
        uploaded.bind();
        uploaded.upload(renderedBuffer);
        VertexBuffer.unbind();
        return uploaded;
    }
}
