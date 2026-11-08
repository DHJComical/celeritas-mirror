package org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class QuadRelativeChunkVertexTest {
    private static final float GRID_STEP = 0x1p-20f;

    /**
     * The result of encoding one axis of a quad.
     */
    private record EncodedAxis(int shift, float[] decoded) {}

    /**
     * Encodes the given coordinates of a quad's four vertices along one axis, then decodes them as
     * {@code vertex_format/quad_relative.glsl} does.
     */
    private static EncodedAxis roundTrip(float... values) {
        int[] quantized = new int[values.length];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < values.length; i++) {
            quantized[i] = QuadRelativeChunkVertex.quantizePosition(values[i]);
            min = Math.min(min, quantized[i]);
            max = Math.max(max, quantized[i]);
        }

        int origin = QuadRelativeChunkVertex.encodeOrigin(min);
        int shift = QuadRelativeChunkVertex.chooseShift(max, max, max, origin, origin, origin);
        float[] decoded = new float[values.length];

        for (int i = 0; i < values.length; i++) {
            int offset = QuadRelativeChunkVertex.encodeOffset(quantized[i], origin, shift);
            decoded[i] = (float) (((origin - 32) << 18) + (offset << shift)) * GRID_STEP;
        }

        return new EncodedAxis(shift, decoded);
    }

    private static float decodeTexture(int value) {
        return value * GRID_STEP;
    }

    @Test
    void gridPositionsRoundTripExactly() {
        float[][] quads = {
                { 0.0f, 1.0f, 1.0f, 0.0f },
                { 0.0625f, 0.9375f, 0.5f, 0.0625f },
                { 15.0f, 16.0f, 16.0f, 15.0f },
                { -0.5f, 0.5f, 0.5f, -0.5f },
                { 1.0f / 1024, 3.0f / 1024, 2.0f / 1024, 1.0f / 1024 }
        };

        for (float[] quad : quads) {
            assertArrayEquals(quad, roundTrip(quad).decoded());
        }
    }

    @Test
    void floatsAboveEightBlocksRoundTripExactly() {
        var random = new Random(1234);

        for (int i = 0; i < 10000; i++) {
            float base = 8.0f + random.nextFloat() * 14.0f;
            float[] quad = { base, base + random.nextFloat(), base + random.nextFloat(), base };

            assertArrayEquals(quad, roundTrip(quad).decoded());
        }
    }

    @Test
    void smallOffsetsStayApartAndOrdered() {
        float[] quad = { 1.0f, 1.0001f, 1.00001f, 1.000001f };
        float[] decoded = roundTrip(quad).decoded();

        assertEquals(1.0f, decoded[0]);
        assertTrue(decoded[3] > decoded[0], "1.000001 collapsed onto 1.0");
        assertTrue(decoded[2] > decoded[3]);
        assertTrue(decoded[1] > decoded[2]);

        for (int i = 0; i < quad.length; i++) {
            assertEquals(quad[i], decoded[i], GRID_STEP / 2);
        }
    }

    @Test
    void offGridPositionsRoundToTheNearestGridPoint() {
        var random = new Random(5678);

        for (int i = 0; i < 10000; i++) {
            float base = -8.0f + random.nextFloat() * 30.0f;
            float[] quad = new float[4];

            for (int v = 0; v < 4; v++) {
                quad[v] = base + random.nextFloat() * 1.74f;
            }

            var encoded = roundTrip(quad);
            assertEquals(0, encoded.shift());

            for (int v = 0; v < 4; v++) {
                assertEquals(quad[v], encoded.decoded()[v], GRID_STEP / 2 + Math.ulp(quad[v]));
            }
        }
    }

    @Test
    void sharedVerticesDecodeIdenticallyAcrossQuads() {
        var random = new Random(91011);

        for (int i = 0; i < 10000; i++) {
            float shared = random.nextFloat() * 16.0f;
            float[] first = { shared, shared - random.nextFloat(), shared, shared - random.nextFloat() };
            float[] second = { shared, shared + random.nextFloat(), shared, shared + random.nextFloat() };

            assertEquals(roundTrip(first).decoded()[0], roundTrip(second).decoded()[0]);
        }
    }

    @Test
    void largeQuadsUseACoarserGrid() {
        float[] quad = { -7.0f, 13.0f, 13.0f, 2.123456f };
        var encoded = roundTrip(quad);

        assertTrue(encoded.shift() > 0);

        for (int v = 0; v < 4; v++) {
            // Rounded once onto the fine grid, then again onto the coarse one
            assertEquals(quad[v], encoded.decoded()[v], (GRID_STEP * (1 << encoded.shift()) + GRID_STEP) / 2);
        }
    }

    @Test
    void positionsOutsideTheRangeAreClampedNotWrapped() {
        float[] decoded = roundTrip(-100.0f, 0.0f, 100.0f, 0.0f).decoded();

        assertEquals(-8.0f, decoded[0]);
        assertEquals(0.0f, decoded[1]);
        assertEquals(24.0f, decoded[2], 1.0e-4f);
    }

    @Test
    void originBlockIsTheBlockContainingTheQuadMinimum() {
        var encoder = new QuadRelativeChunkVertex.Encoder();
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        float[][] positions = { { 3.0f, 6.0f, 9.0f }, { 4.0f, 6.0f, 9.0f }, { 4.0f, 6.0f, 10.0f }, { 3.0f, 6.0f, 10.0f } };

        for (int i = 0; i < 4; i++) {
            quad[i].x = positions[i][0];
            quad[i].y = positions[i][1];
            quad[i].z = positions[i][2];
        }

        encoder.prepare(quad);
        assertEquals(3, encoder.originBlock(0));
        assertEquals(6, encoder.originBlock(1));
        assertEquals(9, encoder.originBlock(2));

        quad[0].y = -0.001f;
        encoder.prepare(quad);
        assertEquals(-1, encoder.originBlock(1));
    }

    @Test
    void textureCoordinatesRoundTowardsTheQuadCenter() {
        var random = new Random(121314);

        for (int i = 0; i < 10000; i++) {
            float u0 = random.nextFloat() * 0.5f;
            float u1 = u0 + 0.001f + random.nextFloat() * 0.4f;
            float center = (u0 + u1) * 0.5f * (1 << 20);

            float decoded0 = decodeTexture(QuadRelativeChunkVertex.quantizeTexture(u0, center));
            float decoded1 = decodeTexture(QuadRelativeChunkVertex.quantizeTexture(u1, center));

            assertTrue(decoded0 >= u0 && decoded0 - u0 < GRID_STEP, "u0 moved outwards: " + u0 + " -> " + decoded0);
            assertTrue(decoded1 <= u1 && u1 - decoded1 < GRID_STEP, "u1 moved outwards: " + u1 + " -> " + decoded1);
        }
    }

    @Test
    void gridTextureCoordinatesRoundTripExactly() {
        float[] values = { 0.0f, 0.5f, 0.75f, 1.0f / 1024, 0.999f };

        for (float value : values) {
            float onGrid = Math.round(value * (1 << 20)) * GRID_STEP;
            assertEquals(onGrid, decodeTexture(QuadRelativeChunkVertex.quantizeTexture(onGrid, 0.5f * (1 << 20))));
        }
    }
}
