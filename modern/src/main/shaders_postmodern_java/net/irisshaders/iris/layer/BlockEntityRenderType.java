package net.irisshaders.iris.layer;

import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;

/**
 * A render type that draws in the block entities phase. Block entities submit their geometry with these in place of
 * their own render types, so that it's batched apart from entities' and drawn with block entity programs.
 */
public final class BlockEntityRenderType extends RenderType {
	private static final Map<RenderType, BlockEntityRenderType> WRAPPED = new Reference2ObjectOpenHashMap<>();
	// Whether block entities are being submitted.
	private static boolean submitting;

	private final RenderType wrapped;

	private BlockEntityRenderType(RenderType wrapped) {
		super("iris:block_entity", wrapped.state);
		this.wrapped = wrapped;
	}

	public static void setSubmitting(boolean submitting) {
		BlockEntityRenderType.submitting = submitting;
	}

	/**
	 * Returns the render type to submit geometry with, which is a block entity one while block entities are submitted.
	 */
	public static RenderType wrapIfSubmitting(RenderType renderType) {
		if (!submitting || renderType instanceof BlockEntityRenderType) {
			return renderType;
		}

		return WRAPPED.computeIfAbsent(renderType, BlockEntityRenderType::new);
	}

	@Override
	public void draw(MeshData mesh) {
		WorldRenderingPhase previous = GbufferPrograms.getCurrentPhase();
		GbufferPrograms.setPhase(WorldRenderingPhase.BLOCK_ENTITIES);

		try {
			wrapped.draw(mesh);
		} finally {
			GbufferPrograms.setPhase(previous);
		}
	}
}
