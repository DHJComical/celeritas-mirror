package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws the first-person hand into the shader pack's gbuffers, since vanilla only draws it after the level, once the
 * pack's composite passes have already run. The hands are submitted once, and their solid and translucent features are
 * drawn in the passes for each.
 */
public class HandRenderer {
	public static final HandRenderer INSTANCE = new HandRenderer();
	public static final float DEPTH = 0.125F;

	private final SubmitNodeStorage submits = new SubmitNodeStorage();
	private final Projection projection = new Projection();
	private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("Iris hand projection");
	private final Matrix4f projectionMatrix = new Matrix4f();
	@Nullable
	private FeatureRenderDispatcher dispatcher;
	private boolean submitted;
	private boolean submitting;
	private boolean active;
	private boolean renderingSolid;

	private static boolean canRender(CameraRenderState camera) {
		Minecraft minecraft = Minecraft.getInstance();

		return minecraft.player != null
			&& !camera.isPanoramicMode
			&& minecraft.options.getCameraType().isFirstPerson()
			&& !camera.entityRenderState.isSleeping
			&& !minecraft.options.hideGui
			&& minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR;
	}

	/**
	 * Submits the hands and draws their solid features.
	 */
	public void renderSolid(CameraRenderState camera) {
		if (!Iris.isPackInUseQuick() || !canRender(camera)) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		GameRenderer gameRenderer = minecraft.gameRenderer;
		LocalPlayer player = minecraft.player;
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		// Scaled on z so that the hand doesn't clip into blocks.
		projection.setupPerspective(0.05F, camera.depthFar, camera.hudFov, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
		projectionMatrix.scaling(1.0F, 1.0F, DEPTH).mul(projection.getMatrix(new Matrix4f()));

		PoseStack poseStack = new PoseStack();
		((GameRendererAccessor) gameRenderer).invokeBobHurt(camera, poseStack);

		if (minecraft.options.bobView().get()) {
			((GameRendererAccessor) gameRenderer).invokeBobView(camera, poseStack);
		}

		Profiler.get().push("iris_hand");
		submitting = true;
		gameRenderer.itemInHandRenderer.renderHandsWithItems(partialTick, poseStack, submits, player,
			minecraft.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick));
		submitting = false;
		submitted = true;

		draw(true);
		Profiler.get().pop();
	}

	/**
	 * Draws the translucent features of the hands submitted in the solid pass.
	 */
	public void renderTranslucent() {
		if (!submitted) {
			return;
		}

		Profiler.get().push("iris_hand_translucent");
		draw(false);
		submits.clear();
		submits.endFrame();
		submitted = false;
		Profiler.get().pop();
	}

	private void draw(boolean solid) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderBuffers buffers = minecraft.renderBuffers();
		MultiBufferSource.BufferSource bufferSource = buffers.bufferSource();

		if (dispatcher == null) {
			// Shares the main pass's buffers, which are flushed before and after each half.
			dispatcher = new FeatureRenderDispatcher(submits, minecraft.getModelManager(), bufferSource, minecraft.getAtlasManager(),
				buffers.outlineBufferSource(), buffers.crumblingBufferSource(), minecraft.font, minecraft.gameRenderer.getGameRenderState());
		}

		bufferSource.endBatch();

		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projectionMatrix), ProjectionType.PERSPECTIVE);
		RenderSystem.getModelViewStack().pushMatrix().identity();

		active = true;
		renderingSolid = solid;
		GbufferPrograms.setPhase(solid ? WorldRenderingPhase.HAND_SOLID : WorldRenderingPhase.HAND_TRANSLUCENT);

		if (solid) {
			dispatcher.renderSolidFeatures();
		} else {
			dispatcher.renderTranslucentFeatures();
		}

		bufferSource.endBatch();

		GbufferPrograms.setPhase(WorldRenderingPhase.NONE);
		active = false;
		renderingSolid = false;

		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.restoreProjectionMatrix();
	}

	/**
	 * Whether the hands are being submitted to be drawn by this renderer, rather than by vanilla.
	 */
	public boolean isSubmitting() {
		return submitting;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isRenderingSolid() {
		return renderingSolid;
	}
}
