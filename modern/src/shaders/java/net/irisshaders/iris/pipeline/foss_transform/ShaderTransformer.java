package net.irisshaders.iris.pipeline.foss_transform;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import oculus.org.antlr.v4.runtime.BailErrorStrategy;
import oculus.org.antlr.v4.runtime.BufferedTokenStream;
import oculus.org.antlr.v4.runtime.CharStreams;
import oculus.org.antlr.v4.runtime.CommonToken;
import oculus.org.antlr.v4.runtime.CommonTokenStream;
import oculus.org.antlr.v4.runtime.ConsoleErrorListener;
import oculus.org.antlr.v4.runtime.DefaultErrorStrategy;
import oculus.org.antlr.v4.runtime.atn.PredictionMode;
import oculus.org.antlr.v4.runtime.misc.ParseCancellationException;
import oculus.org.antlr.v4.runtime.tree.ParseTree;
import oculus.org.antlr.v4.runtime.tree.ParseTreeWalker;
import oculus.org.antlr.v4.runtime.tree.TerminalNode;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.StorageCollector;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.Util;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;
import org.taumc.glsl.grammar.GLSLPreParser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ShaderTransformer {
    static String tab = "";

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

            result = shaderTransformationCache.getAndMoveToFirst(key);
            if(result == null || !useCache) {
                result = ShaderTransformationDiskCache.transformIfAbsent(key, () -> transformInternal(name, inputs, patchType, parameters));
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                if(shaderTransformationCache.size() >= CACHE_SIZE) {
                    shaderTransformationCache.removeLast();
                }
                shaderTransformationCache.putAndMoveToLast(key, result);
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

            result = shaderTransformationCache.getAndMoveToFirst(key);
            if(result == null || !useCache) {
                result = transformInternal(name, inputs, patchType, parameters);
                // Clear this, we don't want whatever random type was last transformed being considered for the key
                parameters.type = null;
                if(shaderTransformationCache.size() >= CACHE_SIZE) {
                    shaderTransformationCache.removeLast();
                }
                shaderTransformationCache.putAndMoveToLast(key, result);
            }

            return result;
        }
    }

    private static final Stopwatch CUMULATIVE_WATCH = Stopwatch.createUnstarted();

    private static <P extends Parameters> Map<PatchShaderType, String> transformInternal(String name, EnumMap<PatchShaderType, String> inputs, Patch patchType, P parameters) {
        EnumMap<PatchShaderType, String> result = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, Transformer> types = new EnumMap<>(PatchShaderType.class);
        EnumMap<PatchShaderType, String> prepatched = new EnumMap<>(PatchShaderType.class);

        Stopwatch watch = Stopwatch.createStarted();
        CUMULATIVE_WATCH.start();

        for (PatchShaderType type : PatchShaderType.values()) {
            parameters.type = type;
            if (inputs.get(type) == null) {
                continue;
            }

            var parsedShader = ShaderParser.parseShader(inputs.get(type));
            var pre = parsedShader.pre();
            var translationUnit = parsedShader.full();
            var preparsed = pre.compiler_directive();
            String profile = null;
            String versionString = null;
            GLSLPreParser.Compiler_directiveContext version = null;
            for (var entry: preparsed) {
                if (entry.version_directive() != null) {
                    version = entry;
                    if (entry.version_directive().number() != null) {
                        versionString = entry.version_directive().number().getText();
                    }
                    if (entry.version_directive().profile() != null) {
                        profile = entry.version_directive().profile().getText();
                    }
                }
            }
            pre.children.remove(version);
            if (versionString == null) {
                continue;
            }
            String profileString = "#version " + versionString + " " + profile;
            var transformer = new Transformer(translationUnit);
            if (Objects.requireNonNull(parameters.patch) == Patch.COMPUTE) {
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
            prepatched.put(type, getFormattedShader(pre, profileString));
        }
        CompTransformer.transformGrouped(types, parameters);
        for (var entry : types.entrySet()) {
            // TODO - move printing of shaders into glsl-transformation-lib itself
            entry.getValue().mutateTree(tree -> {
                result.put(entry.getKey(), getFormattedShader(tree, prepatched.get(entry.getKey())));
            });
        }
        watch.stop();
        CUMULATIVE_WATCH.stop();
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
            root.replaceExpression("gl_FrontColor", "iris_FrontColor");
        }

        if (parameters.type.glShaderType == ShaderType.FRAGMENT) {
            if (root.containsCall("gl_FragColor")) {
                root.replaceExpression("gl_FragColor", "gl_FragData[0]");
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
        getFormattedShader(tree, sb);
        return sb.toString();
    }

    private static void getFormattedShader(ParseTree tree, StringBuilder stringBuilder) {
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
                tab = "\t";
            }

            if (text.equals("}")) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 2);
                tab = "";
            }
            stringBuilder.append(text.equals(";") ? " \n" + tab : " ");
        } else {
            for(int i = 0; i < tree.getChildCount(); ++i) {
                getFormattedShader(tree.getChild(i), stringBuilder);
            }
        }

    }

}
