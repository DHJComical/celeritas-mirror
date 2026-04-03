package org.embeddedt.embeddium.impl.render.vertex;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
//? if <1.21
import org.embeddedt.embeddium.impl.mixin.core.render.VertexFormatAccessor;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

public class VertexFormatDescriptionImpl implements VertexFormatDescription {
    // legacy use only
    @Deprecated
    private final VertexFormat format;

    private final int id;
    private final int stride;

    private final Reference2IntMap<VertexFormatElement> offsets;

    private final boolean isSimple;

    public VertexFormatDescriptionImpl(VertexFormat format, int id) {
        this.format = format;
        this.id = id;
        this.stride = format.getVertexSize();

        this.offsets = getOffsets(format);
        this.isSimple = checkSimple(format);
    }

    private static boolean isImportantElement(VertexFormatElement element) {
        //? if <1.21 {
        return element.getUsage() != VertexFormatElement.Usage.PADDING;
        //?} else
        /*return true;*/
    }

    private static boolean checkSimple(VertexFormat format) {
        ReferenceOpenHashSet<VertexFormatElement> attributes = new ReferenceOpenHashSet<>(format.getElements().size());
        var elementList = format.getElements();

        for (int elementIndex = 0; elementIndex < elementList.size(); elementIndex++) {
            var element = elementList.get(elementIndex);
            if (isImportantElement(element) && !attributes.add(element)) {
                return false;
            }
        }

        return true;
    }

    public static Reference2IntMap<VertexFormatElement> getOffsets(VertexFormat format) {
        Reference2IntOpenHashMap<VertexFormatElement> commonElementOffsets = new Reference2IntOpenHashMap<>(format.getElements().size());

        commonElementOffsets.defaultReturnValue(-1);

        var elementList = format.getElements();
        //? if <1.21
        var elementOffsets = ((VertexFormatAccessor) format).getOffsets();

        for (int elementIndex = 0; elementIndex < elementList.size(); elementIndex++) {
            var element = elementList.get(elementIndex);

            if (!isImportantElement(element)) {
                continue;
            }

            int offset;

            //? if <1.21
            offset = elementOffsets.getInt(elementIndex);
            //? if >=1.21
            /*offset = format.getOffset(element);*/

            commonElementOffsets.put(element, offset);
        }

        return commonElementOffsets;
    }

    @Override
    public boolean containsElement(VertexFormatElement element) {
        return this.offsets.containsKey(element);
    }

    @Override
    public int getElementOffset(VertexFormatElement element) {
        int offset = this.offsets.getInt(element);

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }

    @Override
    public Collection<VertexFormatElement> getElements() {
        return this.offsets.keySet();
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public int stride() {
        return this.stride;
    }

    @Deprecated
    public VertexFormat format() {
        return this.format;
    }

    @Override
    public boolean isSimpleFormat() {
        return this.isSimple;
    }
}
