package net.irisshaders.iris.compat.flywheel.engine;

import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

public record ProgramGroupDepthInfo(int textureId, int width, int height)
{
    public static ProgramGroupDepthInfo of(IrisRenderingPipeline irisPipeline, ClrwlProgramGroup programGroup)
    {
        return switch (programGroup)
        {
            case GBUFFERS ->
            {
                var targets = irisPipeline.getRenderTargets();
                yield new ProgramGroupDepthInfo(targets.getDepthTexture(), targets.getCurrentWidth(), targets.getCurrentHeight());
            }
            case SHADOW ->
            {
                var targets = irisPipeline.getShadowRenderTargets();
                yield new ProgramGroupDepthInfo(targets.getDepthTexture().getTextureId(), targets.getResolution(), targets.getResolution());
            }
        };
    }
}
