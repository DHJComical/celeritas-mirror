package net.irisshaders.iris.pipeline;
import net.irisshaders.iris.shaderpack.materialmap.ModernWorldRenderingSettings;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.Minecraft;

public class ModernVanillaRenderingPipeline extends VanillaRenderingPipeline {
    public ModernVanillaRenderingPipeline() {
        super();
        ModernWorldRenderingSettings.INSTANCE.setBlockTypeIds(null);
        ModernWorldRenderingSettings.INSTANCE.setFallbackTextureMaterialMapping(null);
    }


    @Override
    public ParticleRenderingSettings getParticleRenderingSettings() {
        return Minecraft.useShaderTransparency() ? ParticleRenderingSettings.AFTER : ParticleRenderingSettings.MIXED;
    }
}
