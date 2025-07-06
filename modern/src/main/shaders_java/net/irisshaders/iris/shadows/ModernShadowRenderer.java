package net.irisshaders.iris.shadows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.irisshaders.batchedentityrendering.impl.*;
import net.irisshaders.iris.IrisConstants;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.CommonIrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.irisshaders.iris.shadows.frustum.CommonFrustumHolder;
import net.irisshaders.iris.shadows.frustum.CullEverythingFrustum;
import net.irisshaders.iris.shadows.frustum.ModernFrustumHolder;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.advanced.ReversedAdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.BoxCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModernShadowRenderer extends CommonShadowRenderer {
    public static List<BlockEntity> visibleBlockEntities;
    public static Frustum FRUSTUM;
    private final ShadowCompositeRenderer compositeRenderer;
    private final CommonIrisRenderingPipeline pipeline;
    private final RenderBuffers buffers;
	private final RenderBuffersExt renderBuffersExt;

    public ModernShadowRenderer(CommonIrisRenderingPipeline pipeline, ProgramSource shadow, PackDirectives directives,
                          ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer compositeRenderer, CustomUniforms customUniforms, boolean separateHardwareSamplers) {
        super(shadow, directives, shadowRenderTargets, separateHardwareSamplers);

        this.pipeline = pipeline;

		final PackShadowDirectives shadowDirectives = directives.getShadowDirectives();

		this.compositeRenderer = compositeRenderer;


        int processors = Runtime.getRuntime().availableProcessors();
        int threads = processors;
		this.buffers = new RenderBuffers(/*? if >=1.20.4 {*//*threads*//*?}*/);

		if (this.buffers instanceof RenderBuffersExt) {
			this.renderBuffersExt = (RenderBuffersExt) buffers;
		} else {
			this.renderBuffersExt = null;
		}

		configureSamplingSettings(shadowDirectives);
	}

    @Override
    protected void initFrustumHolders() {
        this.terrainFrustumHolder = new ModernFrustumHolder();
        this.entityFrustumHolder = new ModernFrustumHolder();
    }

    ModernFrustumHolder getTerrainFrustumHolder() {
        return (ModernFrustumHolder) this.terrainFrustumHolder;
    }

    ModernFrustumHolder getEntityFrustumHolder() {
        return (ModernFrustumHolder) this.entityFrustumHolder;
    }

	public static PoseStack createShadowModelView(float sunPathRotation, float intervalSize) {
		// Determine the camera position
		Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();

		double cameraX = cameraPos.x;
		double cameraY = cameraPos.y;
		double cameraZ = cameraPos.z;

		// Set up our modelview matrix stack
		PoseStack modelView = new PoseStack();
		ShadowMatrices.createModelViewMatrix(modelView, getShadowAngle(), intervalSize, sunPathRotation, cameraX, cameraY, cameraZ);

		return modelView;
	}

	private static ClientLevel getLevel() {
		return Objects.requireNonNull(Minecraft.getInstance().level);
	}

    public void setUsesImages(boolean usesImages) {
		this.packHasVoxelization = packHasVoxelization || usesImages;
	}

    private CommonFrustumHolder createShadowFrustum(float renderMultiplier, CommonFrustumHolder holder) {
		// TODO: Cull entities / block entities with Advanced Frustum Culling even if voxelization is detected.
		String distanceInfo;
		String cullingInfo;
		if ((packCullingState == ShadowCullState.DEFAULT && packHasVoxelization) || packCullingState == ShadowCullState.DISTANCE) {
			double distance = halfPlaneLength * renderMultiplier;

			String reason;

			if (packCullingState == ShadowCullState.DISTANCE) {
				reason = "(set by shader pack)";
			} else /*if (packHasVoxelization)*/ {
				reason = "(voxelization detected)";
			}

			if (distance <= 0 || distance > Minecraft.getInstance().options.getEffectiveRenderDistance() * 16) {
				distanceInfo = "render distance = " + Minecraft.getInstance().options.getEffectiveRenderDistance() * 16
					+ " blocks ";
				distanceInfo += Minecraft.getInstance().isLocalServer() ? "(capped by normal render distance)" : "(capped by normal/server render distance)";
				cullingInfo = "disabled " + reason;
				return holder.setInfo(new NonCullingFrustum(), distanceInfo, cullingInfo);
			} else {
				distanceInfo = distance + " blocks (set by shader pack)";
				cullingInfo = "distance only " + reason;
				BoxCuller boxCuller = new BoxCuller(distance);
				holder.setInfo(new BoxCullingFrustum(boxCuller), distanceInfo, cullingInfo);
			}
		} else {
			BoxCuller boxCuller;

			boolean isReversed = packCullingState == ShadowCullState.REVERSED;

			// Assume render multiplier is meant to be 1 if reversed culling is on
			if (isReversed && renderMultiplier < 0) renderMultiplier = 1.0f;

			double distance = (isReversed ? voxelDistance : halfPlaneLength) * renderMultiplier;
			String setter = "(set by shader pack)";

			if (renderMultiplier < 0) {
				distance = IrisVideoSettings.shadowDistance * 16;
				setter = "(set by user)";
			}

			if (distance >= Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 && !isReversed) {
				distanceInfo = "render distance = " + Minecraft.getInstance().options.getEffectiveRenderDistance() * 16
					+ " blocks ";
				distanceInfo += Minecraft.getInstance().isLocalServer() ? "(capped by normal render distance)" : "(capped by normal/server render distance)";
				boxCuller = null;
			} else {
				distanceInfo = distance + " blocks " + setter;

				if (distance == 0.0 && !isReversed) {
					cullingInfo = "no shadows rendered";
					holder.setInfo(new CullEverythingFrustum(), distanceInfo, cullingInfo);
				}

				boxCuller = new BoxCuller(distance);
			}

			cullingInfo = (isReversed ? "Reversed" : "Advanced") + " Frustum Culling enabled";

			Vector4f shadowLightPosition = new CelestialUniforms(sunPathRotation).getShadowLightPositionInWorldSpace();

			Vector3f shadowLightVectorFromOrigin =
				new Vector3f(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());

			shadowLightVectorFromOrigin.normalize();

			if (isReversed) {
				return holder.setInfo(new ReversedAdvancedShadowCullingFrustum(CapturedRenderingState.INSTANCE.getGbufferModelView(),
					(shouldRenderDH && DHCompat.hasRenderingEnabled()) ? DHCompat.getProjection() : CapturedRenderingState.INSTANCE.getGbufferProjection(), shadowLightVectorFromOrigin, boxCuller, new BoxCuller(halfPlaneLength * renderMultiplier)), distanceInfo, cullingInfo);
			} else {
				return holder.setInfo(new AdvancedShadowCullingFrustum(CapturedRenderingState.INSTANCE.getGbufferModelView(),
					(shouldRenderDH && DHCompat.hasRenderingEnabled()) ? DHCompat.getProjection() : CapturedRenderingState.INSTANCE.getGbufferProjection(), shadowLightVectorFromOrigin, boxCuller), distanceInfo, cullingInfo);
			}
		}

		return holder;
	}

	public void setupShadowViewport() {
		// Set up the viewport
		RENDER_SYSTEM.glViewport(0, 0, resolution, resolution);
	}

	public void renderShadows(MCLevelRenderer levelRendererIn, MCCamera playerCamera) {
		if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
			return;
		}
        LevelRendererAccessor levelRenderer = (LevelRendererAccessor) levelRendererIn;

		Minecraft client = Minecraft.getInstance();

		levelRenderer.getLevel().getProfiler().popPush("shadows");
		ACTIVE = true;

		renderDistance = (int) ((halfPlaneLength * renderDistanceMultiplier) / 16);

		if (renderDistanceMultiplier < 0) {
			renderDistance = IrisVideoSettings.shadowDistance;
		}


		visibleBlockEntities = new ArrayList<>();

		// NB: We store the previous player buffers in order to be able to allow mods rendering entities in the shadow pass (Flywheel) to use the shadow buffers instead.
		RenderBuffers playerBuffers = levelRenderer.getRenderBuffers();
		levelRenderer.setRenderBuffers(buffers);

		visibleBlockEntities = new ArrayList<>();
		setupShadowViewport();

		// Create our camera
		PoseStack modelView = createShadowModelView(this.sunPathRotation, this.intervalSize);
		MODELVIEW = new Matrix4f(modelView.last().pose());

		levelRenderer.getLevel().getProfiler().push("terrain_setup");

		levelRenderer.getLevel().getProfiler().push("initialize frustum");

		terrainFrustumHolder = createShadowFrustum(renderDistanceMultiplier, terrainFrustumHolder);

		FRUSTUM = getTerrainFrustumHolder().getWrapper();

		// Determine the player camera position
		Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();

		double cameraX = cameraPos.x();
		double cameraY = cameraPos.y();
		double cameraZ = cameraPos.z();

		// Center the frustum on the player camera position
		getTerrainFrustumHolder().getWrapper().prepare(cameraX, cameraY, cameraZ);

		levelRenderer.getLevel().getProfiler().pop();

		// Disable chunk occlusion culling - it's a bit complex to get this properly working with shadow rendering
		// as-is, however in the future it will be good to work on restoring it for a nice performance boost.
		//
		// TODO: Get chunk occlusion working with shadows
		boolean wasChunkCullingEnabled = client.smartCull;
		client.smartCull = false;

		// Always schedule a terrain update
		// TODO: Only schedule a terrain update if the sun / moon is moving, or the shadow map camera moved.
		// We have to ensure that we don't regenerate clouds every frame, since that's what needsUpdate ends up doing.
		// This took up to 10% of the frame time before we applied this fix! That's really bad!
		boolean regenerateClouds = levelRenderer.shouldRegenerateClouds();
		((LevelRenderer) levelRenderer).needsUpdate();
		levelRenderer.setShouldRegenerateClouds(regenerateClouds);

		// Execute the vanilla terrain setup / culling routines using our shadow frustum.
		levelRenderer.invokeSetupRender((Camera) playerCamera, getTerrainFrustumHolder().getWrapper(), false, false);

		// Don't forget to increment the frame counter! This variable is arbitrary and only used in terrain setup,
		// and if it's not incremented, the vanilla culling code will get confused and think that it's already seen
		// chunks during traversal, and break rendering in concerning ways.
		//worldRenderer.setFrameId(worldRenderer.getFrameId() + 1);

		client.smartCull = wasChunkCullingEnabled;

		levelRenderer.getLevel().getProfiler().popPush("terrain");


		// Set up our orthographic projection matrix and load it into RenderSystem
		Matrix4f shadowProjection;
		if (this.fov != null) {
			// If FOV is not null, the pack wants a perspective based projection matrix. (This is to support legacy packs)
			shadowProjection = ShadowMatrices.createPerspectiveMatrix(this.fov);
		} else {
			shadowProjection = ShadowMatrices.createOrthoMatrix(halfPlaneLength, nearPlane < 0 ? -DHCompat.getRenderDistance() : nearPlane, farPlane < 0 ? DHCompat.getRenderDistance() : farPlane);
		}

		IrisRenderSystem.setShadowProjection(shadowProjection);

		PROJECTION = shadowProjection;

		// Disable backface culling
		// This partially works around an issue where if the front face of a mountain isn't visible, it casts no
		// shadow.
		//
		// However, it only partially resolves issues of light leaking into caves.
		//
		// TODO: Better way of preventing light from leaking into places where it shouldn't
		RENDER_SYSTEM.disableCullFace();

		// Render all opaque terrain unless pack requests not to
		if (shouldRenderTerrain) {
            //? if <1.20.6 {
			levelRenderer.invokeRenderChunkLayer(RenderType.solid(), modelView, cameraX, cameraY, cameraZ, shadowProjection);
			levelRenderer.invokeRenderChunkLayer(RenderType.cutout(), modelView, cameraX, cameraY, cameraZ, shadowProjection);
			levelRenderer.invokeRenderChunkLayer(RenderType.cutoutMipped(), modelView, cameraX, cameraY, cameraZ, shadowProjection);
            //?} else {
            /*levelRenderer.invokeRenderChunkLayer(RenderType.solid(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderChunkLayer(RenderType.cutout(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderChunkLayer(RenderType.cutoutMipped(), cameraX, cameraY, cameraZ,MODELVIEW, shadowProjection);
            *///?}
		}

		// Reset our viewport in case Sodium overrode it
		RENDER_SYSTEM.glViewport(0, 0, resolution, resolution);

		levelRenderer.getLevel().getProfiler().popPush("entities");

		// Get the current tick delta. Normally this is the same as client.getTickDelta(), but when the game is paused,
		// it is set to a fixed value.
		final float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();

		// Create a constrained shadow frustum for entities to avoid rendering faraway entities in the shadow pass,
		// if the shader pack has requested it. Otherwise, use the same frustum as for terrain.
		boolean hasEntityFrustum = false;

		if (entityShadowDistanceMultiplier == 1.0F || entityShadowDistanceMultiplier < 0.0F) {
			entityFrustumHolder.setInfo(terrainFrustumHolder.getFrustum(), terrainFrustumHolder.getDistanceInfo(), terrainFrustumHolder.getCullingInfo());
		} else {
			hasEntityFrustum = true;
			entityFrustumHolder = createShadowFrustum(renderDistanceMultiplier * entityShadowDistanceMultiplier, entityFrustumHolder);
		}

		Frustum entityShadowFrustum = getEntityFrustumHolder().getWrapper();
		entityShadowFrustum.prepare(cameraX, cameraY, cameraZ);

        var celeritasRenderer = ((WorldRendererExtended)levelRenderer).sodium$getWorldRenderer();

        celeritasRenderer.setCurrentViewport(((ViewportProvider)entityShadowFrustum).sodium$createViewport());

		// Render nearby entities
		//
		// Note: We must use a separate BuilderBufferStorage object here, or else very weird things will happen during
		// rendering.
		if (renderBuffersExt != null) {
			renderBuffersExt.beginLevelRendering();
		}

		if (buffers instanceof DrawCallTrackingRenderBuffers) {
			((DrawCallTrackingRenderBuffers) buffers).resetDrawCounts();
		}

		MultiBufferSource.BufferSource bufferSource = buffers.bufferSource();
		EntityRenderDispatcher dispatcher = levelRenderer.getEntityRenderDispatcher();

		if (shouldRenderEntities) {
			renderedShadowEntities = renderEntities(levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ);
		} else if (shouldRenderPlayer) {
			renderedShadowEntities = renderPlayerEntity(levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ);
		}

		levelRenderer.getLevel().getProfiler().popPush("build blockentities");

        Predicate<BlockEntity> blockEntityFilter;
		if (shouldRenderLightBlockEntities && !shouldRenderBlockEntities) {
            blockEntityFilter = be -> WorldUtil.getLightEmission(be.getBlockState(), be.getLevel(), be.getBlockPos()) > 0;
        } else {
            blockEntityFilter = null;
        }

        if (shouldRenderLightBlockEntities || shouldRenderBlockEntities) {
            renderedShadowBlockEntities = celeritasRenderer.renderBlockEntities(modelView, buffers, Long2ObjectMaps.emptyMap(), (Camera) playerCamera, tickDelta, blockEntityFilter);
        }

		levelRenderer.getLevel().getProfiler().popPush("draw entities");

		// NB: Don't try to draw the translucent parts of entities afterwards in the shadow pass. It'll cause problems since some
		// shader packs assume that everything drawn afterwards is actually translucent and should cast a colored
		// shadow...
		if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource)
			fullyBufferedMultiBufferSource.readyUp();

		bufferSource.endBatch();

		copyPreTranslucentDepth(levelRenderer);

		levelRenderer.getLevel().getProfiler().popPush("translucent terrain");

		// TODO: Prevent these calls from scheduling translucent sorting...
		// It doesn't matter a ton, since this just means that they won't be sorted in the normal rendering pass.
		// Just something to watch out for, however...
		if (shouldRenderTranslucent) {
            //? if <1.20.6 {
			levelRenderer.invokeRenderChunkLayer(RenderType.translucent(), modelView, cameraX, cameraY, cameraZ, shadowProjection);
            //?} else
            /*levelRenderer.invokeRenderChunkLayer(RenderType.translucent(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);*/
		}

		// Note: Apparently tripwire isn't rendered in the shadow pass.
		// levelRenderer.invokeRenderChunkLayer(RenderType.tripwire(), modelView, cameraX, cameraY, cameraZ, shadowProjection);

		if (renderBuffersExt != null) {
			renderBuffersExt.endLevelRendering();
		}

		IrisRenderSystem.restorePlayerProjection();

        //? if >=1.20.2 {
		/*debugStringTerrain = ((LevelRenderer) levelRenderer).getSectionStatistics();
        *///?} else
        debugStringTerrain = ((LevelRenderer) levelRenderer).getChunkStatistics();

		levelRenderer.getLevel().getProfiler().popPush("generate mipmaps");

		generateMipmaps();

		levelRenderer.getLevel().getProfiler().popPush("restore gl state");

		// Restore backface culling
		RENDER_SYSTEM.enableCullFace();

		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

		// Restore the old viewport
		RENDER_SYSTEM.glViewport(0, 0, client.getMainRenderTarget().width, client.getMainRenderTarget().height);

        pipeline.removePhaseIfNeeded();
        GLDebug.pushGroup(901, "shadowcomp");
		compositeRenderer.renderAll();
        GLDebug.popGroup();

		levelRenderer.setRenderBuffers(playerBuffers);

		visibleBlockEntities = null;
		ACTIVE = false;

		levelRenderer.getLevel().getProfiler().pop();
		levelRenderer.getLevel().getProfiler().popPush("updatechunks");
	}

	public int renderBlockEntities(MultiBufferSource.BufferSource bufferSource, PoseStack modelView, Camera camera, double cameraX, double cameraY, double cameraZ, float tickDelta, boolean hasEntityFrustum, boolean lightsOnly) {
		getLevel().getProfiler().push("build blockentities");

		int shadowBlockEntities = 0;
		BoxCuller culler = null;
		if (hasEntityFrustum) {
			culler = new BoxCuller(halfPlaneLength * (renderDistanceMultiplier * entityShadowDistanceMultiplier));
			culler.setPosition(cameraX, cameraY, cameraZ);
		}

		for (BlockEntity entity : visibleBlockEntities) {
			if (lightsOnly && entity.getBlockState().getLightEmission() == 0) {
				continue;
			}

			BlockPos pos = entity.getBlockPos();
			if (hasEntityFrustum) {
				if (culler.isCulled(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
					continue;
				}
			}
			modelView.pushPose();
			modelView.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
			Minecraft.getInstance().getBlockEntityRenderDispatcher().render(entity, tickDelta, modelView, bufferSource);
			modelView.popPose();

			shadowBlockEntities++;
		}

		getLevel().getProfiler().pop();

		return shadowBlockEntities;
	}

	private int renderEntities(LevelRendererAccessor levelRenderer, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource, PoseStack modelView, float tickDelta, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		levelRenderer.getLevel().getProfiler().push("cull");

		List<Entity> renderedEntities = new ArrayList<>(32);

		// TODO: I'm sure that this can be improved / optimized.
		for (Entity entity : getLevel().entitiesForRendering()) {
			if (!dispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) || entity.isSpectator()) {
				continue;
			}

			renderedEntities.add(entity);
		}

		levelRenderer.getLevel().getProfiler().popPush("sort");

		// Sort the entities by type first in order to allow vanilla's entity batching system to work better.
		renderedEntities.sort(Comparator.comparingInt(entity -> entity.getType().hashCode()));

		levelRenderer.getLevel().getProfiler().popPush("build entity geometry");

		for (Entity entity : renderedEntities) {
			float realTickDelta = CapturedRenderingState.INSTANCE.getRealTickDelta();
			levelRenderer.invokeRenderEntity(entity, cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
		}

		levelRenderer.getLevel().getProfiler().pop();

		return renderedEntities.size();
	}

	private int renderPlayerEntity(LevelRendererAccessor levelRenderer, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource, PoseStack modelView, float tickDelta, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		levelRenderer.getLevel().getProfiler().push("cull");

		Entity player = Minecraft.getInstance().player;

		int shadowEntities = 0;

		if (!dispatcher.shouldRender(player, frustum, cameraX, cameraY, cameraZ) || player.isSpectator()) {
			levelRenderer.getLevel().getProfiler().pop();
			return 0;
		}

		levelRenderer.getLevel().getProfiler().popPush("build geometry");

		if (!player.getPassengers().isEmpty()) {
			for (int i = 0; i < player.getPassengers().size(); i++) {
				float realTickDelta = CapturedRenderingState.INSTANCE.getRealTickDelta();
				levelRenderer.invokeRenderEntity(player.getPassengers().get(i), cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
				shadowEntities++;
			}
		}

		if (player.getVehicle() != null) {
			float realTickDelta = CapturedRenderingState.INSTANCE.getRealTickDelta();
			levelRenderer.invokeRenderEntity(player.getVehicle(), cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
			shadowEntities++;
		}

		float realTickDelta = CapturedRenderingState.INSTANCE.getRealTickDelta();
		levelRenderer.invokeRenderEntity(player, cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);

		shadowEntities++;

		levelRenderer.getLevel().getProfiler().pop();

		return shadowEntities;
	}

	private void copyPreTranslucentDepth(LevelRendererAccessor levelRenderer) {
		levelRenderer.getLevel().getProfiler().popPush("translucent depth copy");

		targets.copyPreTranslucentDepth();
	}


    @Override
	protected String getEntitiesDebugString() {
		return (shouldRenderEntities || shouldRenderPlayer) ? (renderedShadowEntities + "/" + Minecraft.getInstance().level.getEntityCount()) : "disabled by pack";
	}

    @Override
    protected String getBlockEntitiesDebugString() {
		return (shouldRenderBlockEntities || shouldRenderLightBlockEntities) ? renderedShadowBlockEntities + "" : "disabled by pack"; // TODO: + "/" + MinecraftClient.getInstance().world.blockEntities.size();
	}

    @Override
    protected void addBuffersDebugText(List<String> messages) {
        if (buffers instanceof DrawCallTrackingRenderBuffers drawCallTracker && (shouldRenderEntities || shouldRenderPlayer)) {
            messages.add("[" + IrisConstants.MODNAME + "] Shadow Entity Batching: " + BatchingDebugMessageHelper.getDebugMessage(drawCallTracker));
        }
    }

	public void destroy() {
		targets.destroy();
		((MemoryTrackingRenderBuffers) buffers).freeAndDeleteBuffers();
	}

}
