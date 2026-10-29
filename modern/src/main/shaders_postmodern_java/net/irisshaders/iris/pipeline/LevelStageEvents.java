package net.irisshaders.iris.pipeline;

import net.irisshaders.iris.Iris;
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
		// Lowest priority, so that everything opaque other mods draw here precedes the deferred passes.
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (RenderLevelStageEvent.AfterOpaqueFeatures event) -> {
			WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

			if (pipeline != null) {
				pipeline.beginHand();
				pipeline.beginTranslucents();
			}
		});
	}
}
