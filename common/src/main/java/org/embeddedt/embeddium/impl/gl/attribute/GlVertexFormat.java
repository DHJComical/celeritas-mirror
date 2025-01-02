package org.embeddedt.embeddium.impl.gl.attribute;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides a generic vertex format which contains attributes. Other code can then retrieve
 * the attributes and work with encoded data in a generic manner without needing to rely on a specific format.
 */
@AllArgsConstructor
public class GlVertexFormat {
    private final Reference2ReferenceLinkedOpenHashMap<String, GlVertexAttribute> attributesKeyed;

    private final int stride;

    public static Builder builder(int stride) {
        return new Builder(stride);
    }

    /**
     * Returns the {@link GlVertexAttribute} of this vertex format bound to the type {@param name}.
     * @throws NullPointerException If the attribute does not exist in this format
     */
    public GlVertexAttribute getAttribute(String name) {
        GlVertexAttribute attr = this.attributesKeyed.get(name);

        if (attr == null) {
            throw new NullPointerException("No attribute exists for " + name);
        }

        return attr;
    }

    public Collection<GlVertexAttribute> getAttributes() {
        return Collections.unmodifiableCollection(this.attributesKeyed.values());
    }

    /**
     * @return The stride (or the size of) the vertex format in bytes
     */
    public int getStride() {
        return this.stride;
    }

    @Override
    public String toString() {
        return String.format("GlVertexFormat{attributes=%d,stride=%d}",
                this.attributesKeyed.size(), this.stride);
    }

    public static class Builder {
        private final Reference2ReferenceLinkedOpenHashMap<String, GlVertexAttribute> attributes;
        private final int stride;

        public Builder(int stride) {
            this.attributes = new Reference2ReferenceLinkedOpenHashMap<>();
            this.stride = stride;
        }

        public Builder addElement(String name, int pointer, GlVertexAttributeFormat format, int count, boolean normalized, boolean intType) {
            return this.addElement(new GlVertexAttribute(format, name, count, normalized, pointer, this.stride, intType));
        }

        /**
         * Adds an vertex attribute which will be bound to the given generic attribute type.
         *
         * @param attribute The attribute to bind
         * @throws IllegalStateException If an attribute is already bound to the generic type
         */
        private Builder addElement(GlVertexAttribute attribute) {
            if (attribute.getPointer() >= this.stride) {
                throw new IllegalArgumentException("Element starts outside vertex format");
            }

            if (attribute.getPointer() + attribute.getSize() > this.stride) {
                throw new IllegalArgumentException("Element extends outside vertex format");
            }

            if (this.attributes.put(attribute.getName(), attribute) != null) {
                throw new IllegalStateException("Generic attribute " + attribute.getName() + " already defined in vertex format");
            }

            return this;
        }

        /**
         * Creates a {@link GlVertexFormat} from the current builder.
         */
        public GlVertexFormat build() {
            int size = 0;

            for (var attribute : this.attributes.values()) {
                size = Math.max(size, attribute.getPointer() + attribute.getSize());
            }

            // The stride must be large enough to cover all attributes. This still allows for additional padding
            // to be added to the end of the vertex to accommodate alignment restrictions.
            if (this.stride < size) {
                throw new IllegalArgumentException("Stride is too small");
            }

            return new GlVertexFormat(this.attributes, this.stride);
        }
    }
}
