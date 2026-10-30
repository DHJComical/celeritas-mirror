package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Generates the GLSL that declares every element of a {@link ChunkVertexLayout} as a vertex shader input.
 * <p>
 * With attribute fetch, the elements are declared as {@code in} variables. When pulled, they are declared as globals of the
 * same names and types, and {@code _vert_fetch()} fills them from the {@link #VERTEX_DATA_SAMPLER} texture buffer,
 * converting each component the way attribute fetch would for the same {@link GlVertexAttribute}.
 * <p>
 * Callers may declare an element with a different type than its own, as a shader may declare an attribute. Missing
 * components are taken from {@code (0, 0, 0, 1)}, extra ones are dropped, and components are cast if the scalar types
 * differ.
 */
public final class VertexFetchGenerator {
    public static final String IMPORT_NAME = "sodium:generated/vertex_fetch.glsl";

    /**
     * The {@code usamplerBuffer} that pulled vertex data is read from.
     */
    public static final String VERTEX_DATA_SAMPLER = "u_VertexData";

    public enum Scope {
        HEADER,
        VERTEX
    }

    public enum Conversion {
        UINT,
        SINT,
        FLOAT_BITS,
        UNORM,
        SNORM,
        UINT_TO_FLOAT,
        SINT_TO_FLOAT
    }

    /**
     * One component of an element, held in bits {@code [shift, shift + bits)} of a 32-bit word. The word index is
     * relative to the start of the header or vertex the component belongs to.
     */
    public record Component(Scope scope, int word, int shift, int bits, Conversion conversion) {}

    public record Element(String name, GlslType type, List<Component> components) {}

    public enum ScalarType {
        FLOAT("float", "", "0.0", "1.0"),
        INT("int", "i", "0", "1"),
        UINT("uint", "u", "0u", "1u");

        private final String name;
        private final String vectorPrefix;
        private final String zero;
        private final String one;

        ScalarType(String name, String vectorPrefix, String zero, String one) {
            this.name = name;
            this.vectorPrefix = vectorPrefix;
            this.zero = zero;
            this.one = one;
        }
    }

    /**
     * A scalar or vector type, the only kinds a vertex input can have here.
     */
    public record GlslType(ScalarType scalar, int count) {
        public GlslType {
            if (count < 1 || count > 4) {
                throw new IllegalArgumentException("Vector types have 1 to 4 components, not " + count);
            }
        }

        public static GlslType parse(String name) {
            for (ScalarType scalar : ScalarType.values()) {
                if (name.equals(scalar.name)) {
                    return new GlslType(scalar, 1);
                }

                String prefix = scalar.vectorPrefix + "vec";

                if (name.length() == prefix.length() + 1 && name.startsWith(prefix)) {
                    int count = name.charAt(prefix.length()) - '0';

                    if (count >= 2 && count <= 4) {
                        return new GlslType(scalar, count);
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported vertex input type " + name);
        }

        /**
         * {@return GLSL converting a value of type {@code from} to this type, the way attribute fetch would}
         */
        public String adapt(String expression, GlslType from) {
            if (this.equals(from)) {
                return expression;
            }

            var out = new StringBuilder(this.toString()).append('(').append(expression);

            for (int i = from.count; i < this.count; i++) {
                out.append(", ").append(i == 3 ? this.scalar.one : this.scalar.zero);
            }

            return out.append(')').toString();
        }

        @Override
        public String toString() {
            return this.count == 1 ? this.scalar.name : this.scalar.vectorPrefix + "vec" + this.count;
        }
    }

    private VertexFetchGenerator() {}

    public static Map<String, String> sources(ChunkVertexLayout layout, int verticesPerPrimitive, VertexFetchMode mode, boolean legacyGlsl) {
        return sources(layout, verticesPerPrimitive, mode, legacyGlsl, Map.of());
    }

    /**
     * @param declaredTypes GLSL types to declare elements with instead of their own, by element name
     */
    public static Map<String, String> sources(ChunkVertexLayout layout, int verticesPerPrimitive, VertexFetchMode mode, boolean legacyGlsl,
                                              Map<String, GlslType> declaredTypes) {
        return Map.of(IMPORT_NAME, generate(layout, verticesPerPrimitive, mode, legacyGlsl, declaredTypes));
    }

    public static List<Element> plan(ChunkVertexLayout layout) {
        var elements = new ArrayList<Element>();

        if (layout.header() != null) {
            addElements(elements, layout.header().format(), Scope.HEADER);
        }

        addElements(elements, layout.vertex(), Scope.VERTEX);

        return elements;
    }

    private static void addElements(List<Element> elements, GlVertexFormat format, Scope scope) {
        for (GlVertexAttribute attribute : format.getAttributes()) {
            Conversion conversion = conversionOf(attribute);
            int componentSize = attribute.getFormat().size();
            var components = new ArrayList<Component>(attribute.getCount());

            for (int i = 0; i < attribute.getCount(); i++) {
                int offset = attribute.getPointer() + (i * componentSize);
                components.add(new Component(scope, offset / 4, (offset % 4) * 8, componentSize * 8, conversion));
            }

            elements.add(new Element(attribute.getName(), new GlslType(scalarType(conversion), attribute.getCount()), components));
        }
    }

    private static Conversion conversionOf(GlVertexAttribute attribute) {
        GlVertexAttributeFormat format = attribute.getFormat();

        if (format.equals(GlVertexAttributeFormat.FLOAT)) {
            if (attribute.isIntType()) {
                throw new IllegalArgumentException("Float attribute " + attribute.getName() + " cannot be an integer type");
            }

            return Conversion.FLOAT_BITS;
        }

        boolean signed = format.equals(GlVertexAttributeFormat.BYTE) || format.equals(GlVertexAttributeFormat.SHORT);

        if (attribute.isIntType()) {
            return signed ? Conversion.SINT : Conversion.UINT;
        } else if (attribute.isNormalized()) {
            return signed ? Conversion.SNORM : Conversion.UNORM;
        } else {
            return signed ? Conversion.SINT_TO_FLOAT : Conversion.UINT_TO_FLOAT;
        }
    }

    private static ScalarType scalarType(Conversion conversion) {
        return switch (conversion) {
            case UINT -> ScalarType.UINT;
            case SINT -> ScalarType.INT;
            default -> ScalarType.FLOAT;
        };
    }

    public static String generate(ChunkVertexLayout layout, int verticesPerPrimitive, VertexFetchMode mode, boolean legacyGlsl) {
        return generate(layout, verticesPerPrimitive, mode, legacyGlsl, Map.of());
    }

    public static String generate(ChunkVertexLayout layout, int verticesPerPrimitive, VertexFetchMode mode, boolean legacyGlsl,
                                  Map<String, GlslType> declaredTypes) {
        if (mode == VertexFetchMode.ATTRIBUTES && !layout.supportsAttributeFetch()) {
            throw new IllegalArgumentException("Layout cannot be fetched through vertex attributes: " + layout);
        }

        int verticesPerRecord = layout.header() != null ? layout.header().verticesPerRecord() : verticesPerPrimitive;
        List<Element> elements = plan(layout);
        var out = new StringBuilder();

        for (Element element : elements) {
            out.append("#define VERT_HAS_").append(element.name()).append('\n');
        }

        out.append('\n');

        String storage = mode == VertexFetchMode.ATTRIBUTES ? "in " : "";

        for (Element element : elements) {
            out.append(storage).append(declaredType(element, declaredTypes)).append(' ').append(element.name()).append(";\n");
        }

        out.append("\nint _vert_corner;\n\n");

        if (mode == VertexFetchMode.ATTRIBUTES) {
            out.append("void _vert_fetch() {\n");
            out.append("    _vert_corner = gl_VertexID % ").append(verticesPerRecord).append(";\n");
            out.append("}\n");

            return out.toString();
        }

        boolean needsFloatHelper = legacyGlsl && elements.stream()
                .flatMap(e -> e.components().stream())
                .anyMatch(c -> c.conversion() == Conversion.FLOAT_BITS);

        out.append("#define VERT_CAN_FETCH_CORNERS\n\n");
        out.append("uniform usamplerBuffer ").append(VERTEX_DATA_SAMPLER).append(";\n\n");

        if (needsFloatHelper) {
            appendFloatHelper(out);
        }

        String fetch = legacyGlsl ? "texelFetchBuffer" : "texelFetch";
        int vertexWords = layout.vertex().getStride() / 4;
        int headerWords = layout.header() != null ? layout.header().format().getStride() / 4 : 0;
        int recordWords = headerWords + (verticesPerRecord * vertexWords);

        List<Element> headerElements = elements.stream().filter(e -> e.components().get(0).scope() == Scope.HEADER).toList();
        List<Element> vertexElements = elements.stream().filter(e -> e.components().get(0).scope() == Scope.VERTEX).toList();

        // Holds one corner's per-vertex elements as their own types, so other corners of the primitive can be read too
        out.append("struct _VertInputs {\n");

        for (Element element : vertexElements) {
            out.append("    ").append(element.type()).append(' ').append(element.name()).append(";\n");
        }

        out.append("};\n\n");

        out.append("_VertInputs _vert_fetch_corner(int corner) {\n");
        out.append("    int _vf_vertex = ((gl_VertexID / ").append(verticesPerRecord).append(") * ").append(recordWords).append(") + ");

        if (headerWords != 0) {
            out.append(headerWords).append(" + ");
        }

        out.append("(corner * ").append(vertexWords).append(");\n");
        appendWordFetches(out, vertexElements, "_vf_vertex", fetch);
        out.append("    _VertInputs v;\n");

        for (Element element : vertexElements) {
            out.append("    v.").append(element.name()).append(" = ").append(naturalExpression(element, legacyGlsl)).append(";\n");
        }

        out.append("    return v;\n");
        out.append("}\n\n");

        out.append("void _vert_fetch() {\n");
        out.append("    _vert_corner = gl_VertexID % ").append(verticesPerRecord).append(";\n");

        if (!headerElements.isEmpty()) {
            out.append("    int _vf_record = (gl_VertexID / ").append(verticesPerRecord).append(") * ").append(recordWords).append(";\n");
            appendWordFetches(out, headerElements, "_vf_record", fetch);

            for (Element element : headerElements) {
                out.append("    ").append(element.name()).append(" = ")
                        .append(declaredType(element, declaredTypes).adapt(naturalExpression(element, legacyGlsl), element.type())).append(";\n");
            }
        }

        out.append("    _VertInputs v = _vert_fetch_corner(_vert_corner);\n");

        for (Element element : vertexElements) {
            out.append("    ").append(element.name()).append(" = ")
                    .append(declaredType(element, declaredTypes).adapt("v." + element.name(), element.type())).append(";\n");
        }

        out.append("}\n");

        return out.toString();
    }

    private static void appendWordFetches(StringBuilder out, List<Element> elements, String base, String fetch) {
        var words = new TreeSet<Integer>();

        for (Element element : elements) {
            for (Component component : element.components()) {
                words.add(component.word());
            }
        }

        for (int word : words) {
            Scope scope = elements.get(0).components().get(0).scope();
            out.append("    uint ").append(wordName(scope, word)).append(" = ").append(fetch).append('(').append(VERTEX_DATA_SAMPLER)
                    .append(", ").append(base).append(" + ").append(word).append(").r;\n");
        }
    }

    private static String naturalExpression(Element element, boolean legacyGlsl) {
        var values = element.components().stream().map(component -> componentExpression(component, legacyGlsl)).toList();

        return values.size() == 1 ? values.get(0) : element.type() + "(" + String.join(", ", values) + ")";
    }

    private static GlslType declaredType(Element element, Map<String, GlslType> declaredTypes) {
        return declaredTypes.getOrDefault(element.name(), element.type());
    }

    /**
     * Decodes IEEE 754 single-precision bits using only integer and float arithmetic, for GLSL without
     * {@code uintBitsToFloat}. Exact for normal numbers; denormals become zero, and infinities and NaNs are not handled.
     */
    private static void appendFloatHelper(StringBuilder out) {
        out.append("float _vf_bits_to_float(uint b) {\n");
        out.append("    uint e = (b >> 23u) & 0xFFu;\n");
        out.append("    if (e == 0u) return 0.0;\n");
        out.append("    float f = (1.0 + float(b & 0x7FFFFFu) / 8388608.0) * exp2(float(int(e) - 127));\n");
        out.append("    return (b >> 31u) != 0u ? -f : f;\n");
        out.append("}\n\n");
    }

    private static String wordName(Scope scope, int word) {
        return (scope == Scope.HEADER ? "_vf_h" : "_vf_v") + word;
    }

    private static String componentExpression(Component component, boolean legacyGlsl) {
        String word = wordName(component.scope(), component.word());
        int bits = component.bits();

        String unsigned;
        if (bits == 32) {
            unsigned = word;
        } else if (component.shift() == 0) {
            unsigned = "(" + word + " & " + mask(bits) + ")";
        } else {
            unsigned = "((" + word + " >> " + component.shift() + "u) & " + mask(bits) + ")";
        }

        return switch (component.conversion()) {
            case UINT -> unsigned;
            case SINT -> signedExpression(component, word);
            case FLOAT_BITS -> (legacyGlsl ? "_vf_bits_to_float(" : "uintBitsToFloat(") + word + ")";
            case UNORM -> "(float(" + unsigned + ") / " + ((1L << bits) - 1) + ".0)";
            case SNORM -> "max(float(" + signedExpression(component, word) + ") / " + ((1L << (bits - 1)) - 1) + ".0, -1.0)";
            case UINT_TO_FLOAT -> "float(" + unsigned + ")";
            case SINT_TO_FLOAT -> "float(" + signedExpression(component, word) + ")";
        };
    }

    /**
     * Moves the component to the top of the word, then sign-extends it with an arithmetic right shift.
     */
    private static String signedExpression(Component component, String word) {
        int bits = component.bits();

        if (bits == 32) {
            return "int(" + word + ")";
        }

        int leftShift = 32 - component.shift() - bits;
        String raised = leftShift == 0 ? word : "(" + word + " << " + leftShift + "u)";

        return "(int(" + raised + ") >> " + (32 - bits) + ")";
    }

    private static String mask(int bits) {
        return "0x" + Long.toHexString((1L << bits) - 1).toUpperCase() + "u";
    }
}
