package net.irisshaders.iris.shadows;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.irisshaders.iris.mixin.LevelRendererShadowAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Draws entities and block entities into the shadow map. Like vanilla's main pass, they're extracted into a render
 * state, submitted, and then drawn in batches, but with a render state and submissions of their own.
 */
final class ShadowEntityRenderer {
	private final LevelRenderState renderState = new LevelRenderState();
	private final SubmitNodeStorage submits = new SubmitNodeStorage();
	@Nullable
	private FeatureRenderDispatcher dispatcher;

	/**
	 * Extracts the entities the frustum contains, or only the player, its passengers and its vehicle.
	 *
	 * @return how many entities were extracted
	 */
	int extractEntities(Camera camera, Frustum frustum, boolean onlyPlayer) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		Vec3 cameraPos = camera.position();

		camera.extractRenderState(renderState.cameraRenderState, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));

		if (onlyPlayer) {
			Entity player = minecraft.player;

			if (player != null) {
				for (Entity passenger : player.getPassengers()) {
					extractEntity(dispatcher, level, passenger);
				}

				if (player.getVehicle() != null) {
					extractEntity(dispatcher, level, player.getVehicle());
				}

				if (dispatcher.shouldRender(player, frustum, cameraPos.x, cameraPos.y, cameraPos.z) && !player.isSpectator()) {
					extractEntity(dispatcher, level, player);
				}
			}
		} else {
			for (Entity entity : level.entitiesForRendering()) {
				if (dispatcher.shouldRender(entity, frustum, cameraPos.x, cameraPos.y, cameraPos.z) && !entity.isSpectator()) {
					extractEntity(dispatcher, level, entity);
				}
			}
		}

		return renderState.entityRenderStates.size();
	}

	private void extractEntity(EntityRenderDispatcher dispatcher, ClientLevel level, Entity entity) {
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(!level.tickRateManager().isEntityFrozen(entity));
		renderState.entityRenderStates.add(dispatcher.extractEntity(entity, partialTick));
	}

	/**
	 * Extracts the block entities in the shadow pass's visible sections that pass the filter.
	 *
	 * @return how many block entities were extracted
	 */
	int extractBlockEntities(CeleritasWorldRenderer celeritas, Camera camera, float partialTick, @Nullable Predicate<BlockEntity> filter) {
		Vec3 cameraPos = camera.position();
		celeritas.renderBlockEntities(new CeleritasWorldRenderer.BlockEntityRenderContext(new PoseStack(), renderState,
			cameraPos.x, cameraPos.y, cameraPos.z, Long2ObjectMaps.emptyMap(), partialTick, filter));

		return renderState.blockEntityRenderStates.size();
	}

	/**
	 * Submits and draws everything extracted, and forgets it.
	 */
	void render(LevelRenderer levelRenderer) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderBuffers buffers = minecraft.renderBuffers();

		if (dispatcher == null) {
			// Shares the main pass's buffers, which are empty while the shadow pass runs.
			dispatcher = new FeatureRenderDispatcher(submits, minecraft.getModelManager(), buffers.bufferSource(), minecraft.getAtlasManager(),
				buffers.outlineBufferSource(), buffers.crumblingBufferSource(), minecraft.font, minecraft.gameRenderer.getGameRenderState());
		}

		PoseStack poseStack = new PoseStack();
		LevelRendererShadowAccessor accessor = (LevelRendererShadowAccessor) levelRenderer;
		accessor.invokeSubmitEntities(poseStack, renderState, submits);
		accessor.invokeSubmitBlockEntities(poseStack, renderState, submits);
		dispatcher.renderAllFeatures();
		buffers.bufferSource().endBatch();

		renderState.entityRenderStates.clear();
		renderState.blockEntityRenderStates.clear();
	}
}
