package org.embeddedt.embeddium.impl.gl.attribute;

public class GlVertexAttributeBinding extends GlVertexAttribute {
    private final int index;
    private final int divisor;

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute) {
        this(index, attribute, 0);
    }

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute, int divisor) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.getName(), attribute.isNormalized(), attribute.getPointer(), attribute.getStride(), attribute.isIntType());

        if (divisor < 0) {
            throw new IllegalArgumentException("Divisor must not be negative");
        }

        this.index = index;
        this.divisor = divisor;
    }

    public int getIndex() {
        return this.index;
    }

    public int getDivisor() {
        return this.divisor;
    }
}
