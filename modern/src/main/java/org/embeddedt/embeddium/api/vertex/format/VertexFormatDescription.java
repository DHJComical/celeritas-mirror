package org.embeddedt.embeddium.api.vertex.format;

import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.util.Collection;
import java.util.NoSuchElementException;

public interface VertexFormatDescription {
    /**
     * @param element The type of the element to query
     * @return True if the vertex format contains the generic element, otherwise false
     */
    boolean containsElement(VertexFormatElement element);

    /**
     * @param element The type of the element to query
     * @return The offset (in bytes) at which the generic element begins within the vertex format
     * @throws NoSuchElementException If the vertex format does not contain the generic element
     */
    int getElementOffset(VertexFormatElement element);

    /**
     * Returns a collection of elements in the vertex format.
     */
    Collection<VertexFormatElement> getElements();

    /**
     * Returns the unique identifier for this vertex format.
     */
    int id();

    /**
     * Returns the number of bytes between consecutive vertices in a buffer. Each vertex in a buffer is expected to
     * start at the byte offset (index * stride).
     */
    int stride();

    /**
     * Returns whether or not the format is "simple" (has no duplicate elements).
     */
    boolean isSimpleFormat();
}
