package net.irisshaders.iris.vertices;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.irisshaders.iris.Iris;

public class IrisVertexFormats {
	public static final VertexFormatElement ENTITY_ELEMENT;
	public static final VertexFormatElement ENTITY_ID_ELEMENT;
	public static final VertexFormatElement MID_TEXTURE_ELEMENT;
	public static final VertexFormatElement TANGENT_ELEMENT;
	public static final VertexFormatElement MID_BLOCK_ELEMENT;
    //? if <1.21
	public static final VertexFormatElement PADDING_SHORT;

	public static final VertexFormat TERRAIN;
	public static final VertexFormat ENTITY;
	public static final VertexFormat GLYPH;
	public static final VertexFormat CLOUDS;

	static {
        //? if <1.21 {
		ENTITY_ELEMENT = new VertexFormatElement(11, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.GENERIC, 2);
		ENTITY_ID_ELEMENT = new VertexFormatElement(11, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 3);
		MID_TEXTURE_ELEMENT = new VertexFormatElement(12, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
		TANGENT_ELEMENT = new VertexFormatElement(13, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 4);
		MID_BLOCK_ELEMENT = new VertexFormatElement(14, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 3);
		PADDING_SHORT = new VertexFormatElement(1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.PADDING, 1);

		ImmutableMap.Builder<String, VertexFormatElement> terrainElements = ImmutableMap.builder();
		ImmutableMap.Builder<String, VertexFormatElement> entityElements = ImmutableMap.builder();
		ImmutableMap.Builder<String, VertexFormatElement> glyphElements = ImmutableMap.builder();
		ImmutableMap.Builder<String, VertexFormatElement> cloudsElements = ImmutableMap.builder();

		terrainElements.put("Position", DefaultVertexFormat.ELEMENT_POSITION); // 12
		terrainElements.put("Color", DefaultVertexFormat.ELEMENT_COLOR); // 16
		terrainElements.put("UV0", DefaultVertexFormat.ELEMENT_UV0); // 24
		terrainElements.put("UV2", DefaultVertexFormat.ELEMENT_UV2); // 28
		terrainElements.put("Normal", DefaultVertexFormat.ELEMENT_NORMAL); // 31
		terrainElements.put("Padding", DefaultVertexFormat.ELEMENT_PADDING); // 32
		terrainElements.put("mc_Entity", ENTITY_ELEMENT); // 36
		terrainElements.put("mc_midTexCoord", MID_TEXTURE_ELEMENT); // 44
		terrainElements.put("at_tangent", TANGENT_ELEMENT); // 48
		terrainElements.put("at_midBlock", MID_BLOCK_ELEMENT); // 51
		terrainElements.put("Padding2", DefaultVertexFormat.ELEMENT_PADDING); // 52

		entityElements.put("Position", DefaultVertexFormat.ELEMENT_POSITION); // 12
		entityElements.put("Color", DefaultVertexFormat.ELEMENT_COLOR); // 16
		entityElements.put("UV0", DefaultVertexFormat.ELEMENT_UV0); // 24
		entityElements.put("UV1", DefaultVertexFormat.ELEMENT_UV1); // 28
		entityElements.put("UV2", DefaultVertexFormat.ELEMENT_UV2); // 32
		entityElements.put("Normal", DefaultVertexFormat.ELEMENT_NORMAL); // 35
		entityElements.put("Padding", DefaultVertexFormat.ELEMENT_PADDING); // 36
		entityElements.put("iris_Entity", ENTITY_ID_ELEMENT); // 40
		entityElements.put("mc_midTexCoord", MID_TEXTURE_ELEMENT); // 48
		entityElements.put("at_tangent", TANGENT_ELEMENT); // 52
		entityElements.put("Padding2", PADDING_SHORT); // 52

		glyphElements.put("Position", DefaultVertexFormat.ELEMENT_POSITION); // 12
		glyphElements.put("Color", DefaultVertexFormat.ELEMENT_COLOR); // 16
		glyphElements.put("UV0", DefaultVertexFormat.ELEMENT_UV0); // 24
		glyphElements.put("UV2", DefaultVertexFormat.ELEMENT_UV2); // 28
		glyphElements.put("Normal", DefaultVertexFormat.ELEMENT_NORMAL); // 31
		glyphElements.put("Padding", DefaultVertexFormat.ELEMENT_PADDING); // 32
		glyphElements.put("iris_Entity", ENTITY_ID_ELEMENT); // 38
		glyphElements.put("mc_midTexCoord", MID_TEXTURE_ELEMENT); // 46
		glyphElements.put("at_tangent", TANGENT_ELEMENT); // 50
		glyphElements.put("Padding2", PADDING_SHORT); // 52

		cloudsElements.put("Position", DefaultVertexFormat.ELEMENT_POSITION); // 12
		cloudsElements.put("Color", DefaultVertexFormat.ELEMENT_COLOR); // 16
		cloudsElements.put("Normal", DefaultVertexFormat.ELEMENT_NORMAL); // 31
		cloudsElements.put("Padding", DefaultVertexFormat.ELEMENT_PADDING); // 32

		TERRAIN = new VertexFormat(terrainElements.build());
		ENTITY = new VertexFormat(entityElements.build());
		GLYPH = new VertexFormat(glyphElements.build());
		CLOUDS = new VertexFormat(cloudsElements.build());
        //?} else {
        /*int LAST_UV = 0;
        for (int i = 0; i < VertexFormatElement.MAX_COUNT; i++) {
            VertexFormatElement element = VertexFormatElement.byId(i);
            if (element != null && element.usage() == VertexFormatElement.Usage.UV) {
                LAST_UV = Math.max(LAST_UV, element.index());
            }
        }

        ENTITY_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.GENERIC, 2);
        ENTITY_ID_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), LAST_UV + 1, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 3);
        MID_TEXTURE_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2);
        TANGENT_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 4);
        MID_BLOCK_ELEMENT = VertexFormatElement.register(getNextVertexFormatElementId(), 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 3);

        TERRAIN = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("UV0", VertexFormatElement.UV0)
                .add("UV2", VertexFormatElement.UV2)
                .add("Normal", VertexFormatElement.NORMAL)
                .padding(1)
                .add("mc_Entity", ENTITY_ELEMENT)
                .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
                .add("at_tangent", TANGENT_ELEMENT)
                .add("at_midBlock", MID_BLOCK_ELEMENT)
                .padding(1)
                .build();

        ENTITY = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("UV0", VertexFormatElement.UV0)
                .add("UV1", VertexFormatElement.UV1)
                .add("UV2", VertexFormatElement.UV2)
                .add("Normal", VertexFormatElement.NORMAL)
                .padding(1)
                .add("iris_Entity", ENTITY_ID_ELEMENT)
                .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
                .add("at_tangent", TANGENT_ELEMENT)
                .build();

        GLYPH = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("UV0", VertexFormatElement.UV0)
                .add("UV2", VertexFormatElement.UV2)
                .add("Normal", VertexFormatElement.NORMAL)
                .padding(1)
                .add("iris_Entity", ENTITY_ID_ELEMENT)
                .add("mc_midTexCoord", MID_TEXTURE_ELEMENT)
                .add("at_tangent", TANGENT_ELEMENT)
                .padding(1)
                .build();

        CLOUDS = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("Normal", VertexFormatElement.NORMAL)
                .padding(1)
                .build();
        *///?}
	}

    //? if >=1.21 {
    /*private static int getNextVertexFormatElementId() {
        int id = 0;
        while (VertexFormatElement.byId(id) != null) {
            if (++id >= VertexFormatElement.MAX_COUNT) {
                throw new RuntimeException("Too many mods registering VertexFormatElements");
            }
        }
        return id;
    }
    *///?}
}
