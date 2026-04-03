package org.embeddedt.embeddium.api.vertex.format.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.embeddedt.embeddium.api.vertex.attributes.common.PositionAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;

public class PositionVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance()
            .get(DefaultVertexFormat.POSITION);

    public static final int STRIDE = 12;

    private static final int OFFSET_POSITION = 0;

    public static void put(long ptr, float x, float y, float z) {
        PositionAttribute.put(ptr + OFFSET_POSITION, x, y, z);
    }
}
