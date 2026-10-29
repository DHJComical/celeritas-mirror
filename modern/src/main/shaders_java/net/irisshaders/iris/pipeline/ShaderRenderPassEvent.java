package net.irisshaders.iris.pipeline;

import net.minecraft.client.Camera;
import org.joml.Matrix4fc;
import org.taumc.celeritas.api.eventbus.EmbeddiumEvent;
import org.taumc.celeritas.api.eventbus.EventHandlerRegistrar;

/**
 * Posted at the points of a shader pack's frame where renderers outside the level renderer draw their geometry.
 * Internal to Celeritas; not a supported extension point.
 */
public class ShaderRenderPassEvent extends EmbeddiumEvent {
	public static final EventHandlerRegistrar<ShaderRenderPassEvent> BUS = new EventHandlerRegistrar<>();

	public enum Stage {
		/** Shadow pass, before block entities. */
		SHADOW_SOLID,
		/** Shadow pass, before translucent terrain. Only posted if the pack renders translucent shadows. */
		SHADOW_TRANSLUCENT,
		/** Main pass, after translucent terrain and the remaining translucent entity batches. */
		TRANSLUCENT
	}

	private final Stage stage;
	private final Matrix4fc modelView;
	private final Matrix4fc projection;
	private final Camera camera;
	private final float partialTick;

	public ShaderRenderPassEvent(Stage stage, Matrix4fc modelView, Matrix4fc projection, Camera camera, float partialTick) {
		this.stage = stage;
		this.modelView = modelView;
		this.projection = projection;
		this.camera = camera;
		this.partialTick = partialTick;
	}

	public Stage getStage() {
		return stage;
	}

	public Matrix4fc getModelView() {
		return modelView;
	}

	public Matrix4fc getProjection() {
		return projection;
	}

	public Camera getCamera() {
		return camera;
	}

	public float getPartialTick() {
		return partialTick;
	}
}
