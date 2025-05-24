package net.irisshaders.iris.pipeline;

import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;

public class ArchaicVanillaRenderingPipeline extends VanillaRenderingPipeline{
    public ArchaicVanillaRenderingPipeline() {
        super();
        // TODO: 1.7.10 specific setup
        // ModernWorldRenderingSettings.INSTANCE.setBlockTypeIds(null);
        // ModernWorldRenderingSettings.INSTANCE.setFallbackTextureMaterialMapping(null);
    }
    @Override
    public ParticleRenderingSettings getParticleRenderingSettings() {
        // TODO
        // return Minecraft.useShaderTransparency() ? ParticleRenderingSettings.AFTER : ParticleRenderingSettings.MIXED;
        return true ? ParticleRenderingSettings.AFTER : ParticleRenderingSettings.MIXED;
    }
}
