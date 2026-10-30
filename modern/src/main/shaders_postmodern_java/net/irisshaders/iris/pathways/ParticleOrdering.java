package net.irisshaders.iris.pathways;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;

/**
 * Lets the level renderer move particles to where the shader pack's {@code particles.ordering} asks for. Vanilla
 * already draws opaque particles before translucent terrain and translucent ones after it, which is
 * {@link ParticleRenderingSettings#MIXED}.
 */
public interface ParticleOrdering {
	/**
	 * Whether {@code renderSolidFeatures} leaves out opaque particles, for them to be drawn later with
	 * {@link #iris$renderSolidParticles()}.
	 */
	void iris$setDeferSolidParticles(boolean defer);

	void iris$renderSolidParticles();

	static ParticleRenderingSettings current() {
		return Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline pipeline
			? pipeline.getParticleRenderingSettings()
			: ParticleRenderingSettings.MIXED;
	}
}
