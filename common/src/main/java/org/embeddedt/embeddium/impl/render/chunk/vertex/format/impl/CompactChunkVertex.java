package org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import java.util.List;
import java.util.Map;

public class CompactChunkVertex implements ChunkVertexType {
    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(24)
            .addElement("a_PosId", 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false, true)
            .addElement("a_Color", 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement("a_TexCoord", 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .addElement("a_LightCoord", 16, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .addElement("a_RdhFactor", 20, GlVertexAttributeFormat.BYTE, 4, true, false)
            .build();

    public static final int STRIDE = 24;

    private static final int POSITION_BITS = 21;
    private static final int POSITION_LOW_BITS = 5;
    private static final int POSITION_MASK = (1 << POSITION_BITS) - 1;
    private static final int POSITION_LOW_MASK = (1 << POSITION_LOW_BITS) - 1;
    private static final int POSITION_MAX_VALUE = 1 << POSITION_BITS;

    private static final int TEXTURE_BITS = 18;
    private static final int TEXTURE_LOW_BITS = 2;
    private static final int TEXTURE_MASK = (1 << TEXTURE_BITS) - 1;
    private static final int TEXTURE_LOW_MASK = (1 << TEXTURE_LOW_BITS) - 1;
    private static final int TEXTURE_MAX_VALUE = 1 << (TEXTURE_BITS - 1);

    private static final int MATERIAL_BITS = 4;
    private static final int MATERIAL_MASK = (1 << MATERIAL_BITS) - 1;
    private static final int TEXTURE_LOW_U_SHIFT = MATERIAL_BITS;
    private static final int TEXTURE_LOW_V_SHIFT = MATERIAL_BITS + TEXTURE_LOW_BITS;

    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_RANGE = 32.0f;
    private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;
    private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

    private static final float TEXTURE_SCALE = (1.0f / TEXTURE_MAX_VALUE);

    @Override
    public float getTextureScale() {
        return TEXTURE_SCALE;
    }

    @Override
    public float getPositionScale() {
        return MODEL_SCALE;
    }

    @Override
    public float getPositionOffset() {
        return -MODEL_ORIGIN;
    }

    @Override
    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return (ptr, material, vertex, sectionIndex) -> {
            int x = encodePosition(vertex.x);
            int y = encodePosition(vertex.y);
            int z = encodePosition(vertex.z);

            LWJGL.memPutShort(ptr + 0, (short) (x >>> POSITION_LOW_BITS));
            LWJGL.memPutShort(ptr + 2, (short) (y >>> POSITION_LOW_BITS));
            LWJGL.memPutShort(ptr + 4, (short) (z >>> POSITION_LOW_BITS));
            LWJGL.memPutShort(ptr + 6, (short) ((x & POSITION_LOW_MASK)
                    | ((y & POSITION_LOW_MASK) << POSITION_LOW_BITS)
                    | ((z & POSITION_LOW_MASK) << (POSITION_LOW_BITS * 2))));

            LWJGL.memPutInt(ptr + 8, vertex.color);

            int u = encodeTexture(vertex.u);
            int v = encodeTexture(vertex.v);

            LWJGL.memPutShort(ptr + 12, (short) (u >>> TEXTURE_LOW_BITS));
            LWJGL.memPutShort(ptr + 14, (short) (v >>> TEXTURE_LOW_BITS));

            LWJGL.memPutInt(ptr + 16, (encodeDrawParameters(material, sectionIndex, u, v) << 0) | (encodeLight(vertex.light) << 16));
            LWJGL.memPutInt(ptr + 20, vertex.rdhFactor);

            return ptr + STRIDE;
        };
    }

    @Override
    public Map<String, String> getDefines() {
        var map = ChunkVertexType.super.getDefines();
        map.put("USE_VERTEX_COMPRESSION", "");
        return map;
    }

    private static int encodePosition(float value) {
        return (int) ((MODEL_ORIGIN + value) * MODEL_SCALE_INV) & POSITION_MASK;
    }

    public static float decodePosition(int value) {
        return (((float) (value & POSITION_MASK)) / MODEL_SCALE_INV) - MODEL_ORIGIN;
    }

    private static int encodeDrawParameters(Material material, int sectionIndex, int u, int v) {
        return (((sectionIndex & 0xFF) << 8)
                | ((v & TEXTURE_LOW_MASK) << TEXTURE_LOW_V_SHIFT)
                | ((u & TEXTURE_LOW_MASK) << TEXTURE_LOW_U_SHIFT)
                | ((material.bits() & MATERIAL_MASK) << 0));
    }

    private static int encodeLight(int light) {
        int block = light & 0xFF;
        int sky = (light >> 16) & 0xFF;
        return ((block << 0) | (sky << 8));
    }

    private static int encodeTexture(float value) {
        return Math.round(value * TEXTURE_MAX_VALUE) & TEXTURE_MASK;
    }
}
