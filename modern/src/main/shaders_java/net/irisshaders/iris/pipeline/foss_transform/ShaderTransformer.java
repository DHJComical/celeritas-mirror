package net.irisshaders.iris.pipeline.foss_transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import org.apache.commons.lang3.mutable.MutableObject;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.StorageCollector;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.CommonToken;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.tree.ParseTree;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.taumc.glsl.shadowed.org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderTransformer {

    private static final int CACHE_SIZE = 100;
    private static final Object2ObjectLinkedOpenHashMap<TransformKey, Map<PatchShaderType, String>> shaderTransformationCache = new Object2ObjectLinkedOpenHashMap<>();
    public static final boolean useCache = true;

    public record TransformKey<P extends Parameters>(Patch patchType, EnumMap<PatchShaderType, String> inputs, P params) {}

    public static <P extends Parameters> Map<PatchShaderType, String> transform(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment, P parameters) {
        if (vertex == null && geometry == null && tessControl == null && tessEval == null && fragment == null) {
            return null;
        } else {
            Map<PatchShaderType, String> result;

            var patchType = parameters.patch;

            EnumMap<PatchShaderType, String> inputs = new EnumMap<>(PatchShaderType.class);
            inputs.put(PatchShaderType.VERTEX, vertex);
            inputs.put(PatchShaderType.GEOMETRY, geometry);
            inputs.put(PatchShaderType.TESS_CONTROL, tessControl);
            inputs.put(PatchShaderType.TESS_EVAL, tessEval);
            inputs.put(PatchShaderType.FRAGMENT, fragment);

            var key = new TransformKey(patchType, inputs, parameters);

            synchronized (shaderTransformationCache) {
                result = shaderTransformationCache.getAndMoveToFirst(key);
            }

            if(result == null || !useCache) {
                result = ShaderTransformationDiskCache.transformIfAbsent(key, () -> transformInternal(name, inputs, patchType, parameters));
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                synchronized (shaderTransformationCache) {
                    if(shaderTransformationCache.size() >= CACHE_SIZE) {
                        shaderTransformationCache.removeLast();
                    }
                    shaderTransformationCache.putAndMoveToLast(key, result);
                }
            }

            return result;
        }
    }

    public static <P extends Parameters> Map<PatchShaderType, String> transformCompute(String name, String compute, P parameters) {
        if (compute == null) {
            return null;
        } else {
            Map<PatchShaderType, String> result;

            var patchType = parameters.patch;

            EnumMap<PatchShaderType, String> inputs = new EnumMap<>(PatchShaderType.class);
            inputs.put(PatchShaderType.COMPUTE, compute);

            var key = new TransformKey(patchType, inputs, parameters);

            synchronized (shaderTransformationCache) {
                result = shaderTransformationCache.getAndMoveToFirst(key);
            }
            if(result == null || !useCache) {
                result = transformInternal(name, inputs, patchType, parameters);
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                synchronized (shaderTransformationCache) {
                    if(shaderTransformationCache.size() >= CACHE_SIZE) {
                        shaderTransformationCache.removeLast();
                    }
                    shaderTransformationCache.putAndMoveToLast(key, result);
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

    private static final Pattern texturePattern = Pattern.compile("\\btexture\\s*\\(|(\\btexture\\b)");

    private static String replaceTexture(String input) {
        var matcher = texturePattern.matcher(input);

        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String texMatch = matcher.group(1);
            matcher.appendReplacement(builder, texMatch != null ? matcher.group(0).replaceFirst(Pattern.quote(texMatch), "iris_renamed_texture") : matcher.group(0));
        }
        matcher.appendTail(builder);

        return builder.toString();
    }

    private static <P extends Parameters> Map<PatchShaderType, String> transformInternal(String name, EnumMap<PatchShaderType, String> inputs, Patch patchType, P parameters) {
        EnumMap<PatchShaderType, String> result = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, Transformer> types = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, String> prepatched = new EnumMap<>(PatchShaderType.class);

        for (PatchShaderType type : PatchShaderType.values()) {
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

            // This handles some reserved keywords which cause the AST parser to fail
            // but aren't necessarily invalid for GLSL versions prior to 400. This simple
            // renames the matching strings and prefixes them with iris_renamed_

            input = replaceTexture(input);

            for (int version : versionedReservedWords.keySet()) {
                if (versionInt < version) {
                    for (String reservedWord : versionedReservedWords.get(version)) {
                        String newName = "iris_renamed_" + reservedWord;
                        input = input.replaceAll("\\b" + reservedWord + "\\b", newName);
                    }
                }
            }

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
                result.put(entry.getKey(), getFormattedShader(tree, prepatched.get(entry.getKey())));
            });
        }
        return result;
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
        if (parameters.type.glShaderType == ShaderType.VERTEX && translationUnit.hasVariable("gl_MultiTexCoord3") && !translationUnit.hasVariable("mc_midTexCoord")) {
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
        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            root.injectVariable("out float iris_FogFragCoord;");
            root.prependMain("iris_FogFragCoord = 0.0f;");
        } else if (parameters.type.glShaderType == ShaderType.FRAGMENT) {
            root.injectVariable("in float iris_FogFragCoord;");
        }

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            root.injectVariable("vec4 iris_FrontColor;");
            root.rename("gl_FrontColor", "iris_FrontColor");
        }

        if (parameters.type.glShaderType == ShaderType.FRAGMENT) {
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

        if (parameters.type.glShaderType == ShaderType.VERTEX || parameters.type.glShaderType == ShaderType.FRAGMENT) {
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
        List<TerminalNode> tokens = new ArrayList<>();
        root.mutateTree(tree -> {
            ParseTreeWalker.DEFAULT.walk(new StorageCollector(tokens), tree);
        });

        for (TerminalNode node : tokens) {
            if (!(node.getSymbol() instanceof CommonToken token)) {
                return;
            }
            if (token.getType() == GLSLParser.ATTRIBUTE) {
                token.setType(GLSLParser.IN);
                token.setText(GLSLParser.VOCABULARY.getLiteralName(GLSLParser.IN).replace("'", ""));
            }
            else if (token.getType() == GLSLParser.VARYING) {
                if (parameters.type.glShaderType == ShaderType.VERTEX) {
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
        StringBuilder sb = new StringBuilder(string + "\n");
        MutableObject<String> tab = new MutableObject<>("");
        getFormattedShader(tree, sb, tab);
        return sb.toString();
    }

    private static void getFormattedShader(ParseTree tree, StringBuilder stringBuilder, MutableObject<String> tab) {
        if (tree instanceof TerminalNode) {
            String text = tree.getText();
            if (text.equals("<EOF>")) {
                return;
            }
            if (text.equals("#")) {
                stringBuilder.append("\n#");
                return;
            }
            stringBuilder.append(text);
            if (text.equals("{")) {
                stringBuilder.append(" \n\t");
                tab.setValue("\t");
            }

            if (text.equals("}")) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 2);
                tab.setValue("");
            }
            stringBuilder.append(text.equals(";") ? " \n" + tab.getValue() : " ");
        } else {
            for(int i = 0; i < tree.getChildCount(); ++i) {
                getFormattedShader(tree.getChild(i), stringBuilder, tab);
            }
        }

    }

}
