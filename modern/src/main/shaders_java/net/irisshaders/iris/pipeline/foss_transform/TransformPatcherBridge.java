package net.irisshaders.iris.pipeline.foss_transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.parameter.ComputeParameters;
import net.irisshaders.iris.pipeline.transform.parameter.DHParameters;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.pipeline.transform.parameter.TextureStageParameters;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.Map;

public class TransformPatcherBridge {

    private static Map<ShaderType, String> transform(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
                                                     Parameters parameters) {
        return ShaderTransformer.transform(name, vertex, geometry, tessControl, tessEval, fragment, parameters);
    }

    private static Map<ShaderType, String> transformCompute(String name, String compute,
                                                          Parameters parameters) {
        return ShaderTransformer.transformCompute(name, compute, parameters);
    }

    public static Map<ShaderType, String> patchVanilla(
            String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
            AlphaTest alpha, boolean isLines,
            boolean hasChunkOffset,
            ShaderAttributeInputs inputs,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new VanillaParameters(Patch.VANILLA, textureMap, alpha, isLines, hasChunkOffset, inputs, geometry != null, tessControl != null || tessEval != null));
    }


    public static Map<ShaderType, String> patchDHTerrain(
            String name, String vertex, String tessControl, String tessEval, String geometry, String fragment,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new DHParameters(Patch.DH_TERRAIN, textureMap));
    }


    public static Map<ShaderType, String> patchDHGeneric(
            String name, String vertex, String tessControl, String tessEval, String geometry, String fragment,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new DHParameters(Patch.DH_GENERIC, textureMap));
    }

    public static Map<ShaderType, String> patchSodium(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
                                                           AlphaTest alpha, ShaderAttributeInputs inputs,
                                                           Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, vertex, geometry, tessControl, tessEval, fragment,
                new SodiumParameters(Patch.SODIUM, textureMap, alpha, inputs));
    }

    public static Map<ShaderType, String> patchComposite(
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
                .getOrDefault(ShaderType.COMPUTE, null);
    }
}
