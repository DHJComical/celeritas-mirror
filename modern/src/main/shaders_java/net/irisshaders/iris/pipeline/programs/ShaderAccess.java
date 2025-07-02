package net.irisshaders.iris.pipeline.programs;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.compat.mc.MCShaderInstance;

public class ShaderAccess {
	public static MCShaderInstance getParticleTranslucentShader() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline) {
            MCShaderInstance override = ((ShaderRenderingPipeline) pipeline).getShaderMap().getShader(ModernShaderKey.PARTICLES_TRANS);

			if (override != null) {
				return override;
			}
		}

		return (MCShaderInstance) GameRenderer.getParticleShader();
	}
}
