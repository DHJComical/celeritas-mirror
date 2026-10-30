package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Describes how a vertex type's data is laid out in the geometry buffer. Each primitive is stored as one record: an
 * optional header holding per-primitive elements, followed by its vertices.
 * <p>
 * Only layouts without a header can be fetched through vertex attributes. Any layout can be pulled by the vertex
 * shader, see {@link VertexFetchGenerator}.
 */
public record ChunkVertexLayout(GlVertexFormat vertex, @Nullable Header header) {
    /**
     * @param verticesPerRecord the number of vertices that follow each header
     */
    public record Header(GlVertexFormat format, int verticesPerRecord) {
        public Header {
            Objects.requireNonNull(format);

            if (verticesPerRecord <= 0) {
                throw new IllegalArgumentException("Records must contain at least one vertex");
            }
        }
    }

    public ChunkVertexLayout {
        Objects.requireNonNull(vertex);
        validateWordAligned(vertex);

        if (header != null) {
            validateWordAligned(header.format());
        }
    }

    public static ChunkVertexLayout of(GlVertexFormat vertex) {
        return new ChunkVertexLayout(vertex, null);
    }

    public static ChunkVertexLayout withHeader(GlVertexFormat vertex, GlVertexFormat header, int verticesPerRecord) {
        return new ChunkVertexLayout(vertex, new Header(header, verticesPerRecord));
    }

    /**
     * Pulled records are fetched as 32-bit words, so no component may straddle a word boundary.
     */
    private static void validateWordAligned(GlVertexFormat format) {
        if (format.getStride() % 4 != 0) {
            throw new IllegalArgumentException("Stride of " + format + " is not a multiple of 4 bytes");
        }

        for (GlVertexAttribute attribute : format.getAttributes()) {
            int componentSize = attribute.getFormat().size();

            if (attribute.getPointer() % componentSize != 0) {
                throw new IllegalArgumentException("Attribute " + attribute.getName() + " at offset " + attribute.getPointer()
                        + " is not aligned to its " + componentSize + "-byte components");
            }
        }
    }

    /**
     * {@return the number of bytes one primitive of the given type occupies in the geometry buffer}
     */
    public int bytesPerPrimitive(ChunkPrimitiveType primitiveType) {
        int vertices = primitiveType.getVerticesPerPrimitive();

        if (this.header == null) {
            return this.vertex.getStride() * vertices;
        }

        if (vertices != this.header.verticesPerRecord()) {
            throw new IllegalArgumentException("Layout holds " + this.header.verticesPerRecord() + " vertices per record, but "
                    + primitiveType + " has " + vertices);
        }

        return this.header.format().getStride() + this.vertex.getStride() * vertices;
    }

    public boolean supportsAttributeFetch() {
        return this.header == null;
    }

    /**
     * {@return the attribute format of the given layout}
     * @throws UnsupportedOperationException if the layout cannot be fetched through vertex attributes
     */
    public static GlVertexFormat requireAttributes(ChunkVertexLayout layout) {
        if (layout.supportsAttributeFetch()) {
            return layout.vertex();
        }

        throw new UnsupportedOperationException("Layout must be pulled by the vertex shader: " + layout);
    }
}
