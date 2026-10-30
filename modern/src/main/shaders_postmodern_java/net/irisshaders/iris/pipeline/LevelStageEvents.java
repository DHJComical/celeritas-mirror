package net.irisshaders.iris.pipeline;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.pathways.HandRenderer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Runs the pipeline's stages that sit between parts of vanilla's main pass.
 */
public final class LevelStageEvents {
	private LevelStageEvents() {
	}

	public static void register() {
		// Ends the phases begun where vanilla starts the sky and each batch of features, so that what other mods
		// draw in these events isn't mistaken for them.
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderLevelStageEvent.AfterSky event) -> endPhase());
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderLevelStageEvent.AfterOpaqueFeatures event) -> endPhase());
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderLevelStageEvent.AfterTranslucentFeatures event) -> endPhase());
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderLevelStageEvent.AfterTranslucentParticles event) -> endPhase());

		// Lowest priority, so that everything opaque other mods draw here precedes the deferred passes.
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderLevelStageEvent.AfterOpaqueFeatures event) -> {
			WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

			if (pipeline != null) {
				pipeline.beginHand();
				HandRenderer.INSTANCE.renderSolid(event.getLevelRenderState().cameraRenderState);
				pipeline.beginTranslucents();
			}
		});
	}

	private static void endPhase() {
		GbufferPrograms.setPhase(WorldRenderingPhase.NONE);
	}
}
