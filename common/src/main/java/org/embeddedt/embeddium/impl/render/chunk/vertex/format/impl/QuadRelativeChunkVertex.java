package org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexLayout;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * A pulled format that keeps the precision block models need in 88 bytes per quad.
 * <p>
 * Positions are snapped to a grid of 2<sup>-20</sup> blocks, fine enough to keep offsets like 1.00001 apart from 1.0.
 * Each quad's header holds an origin, and its vertices hold 21-bit offsets from it, which keep the full grid for quads up
 * to 1.75 blocks across. Larger quads shift their offsets onto a coarser grid. Because the grid is global, a vertex shared
 * by two quads with the same shift decodes identically in both. Positions outside [-8, 24) of the section are clamped.
 * <p>
 * Texture coordinates have 20 bits across the atlas, rounded towards the middle of the quad so its corners never move
 * outside the region of the atlas it covers. AO has a byte of its own, written as 255 because vertex colors arrive with
 * AO already applied.
 * <p>
 * Header, 2 words:
 * <ul>
 *     <li>Word 0: origin X, Y and Z in bits 0-6, 7-13 and 14-20, in 1/4 blocks from 8 blocks below the section, the
 *     position shift in bits 21-23, and the section index in bits 24-31.</li>
 *     <li>Word 1: the material in bits 0-3, and {@link #EXTENSION_BITS} bits left to formats built on this one.</li>
 * </ul>
 * Vertex, 5 words:
 * <ul>
 *     <li>Words 0-1: the high 16 bits of the X, Y and Z offsets, then the light coordinates.</li>
 *     <li>Word 2: the high 16 bits of U and V.</li>
 *     <li>Word 3: RGBA color.</li>
 *     <li>Word 4: the low 5 bits of the X, Y and Z offsets in bits 0-14, the low 4 bits of U and V in bits 15-22, and
 *     AO in bits 23-30.</li>
 * </ul>
 * {@code vertex_format/quad_relative.glsl} decodes this layout.
 */
public class QuadRelativeChunkVertex implements ChunkVertexType {
    public static final int HEADER_STRIDE = 8;
    public static final int VERTEX_STRIDE = 20;

    public static final String QUAD_DATA = "a_QuadData";

    public static final int EXTENSION_SHIFT = 4;
    public static final int EXTENSION_BITS = 32 - EXTENSION_SHIFT;

    public static final GlVertexFormat HEADER_FORMAT = GlVertexFormat.builder(HEADER_STRIDE)
            .addElement(QUAD_DATA, 0, GlVertexAttributeFormat.UNSIGNED_INT, 2, false, true)
            .build();

    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(VERTEX_STRIDE)
            .addElement("a_Position", 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 3, false, true)
            .addElement("a_LightCoord", 6, GlVertexAttributeFormat.UNSIGNED_SHORT, 1, false, true)
            .addElement("a_TexCoord", 8, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .addElement("a_Color", 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement("a_LowBits", 16, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .build();

    public static final ChunkVertexLayout LAYOUT = ChunkVertexLayout.withHeader(VERTEX_FORMAT, HEADER_FORMAT, 4);

    private static final int GRID_BITS = 20;
    private static final float GRID_SCALE = 1 << GRID_BITS;
    private static final float POSITION_MIN = -8 * GRID_SCALE;
    private static final float POSITION_MAX = 24 * GRID_SCALE;

    private static final int ORIGIN_BITS = 7;
    private static final int ORIGIN_MAX = (1 << ORIGIN_BITS) - 1;
    private static final int ORIGIN_UNIT_SHIFT = GRID_BITS - 2;
    private static final int ORIGIN_BIAS = 8 << 2;

    private static final int OFFSET_MAX = (1 << 21) - 1;
    private static final int OFFSET_LOW_BITS = 5;
    private static final int OFFSET_LOW_MASK = (1 << OFFSET_LOW_BITS) - 1;
    private static final int MAX_SHIFT = 7;

    private static final int TEXTURE_BITS = 20;
    private static final float TEXTURE_SCALE = 1 << TEXTURE_BITS;
    private static final int TEXTURE_MAX = (1 << TEXTURE_BITS) - 1;
    private static final int TEXTURE_LOW_BITS = 4;
    private static final int TEXTURE_LOW_MASK = (1 << TEXTURE_LOW_BITS) - 1;

    private static final int MATERIAL_MASK = 0xF;

    private static final int AO_UNOCCLUDED = 0xFF;

    @Override
    public ChunkVertexLayout getLayout() {
        return LAYOUT;
    }

    @Override
    public String getShaderLibrary() {
        return "sodium:include/vertex_format/quad_relative.glsl";
    }

    @Override
    public Encoder createEncoder() {
        return new Encoder();
    }

    public static final class Encoder implements ChunkVertexEncoder {
        private final int[] positions = new int[12];
        private final int[] origin = new int[3];
        private int shift;

        @Override
        public long writePrimitive(long ptr, Material material, Vertex[] vertices, int sectionIndex) {
            this.prepare(vertices);
            return this.writeQuad(ptr, material, vertices, sectionIndex, 0);
        }

        /**
         * Chooses the origin and shift of the given quad, which {@link #writeQuad} must then be called with.
         */
        public void prepare(Vertex[] vertices) {
            int[] positions = this.positions;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < 4; i++) {
                var vertex = vertices[i];
                int x = quantizePosition(vertex.x);
                int y = quantizePosition(vertex.y);
                int z = quantizePosition(vertex.z);

                positions[i * 3] = x;
                positions[i * 3 + 1] = y;
                positions[i * 3 + 2] = z;

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }

            int[] origin = this.origin;
            origin[0] = encodeOrigin(minX);
            origin[1] = encodeOrigin(minY);
            origin[2] = encodeOrigin(minZ);
            this.shift = chooseShift(maxX, maxY, maxZ, origin[0], origin[1], origin[2]);
        }

        /**
         * {@return the section-relative block containing the prepared quad's origin on the given axis}
         */
        public int originBlock(int axis) {
            return (this.origin[axis] - ORIGIN_BIAS) >> 2;
        }

        /**
         * Writes the quad last given to {@link #prepare}.
         *
         * @param extension the value of the header's {@link #EXTENSION_BITS} extension bits
         * @return the pointer just past the vertices
         */
        public long writeQuad(long ptr, Material material, Vertex[] vertices, int sectionIndex, int extension) {
            int[] positions = this.positions;
            int originX = this.origin[0], originY = this.origin[1], originZ = this.origin[2];
            int shift = this.shift;

            LWJGL.memPutInt(ptr, originX | (originY << ORIGIN_BITS) | (originZ << (ORIGIN_BITS * 2))
                    | (shift << 21) | ((sectionIndex & 0xFF) << 24));
            LWJGL.memPutInt(ptr + 4, (material.bits() & MATERIAL_MASK) | (extension << EXTENSION_SHIFT));

            float uSum = 0, vSum = 0;

            for (var vertex : vertices) {
                uSum += vertex.u;
                vSum += vertex.v;
            }

            float uCenter = uSum * 0.25f * TEXTURE_SCALE;
            float vCenter = vSum * 0.25f * TEXTURE_SCALE;
            long vertexPtr = ptr + HEADER_STRIDE;

            for (int i = 0; i < 4; i++) {
                var vertex = vertices[i];
                int x = encodeOffset(positions[i * 3], originX, shift);
                int y = encodeOffset(positions[i * 3 + 1], originY, shift);
                int z = encodeOffset(positions[i * 3 + 2], originZ, shift);
                int u = quantizeTexture(vertex.u, uCenter);
                int v = quantizeTexture(vertex.v, vCenter);

                LWJGL.memPutShort(vertexPtr, (short) (x >>> OFFSET_LOW_BITS));
                LWJGL.memPutShort(vertexPtr + 2, (short) (y >>> OFFSET_LOW_BITS));
                LWJGL.memPutShort(vertexPtr + 4, (short) (z >>> OFFSET_LOW_BITS));
                LWJGL.memPutShort(vertexPtr + 6, (short) encodeLight(vertex.light));
                LWJGL.memPutShort(vertexPtr + 8, (short) (u >>> TEXTURE_LOW_BITS));
                LWJGL.memPutShort(vertexPtr + 10, (short) (v >>> TEXTURE_LOW_BITS));
                LWJGL.memPutInt(vertexPtr + 12, vertex.color);
                LWJGL.memPutInt(vertexPtr + 16, (x & OFFSET_LOW_MASK)
                        | ((y & OFFSET_LOW_MASK) << OFFSET_LOW_BITS)
                        | ((z & OFFSET_LOW_MASK) << (OFFSET_LOW_BITS * 2))
                        | ((u & TEXTURE_LOW_MASK) << 15)
                        | ((v & TEXTURE_LOW_MASK) << (15 + TEXTURE_LOW_BITS))
                        | (AO_UNOCCLUDED << 23));

                vertexPtr += VERTEX_STRIDE;
            }

            return vertexPtr;
        }
    }

    /**
     * Scaling by a power of two is exact, so coordinates already on the grid are kept exactly.
     */
    static int quantizePosition(float value) {
        return Math.round(Math.max(POSITION_MIN, Math.min(POSITION_MAX, value * GRID_SCALE)));
    }

    static int encodeOrigin(int min) {
        return Math.max(0, Math.min(ORIGIN_MAX, (min >> ORIGIN_UNIT_SHIFT) + ORIGIN_BIAS));
    }

    private static int originUnits(int origin) {
        return (origin - ORIGIN_BIAS) << ORIGIN_UNIT_SHIFT;
    }

    /**
     * Rounds to the nearest multiple of 2<sup>shift</sup>, the grid shared by every quad with this shift.
     */
    private static int roundToShift(int value, int shift) {
        return shift == 0 ? value : (value + (1 << (shift - 1))) >> shift;
    }

    static int chooseShift(int maxX, int maxY, int maxZ, int originX, int originY, int originZ) {
        for (int shift = 0; shift < MAX_SHIFT; shift++) {
            if (roundToShift(maxX, shift) - (originUnits(originX) >> shift) <= OFFSET_MAX
                    && roundToShift(maxY, shift) - (originUnits(originY) >> shift) <= OFFSET_MAX
                    && roundToShift(maxZ, shift) - (originUnits(originZ) >> shift) <= OFFSET_MAX) {
                return shift;
            }
        }

        return MAX_SHIFT;
    }

    static int encodeOffset(int position, int origin, int shift) {
        return Math.max(0, Math.min(OFFSET_MAX, roundToShift(position, shift) - (originUnits(origin) >> shift)));
    }

    static int quantizeTexture(float value, float center) {
        float scaled = value * TEXTURE_SCALE;
        float rounded;

        if (scaled < center) {
            rounded = (float) Math.ceil(scaled);
        } else if (scaled > center) {
            rounded = (float) Math.floor(scaled);
        } else {
            rounded = Math.round(scaled);
        }

        return (int) Math.max(0, Math.min(TEXTURE_MAX, rounded));
    }

    private static int encodeLight(int light) {
        int block = light & 0xFF;
        int sky = (light >> 16) & 0xFF;
        return block | (sky << 8);
    }
}
