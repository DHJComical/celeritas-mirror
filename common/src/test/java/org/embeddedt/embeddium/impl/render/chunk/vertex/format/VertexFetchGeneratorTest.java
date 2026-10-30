package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.VertexFetchGenerator.Component;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.VertexFetchGenerator.Element;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.VertexFetchGenerator.Scope;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.CompactChunkVertex;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VertexFetchGeneratorTest {
    /**
     * A layout with a header, using every conversion the generator supports.
     */
    private static final ChunkVertexLayout HEADER_LAYOUT = ChunkVertexLayout.withHeader(
            GlVertexFormat.builder(12)
                    .addElement("a_PosId", 0, GlVertexAttributeFormat.FLOAT, 2, false, false)
                    .addElement("a_TexCoord", 8, GlVertexAttributeFormat.SHORT, 2, false, false)
                    .build(),
            GlVertexFormat.builder(12)
                    .addElement("a_Color", 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true, false)
                    .addElement("a_RdhFactor", 4, GlVertexAttributeFormat.SHORT, 2, true, false)
                    .addElement("a_LightCoord", 8, GlVertexAttributeFormat.BYTE, 4, false, true)
                    .build(),
            4);

    @Test
    void compactPlanMatchesAttributeFetch() {
        assertPlanMatchesAttributeFetch(CompactChunkVertex.LAYOUT, 4);
    }

    @Test
    void vanillaLikePlanMatchesAttributeFetch() {
        assertPlanMatchesAttributeFetch(VanillaLikeChunkVertex.LAYOUT, 4);
    }

    @Test
    void headerPlanMatchesAttributeFetch() {
        assertPlanMatchesAttributeFetch(HEADER_LAYOUT, 4);
    }

    @Test
    void rejectsComponentsStraddlingWords() {
        assertThrows(IllegalArgumentException.class, () -> ChunkVertexLayout.of(GlVertexFormat.builder(8)
                .addElement("a_PosId", 1, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
                .build()));
    }

    @Test
    void rejectsStridesThatAreNotWords() {
        assertThrows(IllegalArgumentException.class, () -> ChunkVertexLayout.of(GlVertexFormat.builder(6)
                .addElement("a_PosId", 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
                .build()));
    }

    @Test
    void headerLayoutsCannotUseAttributes() {
        assertFalse(HEADER_LAYOUT.supportsAttributeFetch());
        assertThrows(IllegalArgumentException.class,
                () -> VertexFetchGenerator.generate(HEADER_LAYOUT, 4, VertexFetchMode.ATTRIBUTES, false));
    }

    @Test
    void legacyPullingDecodesFloatsWithoutBitCasts() {
        String source = VertexFetchGenerator.generate(VanillaLikeChunkVertex.LAYOUT, 4, VertexFetchMode.PULLED, true);

        assertTrue(source.contains("texelFetchBuffer("));
        assertTrue(source.contains("_vf_bits_to_float("));
        assertFalse(source.contains("uintBitsToFloat"));
    }

    @Test
    void declaresEveryElementOfTheLayout() {
        var layout = ChunkVertexLayout.of(GlVertexFormat.builder(8)
                .addElement("mc_Entity", 0, GlVertexAttributeFormat.SHORT, 2, false, false)
                .addElement("anything_else", 4, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
                .build());

        String source = VertexFetchGenerator.generate(layout, 4, VertexFetchMode.ATTRIBUTES, false);

        assertTrue(source.contains("in vec2 mc_Entity;"));
        assertTrue(source.contains("in uint anything_else;"));
    }

    @Test
    void declaredTypesPadLikeAttributeFetch() {
        var layout = ChunkVertexLayout.of(GlVertexFormat.builder(4)
                .addElement("mc_Entity", 0, GlVertexAttributeFormat.SHORT, 2, false, false)
                .build());
        var declared = Map.of("mc_Entity", VertexFetchGenerator.GlslType.parse("vec4"));

        String attributes = VertexFetchGenerator.generate(layout, 4, VertexFetchMode.ATTRIBUTES, false, declared);
        String pulled = VertexFetchGenerator.generate(layout, 4, VertexFetchMode.PULLED, false, declared);

        assertTrue(attributes.contains("in vec4 mc_Entity;"));
        assertTrue(pulled.contains("\nvec4 mc_Entity;"));
        assertTrue(pulled.contains("v.mc_Entity = vec2(float((int((_vf_v0 << 16u)) >> 16)), float((int(_vf_v0) >> 16)));"), pulled);
        assertTrue(pulled.contains("mc_Entity = vec4(v.mc_Entity, 0.0, 1.0);"), pulled);
    }

    @Test
    void declaredTypesTruncateAndCast() {
        var layout = ChunkVertexLayout.of(GlVertexFormat.builder(4)
                .addElement("at_midBlock", 0, GlVertexAttributeFormat.BYTE, 4, false, false)
                .build());
        var declared = Map.of("at_midBlock", VertexFetchGenerator.GlslType.parse("ivec2"));

        String pulled = VertexFetchGenerator.generate(layout, 4, VertexFetchMode.PULLED, false, declared);

        assertTrue(pulled.contains("at_midBlock = ivec2(v.at_midBlock);"), pulled);
    }

    @Test
    void adaptsValuesLikeAttributeFetch() {
        var vec4 = VertexFetchGenerator.GlslType.parse("vec4");

        assertEquals("x", vec4.adapt("x", vec4));
        assertEquals("vec3(x)", VertexFetchGenerator.GlslType.parse("vec3").adapt("x", vec4));
        assertEquals("vec4(x, 0.0, 1.0)", vec4.adapt("x", VertexFetchGenerator.GlslType.parse("vec2")));
        assertEquals("ivec4(x, 1)", VertexFetchGenerator.GlslType.parse("ivec4").adapt("x", VertexFetchGenerator.GlslType.parse("vec3")));
    }

    @Test
    void parsesOnlyScalarAndVectorTypes() {
        assertEquals(new VertexFetchGenerator.GlslType(VertexFetchGenerator.ScalarType.UINT, 3), VertexFetchGenerator.GlslType.parse("uvec3"));
        assertEquals(new VertexFetchGenerator.GlslType(VertexFetchGenerator.ScalarType.INT, 1), VertexFetchGenerator.GlslType.parse("int"));
        assertThrows(IllegalArgumentException.class, () -> VertexFetchGenerator.GlslType.parse("mat4"));
        assertThrows(IllegalArgumentException.class, () -> VertexFetchGenerator.GlslType.parse("vec5"));
    }

    /**
     * Mirrors the arithmetic of the generated {@code _vf_bits_to_float}, which must be exact for normal floats.
     */
    @Test
    void legacyFloatDecodeIsExactForNormals() {
        var random = new Random(5678);

        for (int i = 0; i < 1_000_000; i++) {
            int bits = random.nextInt();
            int exponent = (bits >>> 23) & 0xFF;

            if (exponent == 0 || exponent == 0xFF) {
                continue;
            }

            float magnitude = (1.0f + (float) (bits & 0x7FFFFF) / 8388608.0f) * Math.scalb(1.0f, exponent - 127);
            float decoded = (bits >>> 31) != 0 ? -magnitude : magnitude;

            assertEquals(Float.intBitsToFloat(bits), decoded);
        }
    }

    @Test
    void compactPulledSourceIsUnchanged() throws IOException {
        assertEquals(readResource("compact_pulled.glsl"),
                VertexFetchGenerator.generate(CompactChunkVertex.LAYOUT, 4, VertexFetchMode.PULLED, false));
    }

    @Test
    void compactAttributeSourceIsUnchanged() throws IOException {
        assertEquals(readResource("compact_attributes.glsl"),
                VertexFetchGenerator.generate(CompactChunkVertex.LAYOUT, 4, VertexFetchMode.ATTRIBUTES, false));
    }

    /**
     * Fills records with random bytes, then checks that every component the plan extracts from 32-bit words equals what
     * attribute fetch reads from the attribute's own offset and type.
     */
    private static void assertPlanMatchesAttributeFetch(ChunkVertexLayout layout, int verticesPerPrimitive) {
        var header = layout.header();
        int headerStride = header != null ? header.format().getStride() : 0;
        int vertices = header != null ? header.verticesPerRecord() : verticesPerPrimitive;
        int vertexStride = layout.vertex().getStride();

        var random = new Random(1234);
        var record = ByteBuffer.allocate(headerStride + vertices * vertexStride).order(ByteOrder.LITTLE_ENDIAN);
        List<Element> plan = VertexFetchGenerator.plan(layout);

        assertFalse(plan.isEmpty());

        for (int iteration = 0; iteration < 1000; iteration++) {
            random.nextBytes(record.array());

            for (int corner = 0; corner < vertices; corner++) {
                int vertexBase = headerStride + corner * vertexStride;

                for (Element element : plan) {
                    boolean inHeader = element.components().get(0).scope() == Scope.HEADER;
                    GlVertexAttribute attribute = (inHeader ? header.format() : layout.vertex()).getAttribute(element.name());
                    int base = inHeader ? 0 : vertexBase;

                    for (int i = 0; i < element.components().size(); i++) {
                        Component component = element.components().get(i);
                        int word = record.getInt(base + component.word() * 4);

                        double expected = attributeFetch(record, base + attribute.getPointer() + i * attribute.getFormat().size(), attribute);
                        double actual = interpret(component, word);

                        assertEquals(expected, actual, "component " + i + " of " + element.name());
                    }
                }
            }
        }
    }

    private static double attributeFetch(ByteBuffer record, int offset, GlVertexAttribute attribute) {
        GlVertexAttributeFormat format = attribute.getFormat();

        if (format.equals(GlVertexAttributeFormat.FLOAT)) {
            return record.getFloat(offset);
        }

        long value;
        int bits = format.size() * 8;
        boolean signed = format.equals(GlVertexAttributeFormat.BYTE) || format.equals(GlVertexAttributeFormat.SHORT);

        if (format.equals(GlVertexAttributeFormat.UNSIGNED_BYTE)) {
            value = Byte.toUnsignedLong(record.get(offset));
        } else if (format.equals(GlVertexAttributeFormat.BYTE)) {
            value = record.get(offset);
        } else if (format.equals(GlVertexAttributeFormat.UNSIGNED_SHORT)) {
            value = Short.toUnsignedLong(record.getShort(offset));
        } else if (format.equals(GlVertexAttributeFormat.SHORT)) {
            value = record.getShort(offset);
        } else if (format.equals(GlVertexAttributeFormat.UNSIGNED_INT)) {
            value = Integer.toUnsignedLong(record.getInt(offset));
        } else {
            throw new IllegalArgumentException("Unhandled format " + format);
        }

        if (attribute.isIntType() || !attribute.isNormalized()) {
            return attribute.isIntType() ? value : (float) value;
        }

        if (signed) {
            return Math.max((float) value / (float) ((1L << (bits - 1)) - 1), -1.0f);
        } else {
            return (float) value / (float) ((1L << bits) - 1);
        }
    }

    /**
     * Evaluates a planned component the way the generated GLSL does.
     */
    private static double interpret(Component component, int word) {
        int bits = component.bits();
        long unsigned = bits == 32 ? Integer.toUnsignedLong(word) : (word >>> component.shift()) & ((1L << bits) - 1);
        long signed = bits == 32 ? word : (word << (32 - component.shift() - bits)) >> (32 - bits);

        return switch (component.conversion()) {
            case UINT -> unsigned;
            case SINT -> signed;
            case FLOAT_BITS -> Float.intBitsToFloat(word);
            case UNORM -> (float) unsigned / (float) ((1L << bits) - 1);
            case SNORM -> Math.max((float) signed / (float) ((1L << (bits - 1)) - 1), -1.0f);
            case UINT_TO_FLOAT -> (float) unsigned;
            case SINT_TO_FLOAT -> (float) signed;
        };
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = VertexFetchGeneratorTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "missing test resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
