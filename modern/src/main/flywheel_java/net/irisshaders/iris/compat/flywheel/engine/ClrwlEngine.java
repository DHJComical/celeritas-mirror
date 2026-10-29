package net.irisshaders.iris.compat.flywheel.engine;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.ClrwlBackend;
import net.irisshaders.iris.compat.flywheel.engine.embed.EmbeddedEnvironment;
import net.irisshaders.iris.compat.flywheel.engine.embed.EnvironmentStorage;
import net.irisshaders.iris.compat.flywheel.indirect.ClrwlBufferBindings;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlGbuffersPassUniforms;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlOptionsUniforms;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlShadowPassUniforms;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlEngine implements Engine
{
	public interface DrawManagerFactory
	{
		ClrwlDrawManager<?> build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean isFallback);
	}

	private final ClrwlDrawManager<?> drawManager;
	private final int sqrMaxOriginDistance;
	private final EnvironmentStorage environmentStorage;
	private final LightStorage lightStorage;
	private BlockPos renderOrigin = BlockPos.ZERO;

	private final LevelAccessor level;
	private final NamespacedId dimension;
	private final ShaderPack pack;

	private final ShadowCulling shadowCulling;

	public ClrwlEngine(LevelAccessor level, int maxOriginDistance, DrawManagerFactory drawManagerFactory)
	{
		ClientLevel clientLevel = (ClientLevel) level;
		this.level = level;
		this.dimension = new NamespacedId(clientLevel.dimension().location().getNamespace(),
										  clientLevel.dimension().location().getPath());

		this.pack = Iris.getCurrentPack().orElseThrow();

		var programSet = pack.getProgramSet(dimension);
		ClrwlBufferBindings.setBase(ClrwlBackend.getShaderStorageBase(pack));
		var isFallback = (programSet.getClrwlPrograms().isFallbackMode());

		this.drawManager = drawManagerFactory.build(FlwPrograms.SOURCES, pack, programSet, isFallback);
		this.sqrMaxOriginDistance = maxOriginDistance * maxOriginDistance;
		this.environmentStorage = new EnvironmentStorage();
		this.lightStorage = new LightStorage(level);

		this.shadowCulling = new ShadowCulling(programSet);
	}

	@Override
	public VisualizationContext createVisualizationContext()
	{
		return new ClrwlMainVisualizationContext();
	}

	@Override
	public Plan<RenderContext> createFramePlan()
	{
		// render() runs up to three times per frame (shadow, solid, translucent), but must only prepare the frame
		// once. Flywheel raises its frame flag after this plan and waits for it before render(), which orders this
		// write before the read there.
		return drawManager.createFramePlan()
				.and(lightStorage.createFramePlan())
				.then(SimplePlan.of(() -> shouldPrepareFrame = true));
	}

	@Override
	public Vec3i renderOrigin()
	{
		return renderOrigin;
	}

	@Override
	public boolean updateRenderOrigin(Camera camera)
	{
		Vec3 cameraPos = camera.getPosition();
		double dx = renderOrigin.getX() - cameraPos.x;
		double dy = renderOrigin.getY() - cameraPos.y;
		double dz = renderOrigin.getZ() - cameraPos.z;
		double distanceSqr = dx * dx + dy * dy + dz * dz;

		if (distanceSqr <= sqrMaxOriginDistance) {
			return false;
		}

		renderOrigin = BlockPos.containing(cameraPos);
		drawManager.onRenderOriginChanged();
		return true;
	}

	@Override
	public void lightSections(LongSet longSet)
	{
		lightStorage.sections(longSet);
	}

	@Override
	public void onLightUpdate(SectionPos sectionPos, LightLayer lightLayer)
	{
		lightStorage.onLightUpdate(sectionPos.asLong());
	}

	private boolean shouldPrepareFrame = false;

	private void prepareFrame(RenderContext context)
	{
		if (shouldPrepareFrame)
		{
			shouldPrepareFrame = false;

			environmentStorage.flush();
			shadowCulling.refresh();
			ClrwlUniforms.updateFrame(context);
			ClrwlOptionsUniforms.update(Minecraft.getInstance().options);

			drawManager.prepareFrame(lightStorage, environmentStorage);
		}
	}

	private void preparePass(RenderContext context, IrisRenderingPipeline irisPipeline)
	{
		if (context instanceof ShadowRenderContext shadowContext)
		{
			drawManager.preparePass(irisPipeline, true);
			ClrwlShadowPassUniforms.update(shadowContext, pack, dimension, shadowCulling);
		}
		else
		{
			drawManager.preparePass(irisPipeline, false);
			ClrwlGbuffersPassUniforms.update(context, pack, dimension);
		}
	}

	@Override
	public void render(RenderContext context)
	{
		try (var state = GlStateTracker.getRestoreState())
		{
			prepareFrame(context);

			var curPipeline = Iris.getPipelineManager().getPipelineNullable();

			if (curPipeline instanceof IrisRenderingPipeline irisPipeline)
			{
				if (context instanceof ShadowRenderContext shadowContext)
				{
					if (shadowCulling.shouldRenderShadow())
					{
						if (shadowContext.phase() == ShadowRenderingPhase.SOLID)
						{
							preparePass(context, irisPipeline);
							drawManager.renderSolid(irisPipeline, true);
						}
						else
						{
							drawManager.renderTranslucent(irisPipeline, true);
						}
					}
				}
				else if (context instanceof TranslucentRenderContext)
				{
					drawManager.renderTranslucent(irisPipeline, false);
				}
				else
				{
					preparePass(context, irisPipeline);
					drawManager.renderSolid(irisPipeline, false);
				}
			}
			else
			{
				handleInvalidPipeline(curPipeline);
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Falling back", e);
			drawManager.triggerFallback();
		}
	}

	@Override
	public void renderCrumbling(RenderContext renderContext, List<CrumblingBlock> crumblingBlocks)
	{
		try (var state = GlStateTracker.getRestoreState())
		{
			var curPipeline = Iris.getPipelineManager().getPipelineNullable();

			if (curPipeline instanceof IrisRenderingPipeline irisPipeline)
			{
				drawManager.renderCrumbling(irisPipeline, crumblingBlocks);
			}
			else
			{
				handleInvalidPipeline(curPipeline);
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Falling back", e);
			drawManager.triggerFallback();
		}
	}

	protected boolean alreadyGotInvalidPipeline = false;

	protected void handleInvalidPipeline(WorldRenderingPipeline worldPipeline)
	{
		if (alreadyGotInvalidPipeline)
		{
			return;
		}

		Colorwheel.LOGGER.warn("Got unexpected rendering pipeline, rendering issues may occur.");
		Colorwheel.LOGGER.warn("Got pipeline: {}", worldPipeline);

		alreadyGotInvalidPipeline = true;
	}

	@Override
	public void delete()
	{
		drawManager.delete();
		lightStorage.delete();
		environmentStorage.delete();
	}

	public EnvironmentStorage environmentStorage() {
		return environmentStorage;
	}

	public LightStorage lightStorage() {
		return lightStorage;
	}

	public LevelAccessor level() { return level; }

    public <I extends Instance>Instancer<I> instancer(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
	{
		return drawManager.getInstancer(visual, environment, type, model, bias);
	}

	public class ClrwlMainVisualizationContext implements VisualizationContext
	{
		private final ClrwlInstancerProvider instancerProvider;
		private final Map<ClrwlBlockEntityVisualizationContext.Key, ClrwlBlockEntityVisualizationContext> blockEntityCtxs = new HashMap<>();
		private final Map<Integer, ClrwlEntityVisualizationContext> entityCtxs = new HashMap<>();

		public ClrwlMainVisualizationContext()
		{
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.undefined());
		}

		public VisualizationContext getBlockEntityVisualCtx(int irisId, int lightEmission)
		{
			return blockEntityCtxs.computeIfAbsent(new ClrwlBlockEntityVisualizationContext.Key(irisId, lightEmission), ClrwlBlockEntityVisualizationContext::new);
		}

		public VisualizationContext getEntityVisualCtx(int irisId)
		{
			return entityCtxs.computeIfAbsent(irisId, ClrwlEntityVisualizationContext::new);
		}

		@Override
		public InstancerProvider instancerProvider() {
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.undefined(), renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}

	private class ClrwlBlockEntityVisualizationContext implements VisualizationContext
	{
		record Key(int irisId, int lightEmission)
		{
		}

		private final int irisId;
		private final int lightEmission;
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlBlockEntityVisualizationContext(Key key)
		{
			this.irisId = key.irisId;
			this.lightEmission = key.lightEmission;
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId, lightEmission));
		}

		@Override
		public InstancerProvider instancerProvider()
		{
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId, lightEmission), renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}

	private class ClrwlEntityVisualizationContext implements VisualizationContext
	{
		private final int irisId;
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlEntityVisualizationContext(int irisId)
		{
			this.irisId = irisId;
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.entity(irisId));
		}

		@Override
		public InstancerProvider instancerProvider()
		{
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.entity(irisId), renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}
}
