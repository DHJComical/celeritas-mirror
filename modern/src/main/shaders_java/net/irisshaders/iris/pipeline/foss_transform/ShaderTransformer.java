package net.irisshaders.iris.pipeline.foss_transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.pipeline.ShaderLoadTimings;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import org.apache.commons.lang3.mutable.MutableObject;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.ShaderPrinter;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.CommonToken;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.tree.ParseTree;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderTransformer {

    private static final int CACHE_SIZE = 256;
    private static final Object2ObjectLinkedOpenHashMap<TransformKey, Map<ShaderType, String>> shaderTransformationCache = new Object2ObjectLinkedOpenHashMap<>();
    public static final boolean useCache = true;

    public record TransformKey<P extends Parameters>(Patch patchType, Map<ShaderType, String> inputs, P params) {}

    public static <P extends Parameters> Map<ShaderType, String> transform(String name, Map<ShaderType, String> sources, P parameters) {
        if (sources.isEmpty()) {
            return null;
        } else {
            Map<ShaderType, String> result;

            var patchType = parameters.patch;

            var key = new TransformKey(patchType, Map.copyOf(sources), parameters);

            synchronized (shaderTransformationCache) {
                result = shaderTransformationCache.getAndMoveToFirst(key);
            }

            if(result == null || !useCache) {
                long transformStart = ShaderLoadTimings.now();
                result = ShaderTransformationDiskCache.transformIfAbsent(key, () -> transformInternal(name, sources, patchType, parameters));
                ShaderLoadTimings.record(ShaderLoadTimings.Stage.TRANSFORM, transformStart);
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                synchronized (shaderTransformationCache) {
                    if(shaderTransformationCache.size() >= CACHE_SIZE) {
                        shaderTransformationCache.removeLast();
                    }
                    shaderTransformationCache.putAndMoveToFirst(key, result);
                }
            }

            return result;
        }
    }

    public static <P extends Parameters> Map<ShaderType, String> transformCompute(String name, String compute, P parameters) {
        if (compute == null) {
            return null;
        } else {
            Map<ShaderType, String> result;

            var patchType = parameters.patch;

            EnumMap<ShaderType, String> inputs = new EnumMap<>(ShaderType.class);
            inputs.put(ShaderType.COMPUTE, compute);

            var key = new TransformKey(patchType, inputs, parameters);

            synchronized (shaderTransformationCache) {
                result = shaderTransformationCache.getAndMoveToFirst(key);
            }
            if(result == null || !useCache) {
                long transformStart = ShaderLoadTimings.now();
                result = transformInternal(name, inputs, patchType, parameters);
                ShaderLoadTimings.record(ShaderLoadTimings.Stage.TRANSFORM, transformStart);
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                synchronized (shaderTransformationCache) {
                    if(shaderTransformationCache.size() >= CACHE_SIZE) {
                        shaderTransformationCache.removeLast();
                    }
                    shaderTransformationCache.putAndMoveToFirst(key, result);
                }
            }

            return result;
        }
    }

    private static final Pattern versionPattern = Pattern.compile("#version\\s+(\\d+)(?:\\s+(\\w+))?");

    private static final Map<Integer, List<String>> versionedReservedWords = new HashMap<>();;

    static {
        versionedReservedWords.put(400, List.of("sample"));
    }

    /**
     * Renames identifiers that are keywords to the AST parser but valid in the given GLSL version, prefixing them
     * with {@code iris_renamed_}.
     */
    public static String renameReservedWords(String input, int version) {
        // texture() calls keep their name, only variables called texture are renamed.
        input = renameWord(input, "texture", true);

        for (int reservedVersion : versionedReservedWords.keySet()) {
            if (version < reservedVersion) {
                for (String reservedWord : versionedReservedWords.get(reservedVersion)) {
                    input = renameWord(input, reservedWord, false);
                }
            }
        }

        return input;
    }

    /**
     * Prefixes every whole-word occurrence of {@code word} with {@code iris_renamed_}, matching what the regex
     * {@code \bword\b} finds. A regex starting with {@code \b} can't use Java's fast literal search and tries a match
     * at every position of these large sources, so this searches for the word directly.
     */
    private static String renameWord(String input, String word, boolean skipCalls) {
        int index = input.indexOf(word);

        if (index < 0) {
            return input;
        }

        StringBuilder builder = null;
        int copiedUpTo = 0;

        while (index >= 0) {
            int end = index + word.length();

            if ((index == 0 || !isWordChar(input.charAt(index - 1))) && (end == input.length() || !isWordChar(input.charAt(end)))
                    && !(skipCalls && isFollowedByParenthesis(input, end))) {
                if (builder == null) {
                    builder = new StringBuilder(input.length() + 64);
                }

                builder.append(input, copiedUpTo, index).append("iris_renamed_").append(word);
                copiedUpTo = end;
            }

            index = input.indexOf(word, end);
        }

        if (builder == null) {
            return input;
        }

        return builder.append(input, copiedUpTo, input.length()).toString();
    }

    // The characters Java 17's regex \b treats as part of a word.
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // Matches the regex \s*\(
    private static boolean isFollowedByParenthesis(String input, int index) {
        while (index < input.length()) {
            char c = input.charAt(index);

            if (c == '(') {
                return true;
            } else if (c != ' ' && c != '\t' && c != '\n' && c != '\u000B' && c != '\f' && c != '\r') {
                return false;
            }

            index++;
        }

        return false;
    }

    private static <P extends Parameters> Map<ShaderType, String> transformInternal(String name, Map<ShaderType, String> inputs, Patch patchType, P parameters) {
        EnumMap<ShaderType, String> result = new EnumMap<>(ShaderType.class);
        EnumMap<ShaderType, Transformer> types = new EnumMap<>(ShaderType.class);
        EnumMap<ShaderType, String> prepatched = new EnumMap<>(ShaderType.class);

        for (ShaderType type : ShaderType.values()) {
            parameters.type = type;
            if (inputs.get(type) == null) {
                continue;
            }

            String input = inputs.get(type);

            Matcher matcher = versionPattern.matcher(input);
            if (!matcher.find()) {
                throw new IllegalArgumentException("No #version directive found in source code!");
            }

            String versionString = matcher.group(1);
            if (versionString == null) {
                continue;
            }

            String profile = "";
            int versionInt = Integer.parseInt(versionString);
            if (versionInt >= 150) {
                profile = matcher.group(2);
                if (profile == null) {
                    profile = "core";
                }
            }

            String profileString = "#version " + versionString + " " + profile + "\n";

            input = renameReservedWords(input, versionInt);

            var parsedShader = ShaderParser.parseShader(input);
            var translationUnit = parsedShader.full();

            var transformer = new Transformer(translationUnit);
            if (Objects.requireNonNull(parameters.patch) == Patch.COMPUTE) {
                // Always core profile
                if (Integer.parseInt(versionString) < 330) {
                    profileString = "#version 330 core";
                } else {
                    profileString = "#version " + versionString + " core";
                }
                commonPatch(transformer, parameters, true);
            } else {
                boolean isLine = (parameters.patch == Patch.VANILLA && ((VanillaParameters) parameters).isLines());
                if (isLine || (profile == null && Integer.parseInt(versionString) >= 150 || profile != null && profile.equals("core"))) {
                    if (Integer.parseInt(versionString) < 330) {
                        profileString = "#version 330 core";
                    }

                    switch(patchType) {
                        case SODIUM:
                            SodiumTransformer.patchSodiumCore(transformer, (SodiumParameters)parameters);
                            break;
                        case COMPOSITE:
                            CompositeTransformer.patchCompositeCore(transformer, parameters);
                            break;
                        case VANILLA:
                            VanillaTransformer.patchVanillaCore(transformer, (VanillaParameters)parameters);
                            break;
                        default:
                            throw new IllegalStateException("Unknown patch type: " + patchType.name());
                    }
                } else {
                    if (Integer.parseInt(versionString) < 330) {
                        profileString = "#version 330 core";
                    } else {
                        profileString = "#version " + versionString + " core";
                    }
                    switch(patchType) {
                        case SODIUM:
                            SodiumTransformer.patchSodium(transformer, (SodiumParameters)parameters);
                            break;
                        case COMPOSITE:
                            CompositeTransformer.patchComposite(transformer, parameters);
                            break;
                        case VANILLA:
                            VanillaTransformer.patchVanilla(transformer, (VanillaParameters)parameters);
                            break;
                        default:
                            throw new IllegalStateException("Unknown patch type: " + patchType.name());
                    }
                }
            }
            CompTransformer.transformEach(transformer, parameters);
            types.put(type, transformer);

            var extensions = versionPattern.matcher(getFormattedShader(parsedShader.pre(), "")).replaceFirst("").trim();
            prepatched.put(type, profileString + "\n" + extensions);
        }
        CompTransformer.transformGrouped(types, parameters);
        for (var entry : types.entrySet()) {
            // TODO - move printing of shaders into glsl-transformation-lib itself
            entry.getValue().mutateTree(tree -> {
                String header = prepatched.get(entry.getKey());
                if (entry.getKey() == ShaderType.VERTEX && patchType == Patch.SODIUM) {
                    // Inject _vert_init contents
                    header += computeCeleritasHeader((SodiumParameters)parameters);
                }
                String formattedShader = getFormattedShader(tree, header);
                result.put(entry.getKey(), formattedShader);
            });
        }
        return result;
    }

    private static String computeCeleritasHeader(SodiumParameters parameters) {
        var defines = parameters.vertexType.getDefines();
        String chunkVertexHeader = org.embeddedt.embeddium.impl.gl.shader.ShaderParser.parseShader(ShaderLoader.getShaderSource("sodium:include/chunk_vertex.glsl"), ShaderLoader::getShaderSource,
                ShaderConstants.builder().addAll(defines).build());
        return "\n\n" + chunkVertexHeader + "\n\n";
    }

    private static void patchVanillaCore(Transformer translationUnit, VanillaParameters parameters) {
        commonPatch(translationUnit, parameters, true);

    }

    public static void applyIntelHd4000Workaround(Transformer translationUnit) {
        translationUnit.renameFunctionCall("ftransform", "iris_ftransform");
    }


    public static void replaceGlMultiTexCoordBounded(Transformer translationUnit, int min, int max) {
        for (int i = min; i <= max; i++) {
            translationUnit.replaceExpression("gl_MultiTexCoord" + i, "vec4(0.0, 0.0, 0.0, 1.0)");
        }
    }

    public static void patchMultiTexCoord3(Transformer translationUnit, Parameters parameters) {
        if (parameters.type == ShaderType.VERTEX && translationUnit.hasVariable("gl_MultiTexCoord3") && !translationUnit.hasVariable("mc_midTexCoord")) {
            translationUnit.rename("gl_MultiTexCoord3", "mc_midTexCoord");
            translationUnit.injectVariable("attribute vec4 mc_midTexCoord;");
        }
    }

    public static void replaceMidTexCoord(Transformer translationUnit, float textureScale) {
        int type = translationUnit.findType("mc_midTexCoord");
        if (type != 0) {
            translationUnit.removeVariable("mc_midTexCoord");
        }
        translationUnit.replaceExpression("mc_midTexCoord", "iris_MidTex");
        switch (type) {
            case 0:
                return;
            case GLSLLexer.BOOL:
                return;
            case GLSLLexer.FLOAT:
                translationUnit.injectFunction("float iris_MidTex = (mc_midTexCoord.x * " + textureScale + ").x;"); //TODO go back to variable if order is fixed
                break;
            case GLSLLexer.VEC2:
                translationUnit.injectFunction("vec2 iris_MidTex = (mc_midTexCoord.xy * " + textureScale + ").xy;");
                break;
            case GLSLLexer.VEC3:
                translationUnit.injectFunction("vec3 iris_MidTex = vec3(mc_midTexCoord.xy * " + textureScale + ", 0.0);");
                break;
            case GLSLLexer.VEC4:
                translationUnit.injectFunction("vec4 iris_MidTex = vec4(mc_midTexCoord.xy * " + textureScale + ", 0.0, 1.0);");
                break;
            default:

        }

        translationUnit.injectVariable("in vec2 mc_midTexCoord;"); //TODO why is this inserted oddly?

    }

    /**
     * Declares an attribute the vertex format provides as integers with its integer type, and converts each use of it
     * to the type the shader declared it with. Integer attributes are read through {@code iris_<name>}.
     */
    public static void patchIntegerAttribute(Transformer translationUnit, String name, int components) {
        int type = translationUnit.findType(name);
        String attribute = "iris_" + name;
        String conversion = switch (type) {
            case GLSLLexer.FLOAT -> "float(" + attribute + ".x)";
            case GLSLLexer.INT -> attribute + ".x";
            case GLSLLexer.VEC2 -> "vec2(" + padComponents(attribute, components, 2, "0.0", "1.0") + ")";
            case GLSLLexer.VEC3 -> "vec3(" + padComponents(attribute, components, 3, "0.0", "1.0") + ")";
            case GLSLLexer.VEC4 -> "vec4(" + padComponents(attribute, components, 4, "0.0", "1.0") + ")";
            case GLSLLexer.IVEC2 -> "ivec2(" + padComponents(attribute, components, 2, "0", "1") + ")";
            case GLSLLexer.IVEC3 -> "ivec3(" + padComponents(attribute, components, 3, "0", "1") + ")";
            case GLSLLexer.IVEC4 -> "ivec4(" + padComponents(attribute, components, 4, "0", "1") + ")";
            default -> null;
        };

        if (conversion == null) {
            return;
        }

        translationUnit.removeVariable(name);
        translationUnit.replaceExpression(name, conversion);
        translationUnit.injectVariable("in ivec" + components + " " + attribute + ";");
    }

    // Missing components read as they would from a float attribute: zero, and one in the fourth.
    private static String padComponents(String attribute, int available, int declared, String zero, String one) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < declared; i++) {
            if (i > 0) {
                builder.append(", ");
            }

            if (i < available) {
                builder.append(attribute).append('.').append("xyzw".charAt(i));
            } else {
                builder.append(i == 3 ? one : zero);
            }
        }

        return builder.toString();
    }

    public static void addIfNotExists(Transformer translationUnit, String name, String code) {
        if (!translationUnit.hasVariable(name)) {
            translationUnit.injectVariable(code);
        }
    }

    public static void addIfNotExistsType(Transformer translationUnit, String name, String type) {
        if (!translationUnit.hasVariable(name)) {
            translationUnit.injectVariable(type + " " + name + ";");
        }
    }

    private static final Map<String, String> COMMON_TEXTURE_RENAMES = Map.ofEntries(
            Map.entry("texture2D", "texture"),
            Map.entry("texture3D", "texture"),
            Map.entry("texture2DLod", "textureLod"),
            Map.entry("texture3DLod", "textureLod"),
            Map.entry("texture2DProj", "textureProj"),
            Map.entry("texture3DProj", "textureProj"),
            Map.entry("texture2DGrad", "textureGrad"),
            Map.entry("texture2DGradARB", "textureGrad"),
            Map.entry("texture3DGrad", "textureGrad"),
            Map.entry("texelFetch2D", "texelFetch"),
            Map.entry("texelFetch3D", "texelFetch"),
            Map.entry("textureSize2D", "textureSize"));

    public static void commonPatch(Transformer root, Parameters parameters, boolean core) {
        root.rename("gl_FogFragCoord", "iris_FogFragCoord");
        if (parameters.type == ShaderType.VERTEX) {
            root.injectVariable("out float iris_FogFragCoord;");
            root.prependMain("iris_FogFragCoord = 0.0f;");
        } else if (parameters.type == ShaderType.FRAGMENT) {
            root.injectVariable("in float iris_FogFragCoord;");
        }

        if (parameters.type == ShaderType.VERTEX) {
            root.injectVariable("vec4 iris_FrontColor;");
            root.rename("gl_FrontColor", "iris_FrontColor");
        }

        if (parameters.type == ShaderType.FRAGMENT) {
            if (root.containsCall("gl_FragColor")) {
                root.replaceExpression("gl_FragColor", "gl_FragData[0]", GLSLParser::unary_expression);
            }

            if (root.containsCall("gl_TexCoord")) {
                root.rename("gl_TexCoord", "irs_texCoords");
                root.injectVariable("in vec4 irs_texCoords[3];");
            }

            if (root.containsCall("gl_Color")) {
                root.rename("gl_Color", "irs_Color");
                root.injectVariable("in vec4 irs_Color;");
            }

            Set<Integer> found = new HashSet<>();
            root.renameArray("gl_FragData", "iris_FragData", found);

            for (Integer i : found) {
                root.injectFunction("layout (location = " + i + ") out vec4 iris_FragData" + i + ";");
            }

            if ((parameters.getAlphaTest() != AlphaTest.ALWAYS && !core) && found.contains(0)) {
                root.injectVariable("uniform float iris_currentAlphaTest;");
                root.appendMain(parameters.getAlphaTest().toExpression("iris_FragData0.a", "iris_currentAlphaTest", ""));
            }

        }

        if (parameters.type == ShaderType.VERTEX || parameters.type == ShaderType.FRAGMENT) {
            upgradeStorageQualifiers(root, parameters);
        }

        if (root.containsCall("texture") && root.hasVariable("texture")) {
            root.rename("texture", "gtexture");
        }

        if (root.containsCall("gcolor") && root.hasVariable("gcolor")) {
            root.rename("gcolor", "gtexture");
        }

        root.rename("gl_Fog", "iris_Fog");
        root.injectVariable("uniform float iris_FogDensity;");
        root.injectVariable("uniform float iris_FogStart;");
        root.injectVariable("uniform float iris_FogEnd;");
        root.injectVariable("uniform vec4 iris_FogColor;");
        root.injectFunction("struct iris_FogParameters {vec4 color;float density;float start;float end;float scale;};");
        root.injectFunction("iris_FogParameters iris_Fog = iris_FogParameters(iris_FogColor, iris_FogDensity, iris_FogStart, iris_FogEnd, 1.0f / (iris_FogEnd - iris_FogStart));");

        root.renameFunctionCall(COMMON_TEXTURE_RENAMES);
        root.renameAndWrapShadow("shadow2D", "texture");
        root.renameAndWrapShadow("shadow2DLod", "textureLod");
    }



    public static void upgradeStorageQualifiers(Transformer root, Parameters parameters) {
        for (TerminalNode node : root.collectStorage()) {
            if (!(node.getSymbol() instanceof CommonToken token)) {
                return;
            }
            if (token.getType() == GLSLParser.ATTRIBUTE) {
                token.setType(GLSLParser.IN);
                token.setText(GLSLParser.VOCABULARY.getLiteralName(GLSLParser.IN).replace("'", ""));
            }
            else if (token.getType() == GLSLParser.VARYING) {
                if (parameters.type == ShaderType.VERTEX) {
                    token.setType(GLSLParser.OUT);
                    token.setText(GLSLParser.VOCABULARY.getLiteralName(GLSLParser.OUT).replace("'", ""));
                } else {
                    token.setType(GLSLParser.IN);
                    token.setText(GLSLParser.VOCABULARY.getLiteralName(GLSLParser.IN).replace("'", ""));
                }
            }
        }
    }

    public static String getFormattedShader(ParseTree tree, String string) {
        return string + "\n" + ShaderPrinter.getFormattedShader(tree);
    }
}
