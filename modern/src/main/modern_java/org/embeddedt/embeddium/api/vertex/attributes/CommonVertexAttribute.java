package org.embeddedt.embeddium.api.vertex.attributes;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CommonVertexAttribute {
    //? if <1.21 {
    public static final VertexFormatElement POSITION = DefaultVertexFormat.ELEMENT_POSITION;
    public static final VertexFormatElement COLOR = DefaultVertexFormat.ELEMENT_COLOR;
    public static final VertexFormatElement TEXTURE = DefaultVertexFormat.ELEMENT_UV0;
    public static final VertexFormatElement OVERLAY = DefaultVertexFormat.ELEMENT_UV1;
    public static final VertexFormatElement LIGHT = DefaultVertexFormat.ELEMENT_UV2;
    public static final VertexFormatElement NORMAL = DefaultVertexFormat.ELEMENT_NORMAL;
    //?} else {
    /*public static final VertexFormatElement POSITION = VertexFormatElement.POSITION;
    public static final VertexFormatElement COLOR = VertexFormatElement.COLOR;
    public static final VertexFormatElement TEXTURE = VertexFormatElement.UV0;
    public static final VertexFormatElement OVERLAY = VertexFormatElement.UV1;
    public static final VertexFormatElement LIGHT = VertexFormatElement.UV2;
    public static final VertexFormatElement NORMAL = VertexFormatElement.NORMAL;
    *///?}
}
