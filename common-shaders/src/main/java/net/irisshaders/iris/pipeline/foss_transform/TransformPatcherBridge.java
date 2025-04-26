package net.irisshaders.iris.pipeline.foss_transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.ComputeParameters;
import net.irisshaders.iris.pipeline.transform.parameter.DHParameters;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.pipeline.transform.parameter.TextureStageParameters;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.lang.reflect.Method;
import java.util.Map;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class TransformPatcherBridge {
    private static final boolean USE_GLSL_TRANSFORMER;

    private static final Method transformPatcherTransform, transformPatcherTransformCompute;

    static {
        boolean useGlslTransformer = Boolean.getBoolean("cornea.use_glsl_transformer");
        Method transform = null, transformCompute = null;
        try {
            Class<?> transformPatcher = Class.forName("net.irisshaders.iris.pipeline.transform.TransformPatcher");
            transform = transformPatcher.getDeclaredMethod("transform", String.class, String.class, String.class, String.class, String.class, String.class, Parameters.class);
            transformCompute = transformPatcher.getDeclaredMethod("transformCompute", String.class, String.class, Parameters.class);
        } catch(ReflectiveOperationException e) {
        }

        if(useGlslTransformer && transform == null) {
            IRIS_LOGGER.warn("glsl-transformer is requested, but is not available in this jar");
            useGlslTransformer = false;
        }

        transformPatcherTransform = transform;
        transformPatcherTransformCompute = transformCompute;
        USE_GLSL_TRANSFORMER = useGlslTransformer;
    }

    private static Map<PatchShaderType, String> transform(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
                                                          Parameters parameters) {
        if (USE_GLSL_TRANSFORMER) {
            try {
                return (Map<PatchShaderType, String>)transformPatcherTransform.invoke(null, name, vertex, geometry, tessControl, tessEval, fragment, parameters);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            return ShaderTransformer.transform(name, vertex, geometry, tessControl, tessEval, fragment, parameters);
        }
    }

    private static Map<PatchShaderType, String> transformCompute(String name, String compute,
                                                          Parameters parameters) {
        if (USE_GLSL_TRANSFORMER) {
            try {
                return (Map<PatchShaderType, String>)transformPatcherTransformCompute.invoke(null, name, compute, parameters);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        } else {
            return ShaderTransformer.transformCompute(name, compute, parameters);
        }
    }

    public static Map<PatchShaderType, String> patchVanilla(
            String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
            AlphaTest alpha, boolean isLines,
            boolean hasChunkOffset,
            ShaderAttributeInputs inputs,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new VanillaParameters(Patch.VANILLA, textureMap, alpha, isLines, hasChunkOffset, inputs, geometry != null, tessControl != null || tessEval != null));
    }


    public static Map<PatchShaderType, String> patchDHTerrain(
            String name, String vertex, String tessControl, String tessEval, String geometry, String fragment,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new DHParameters(Patch.DH_TERRAIN, textureMap));
    }


    public static Map<PatchShaderType, String> patchDHGeneric(
            String name, String vertex, String tessControl, String tessEval, String geometry, String fragment,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new DHParameters(Patch.DH_GENERIC, textureMap));
    }

    public static Map<PatchShaderType, String> patchSodium(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
                                                           AlphaTest alpha, ShaderAttributeInputs inputs,
                                                           Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new SodiumParameters(Patch.SODIUM, textureMap, alpha, inputs));
    }

    public static Map<PatchShaderType, String> patchComposite(
            String name, String vertex, String geometry, String fragment,
            TextureStage stage,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, null, null, fragment, new TextureStageParameters(Patch.COMPOSITE, stage, textureMap));
    }

    public static String patchCompute(
            String name, String compute,
            TextureStage stage,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transformCompute(name, compute, new ComputeParameters(Patch.COMPUTE, stage, textureMap))
                .getOrDefault(PatchShaderType.COMPUTE, null);
    }
}
