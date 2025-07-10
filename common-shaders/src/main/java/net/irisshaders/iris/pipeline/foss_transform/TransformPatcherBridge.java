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
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

import java.util.Map;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class TransformPatcherBridge {

    private static Map<ShaderType, String> transform(String name, Map<ShaderType, String> sources,
                                                     Parameters parameters) {
        return ShaderTransformer.transform(name, sources, parameters);
    }

    private static Map<ShaderType, String> transformCompute(String name, String compute,
                                                          Parameters parameters) {
        return ShaderTransformer.transformCompute(name, compute, parameters);
    }

    public static Map<ShaderType, String> patchVanilla(
            String name, Map<ShaderType, String> sources,
            AlphaTest alpha, boolean isLines,
            boolean hasChunkOffset,
            ShaderAttributeInputs inputs,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name,sources,
                new VanillaParameters(Patch.VANILLA, textureMap, alpha, isLines, hasChunkOffset, inputs, sources.containsKey(ShaderType.GEOM), sources.containsKey(ShaderType.TESS_CTRL) || sources.containsKey(ShaderType.TESS_EVALUATE)));
    }


    public static Map<ShaderType, String> patchDHTerrain(
            String name, Map<ShaderType, String> sources,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, sources,
                new DHParameters(Patch.DH_TERRAIN, textureMap));
    }


    public static Map<ShaderType, String> patchDHGeneric(
            String name, Map<ShaderType, String> sources,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, sources,
                new DHParameters(Patch.DH_GENERIC, textureMap));
    }

    public static Map<ShaderType, String> patchSodium(String name, Map<ShaderType, String> sources,
                                                      AlphaTest alpha, ShaderAttributeInputs inputs,
                                                      Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap, TerrainRenderPass pass) {
        return transform(name, sources,
                new SodiumParameters(Patch.SODIUM, textureMap, alpha, inputs, pass.vertexType()));
    }

    public static Map<ShaderType, String> patchComposite(
            String name, Map<ShaderType, String> sources,
            TextureStage stage,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transform(name, sources, new TextureStageParameters(Patch.COMPOSITE, stage, textureMap));
    }

    public static String patchCompute(
            String name, String compute,
            TextureStage stage,
            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
        return transformCompute(name, compute, new ComputeParameters(Patch.COMPUTE, stage, textureMap))
                .getOrDefault(ShaderType.COMPUTE, null);
    }
}
