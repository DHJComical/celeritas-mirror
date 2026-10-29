package net.irisshaders.iris.compat.flywheel.indirect;

import net.irisshaders.iris.compat.flywheel.ClrwlBackend;
import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlIndirectPrograms;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlPrograms;
import net.irisshaders.iris.compat.flywheel.engine.*;
import net.irisshaders.iris.compat.flywheel.engine.embed.EnvironmentStorage;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlUniforms;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.engine.CommonCrumbling;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.TextureBinder;
import dev.engine_room.flywheel.backend.engine.indirect.IndirectBuffers;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL40.glDrawElementsIndirect;
import static org.lwjgl.opengl.GL42.GL_COMMAND_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class ClrwlIndirectDrawManager extends ClrwlDrawManager<ClrwlIndirectInstancer<?>>
{
	public record PipelineData(ClrwlIndirectPrograms.PipelinePrograms programs,
							   ClrwlFramebuffers framebuffers,
							   ClrwlDepthPyramid depthPyramid,
							   ClrwlDepthPyramid shadowDepthPyramid,
							   Map<ClrwlIndirectCullingGroup<?>, ClrwlIndirectBuffers.PipelineBuffers> buffers)
	{
		public ClrwlIndirectBuffers.PipelineBuffers getBuffers(ClrwlIndirectCullingGroup<?> group)
		{
			return buffers.computeIfAbsent(group, ClrwlIndirectCullingGroup::makePipelineBuffers);
		}

		public ClrwlDepthPyramid depthPyramid(boolean isShadow)
		{
			return isShadow ? shadowDepthPyramid : depthPyramid;
		}

		public void delete()
		{
			programs.delete();
			framebuffers.delete();
			depthPyramid.delete();
			shadowDepthPyramid.delete();

			for (var buffer : buffers.values())
			{
				buffer.delete();
			}

			buffers.clear();
		}
	}

	private final ClrwlIndirectPrograms programs;
	private final ClrwlMeshPool meshPool;
	private final GlVertexArray vao;
	private final Map<InstanceType<?>, ClrwlIndirectCullingGroup<?>> cullingGroups = new HashMap<>();
	private final GlBuffer crumblingDrawBuffer = new GlBuffer(GlBufferUsage.STREAM_DRAW);
	private final StagingBuffer stagingBuffer;
	private final ClrwlLightBuffers lightBuffers;
	private final ClrwlMatrixBuffer matrixBuffer;

	private final Map<IrisRenderingPipeline, PipelineData> pipelineData = new HashMap<>();
	private final List<ClrwlIndirectBuffers.DrawSnapshot> shadowSolidDraws = new ArrayList<>();
	private final List<ClrwlIndirectBuffers.DrawSnapshot> shadowTranslucentDraws = new ArrayList<>();

	private final ShaderPack pack;
	private final ProgramSet programSet;

	private boolean areModelInstanceCountsDirty = false;
	private boolean hasInflightApply = false;

	public ClrwlIndirectDrawManager(ShaderPack pack, ProgramSet programSet, ClrwlIndirectPrograms programs)
	{
		this.pack = pack;
		this.programSet = programSet;

		this.programs = programs;
		this.meshPool = new ClrwlMeshPool();
		this.vao = GlVertexArray.create();
		this.stagingBuffer = new StagingBuffer(programs.getFlwPrograms());
		this.lightBuffers = new ClrwlLightBuffers();
		this.matrixBuffer = new ClrwlMatrixBuffer();

		this.meshPool.bind(vao);
	}

	public static ClrwlIndirectDrawManager build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean isFallback)
	{
		var programs = ClrwlIndirectPrograms.build(sources, pack, programSet, isFallback);

		if (programs == null)
		{
			return null;
		}

		return new ClrwlIndirectDrawManager(pack, programSet, programs);
	}

	@Override
	protected <I extends Instance> ClrwlIndirectInstancer<?> create(ClrwlInstancerKey<I> key)
	{
		return new ClrwlIndirectInstancer<>(key, new ClrwlAbstractInstancer.Recreate<>(key, this));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <I extends Instance> void initialize(ClrwlInstancerKey<I> key, ClrwlIndirectInstancer<?> instancer)
	{
		var group = (ClrwlIndirectCullingGroup<I>) cullingGroups.computeIfAbsent(key.type(), t -> new ClrwlIndirectCullingGroup<>(t, programs, programSet));
		group.add((ClrwlIndirectInstancer<I>) instancer, key, meshPool);
	}

	@Override
	public void prepareFrame(LightStorage lightStorage, EnvironmentStorage environmentStorage)
	{
		super.prepareFrame(lightStorage, environmentStorage);

		if (!shadowSolidDraws.isEmpty())
		{
			for (var snapshot : shadowSolidDraws)
			{
				snapshot.destroy();
			}

			shadowSolidDraws.clear();
		}

		if (!shadowTranslucentDraws.isEmpty())
		{
			for (var snapshot : shadowTranslucentDraws)
			{
				snapshot.destroy();
			}

			shadowTranslucentDraws.clear();
		}

		// Flush instance counts, page mappings, and prune empty groups.
		cullingGroups.values()
				.removeIf(ClrwlIndirectCullingGroup::flushInstancers);

		// Instancers may have been emptied in the above call, now remove them here.
		instancers.values()
				.removeIf(instancer -> instancer.instanceCount() == 0);

		meshPool.flush();

		setPhase(ClrwlRenderingPhase.STAGING_BUFFER_FLUSH, false);

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		stagingBuffer.reclaim();

		// Genuinely nothing to do, we can just early out.
		// Still process the mesh pool and reclaim fenced staging regions though.
		if (cullingGroups.isEmpty())
		{
			return;
		}

		lightBuffers.flush(stagingBuffer, lightStorage);
		matrixBuffer.flush(stagingBuffer, environmentStorage);

		for (var group : cullingGroups.values())
		{
			group.upload(stagingBuffer);
		}

		stagingBuffer.flush();

		areModelInstanceCountsDirty = false;

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		setPhase(ClrwlRenderingPhase.INDIRECT_CULL_TRANSFORM, false);

		// Bounding spheres can be computed here because they are in world space

		ClrwlUniforms.bind(false);

		for (var group : cullingGroups.values())
		{
			group.dispatchTransform();
		}
	}

	public void preparePass(IrisRenderingPipeline pipeline, boolean isShadow)
	{
		if (cullingGroups.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(pipeline);

		for (var group : cullingGroups.values())
		{
			pipelineData.getBuffers(group).updateCounts();
		}

		resetModelCountIfDirty(true);

		if (!useOcclusionCulling(isShadow))
		{
			ClrwlUniforms.bind(isShadow);
			computeFullCull(isShadow, ClrwlIndirectPrograms.Culling.MaterialFilter.ALL, false, pipelineData);
		}
	}

	private void dispatchCull(ClrwlIndirectPrograms.Culling culling, int materialFilter, PipelineData pipelineData)
	{
		setPhase(ClrwlRenderingPhase.INDIRECT_CULL, culling.programGroup() == ClrwlProgramGroup.SHADOW);

		var program = pipelineData.programs.getCullingProgram(culling);

		program.bind();
		program.setUInt("clrwl_materialFilter", materialFilter);

		for (var group : cullingGroups.values())
		{
			if ((group.materialFilter() & materialFilter) == 0)
			{
				continue;
			}

			pipelineData.getBuffers(group).bindForCull(culling.programGroup() == ClrwlProgramGroup.SHADOW);
			group.dispatchCull();
		}

		areModelInstanceCountsDirty = true;
	}

	private void dispatchApply(boolean isShadow)
	{
		setPhase(ClrwlRenderingPhase.INDIRECT_CULL_APPLY, isShadow);

		programs.getApplyProgram()
				.bind();

		for (var group : cullingGroups.values())
		{
			group.dispatchApply();
		}

		hasInflightApply = true;
	}

	private void dispatchModelReset()
	{
		setPhase(ClrwlRenderingPhase.INDIRECT_CULL_RESET, false);

		programs.getZeroModelsProgram()
				.bind();

		for (var group : cullingGroups.values())
		{
			group.dispatchModelReset();
		}
	}

	private void generateDepthPyramid(ClrwlDepthPyramid depthPyramid)
	{
		setPhase(ClrwlRenderingPhase.INDIRECT_DEPTH_PYRAMID, false);
		depthPyramid.generate();
	}

	private void saveShadowDraws(List<ClrwlIndirectBuffers.DrawSnapshot> snapshots)
	{
		snapshots.clear();

		for (var group : cullingGroups.values())
		{
			snapshots.add(group.makeDrawSnapshot());
		}
	}

	private void loadShadowDraws(List<ClrwlIndirectBuffers.DrawSnapshot> snapshots)
	{
		stagingBuffer.reclaim();

		for (var snapshot : snapshots)
		{
			snapshot.applyAndConsume(stagingBuffer);
		}

		stagingBuffer.flush();

		snapshots.clear();

		hasInflightApply = true;
	}

	private void computeEarlyCull(boolean isShadow, PipelineData pipelineData)
	{
		ClrwlIndirectPrograms.Culling cullProgram = isShadow
				? ClrwlIndirectPrograms.Culling.SHADOW_EARLY
				: ClrwlIndirectPrograms.Culling.GBUFFERS_EARLY;

		resetModelCountIfDirty(true);

		dispatchCull(cullProgram, ClrwlIndirectPrograms.Culling.MaterialFilter.SOLID, pipelineData);

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		dispatchApply(isShadow);
	}

	private void computeLateCull(boolean isShadow, PipelineData pipelineData)
	{
		ClrwlIndirectPrograms.Culling cullProgram = isShadow
				? ClrwlIndirectPrograms.Culling.SHADOW_LATE
				: ClrwlIndirectPrograms.Culling.GBUFFERS_LATE;

		var depthPyramid = pipelineData.depthPyramid(isShadow);

		resetModelCountIfDirty(false);
		generateDepthPyramid(depthPyramid);

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		depthPyramid.bindForCull();
		dispatchCull(cullProgram, ClrwlIndirectPrograms.Culling.MaterialFilter.SOLID, pipelineData);

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		dispatchApply(isShadow);
	}

	private void computeFullCull(boolean isShadow, int matFilter, boolean useOcclusion, PipelineData pipelineData)
	{
		ClrwlIndirectPrograms.Culling cullProgram = isShadow
					? ClrwlIndirectPrograms.Culling.SHADOW_FULL
					: ClrwlIndirectPrograms.Culling.GBUFFERS_FULL;

		var depthPyramid = pipelineData.depthPyramid(isShadow);

		resetModelCountIfDirty(false);

		if (useOcclusion)
		{
			generateDepthPyramid(depthPyramid);
		}

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		if (useOcclusion)
		{
			depthPyramid.bindForCull();
		}

		dispatchCull(cullProgram, matFilter, pipelineData);

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		dispatchApply(isShadow);
	}

	private void resetModelCountIfDirty(boolean needsBarrier)
	{
		if (areModelInstanceCountsDirty)
		{
			dispatchModelReset();

			if (needsBarrier)
			{
				glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
			}

			areModelInstanceCountsDirty = false;
		}
	}

	private void drawBarrierIfNeeded()
	{
		if (hasInflightApply)
		{
			glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_COMMAND_BARRIER_BIT);
			hasInflightApply = false;
		}
	}

	public void renderSolid(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
top:	{
			for (var group : cullingGroups.values())
			{
				if (group.hasSolidDraws())
				{
					break top;
				}
			}

			return;
		}

		var pipelineData = getPipelineData(irisPipeline);

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		lightBuffers.bind();
		matrixBuffer.bind();


		try
		{
			if (SHADOW_CULL_DEBUG && !isShadow)
			{
				loadShadowDraws(shadowSolidDraws);
			}
			else if (useTwoPassCulling(isShadow))
			{
				computeEarlyCull(isShadow, pipelineData);
			}
			else if (useOcclusionCulling(isShadow))
			{
				computeFullCull(isShadow, ClrwlIndirectPrograms.Culling.MaterialFilter.SOLID, true, pipelineData);
			}

			dispatchSolidDraws(irisPipeline, isShadow, pipelineData);

			if (SHADOW_CULL_DEBUG && isShadow)
			{
				saveShadowDraws(shadowSolidDraws);
			}

			if (useTwoPassCulling(isShadow) && LATE_CULL_ENABLED && !(SHADOW_CULL_DEBUG && !isShadow))
			{
				computeLateCull(isShadow, pipelineData);
				dispatchSolidDraws(irisPipeline, isShadow, pipelineData);
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Got error while rendering solid instances", e);
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private void dispatchSolidDraws(IrisRenderingPipeline irisPipeline, boolean isShadow, PipelineData pipelineData)
	{
		var pipelinePrograms = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		setPhase(ClrwlRenderingPhase.SOLID, isShadow);

		drawBarrierIfNeeded();

		for (var group : cullingGroups.values())
		{
			group.submitSolid(pipelinePrograms, framebuffers, isShadow);
		}
	}

	public void renderTranslucent(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
top:	{
			for (var group : cullingGroups.values())
			{
				if (group.hasTranslucentDraws() || group.hasOitDraws())
				{
					break top;
				}
			}

			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var pipelinePrograms = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();
		var program = !isShadow
				? ClrwlProgramId.GBUFFERS_TRANSLUCENT
				: ClrwlProgramId.SHADOW_TRANSLUCENT;

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		lightBuffers.bind();
		matrixBuffer.bind();


		try
		{
			if (SHADOW_CULL_DEBUG && !isShadow)
			{
				loadShadowDraws(shadowTranslucentDraws);
			}
			else if (useOcclusionCulling(isShadow))
			{
				computeFullCull(isShadow, ClrwlIndirectPrograms.Culling.MaterialFilter.TRANSLUCENT, true, pipelineData);
			}

			if (SHADOW_CULL_DEBUG && isShadow)
			{
				saveShadowDraws(shadowTranslucentDraws);
			}

			setPhase(ClrwlRenderingPhase.TRANSLUCENT, isShadow);

			drawBarrierIfNeeded();

			boolean hasOit = false;
			for (var group : cullingGroups.values())
			{
				group.submitTranslucent(pipelinePrograms, framebuffers, isShadow);

				if (group.hasOitDraws())
				{
					hasOit = true;
				}
			}

top: 		if (hasOit)
			{
				var clrwlDirectives = programSet.getClrwlPrograms().getClrwlDirectives();
				var isOitEnabled = clrwlDirectives.getOitConfig(program.group()).enabled();

				if (isOitEnabled)
				{
					var maybeSrc = programSet.getClrwlPrograms().getClrwlProgramSource(program);

					if (maybeSrc.isEmpty())
					{
						break top;
					}

					var framebuffer = framebuffers.getFramebuffer(program);
					var directives = maybeSrc.get().getDirectives();
					var oitFramebuffer = framebuffers.getOitFramebuffers(program.group(), programs.getOitPrograms(), programSet, directives);
					var blendOverride = framebuffers.getBlendModeOverride(program).orElse(null);
					var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(program);

					if (framebuffer == null || oitFramebuffer == null)
					{
						break top;
					}

					setPhase(ClrwlRenderingPhase.OIT_DEPTH_RANGE, isShadow);

					oitFramebuffer.prepare();

					oitFramebuffer.prepareDepthRange();
					for (var group : cullingGroups.values())
					{
						group.submitOit(ClrwlPrograms.OitMode.DEPTH_RANGE, pipelinePrograms, framebuffers, isShadow, currentRenderPhase);
					}

					if (oitFramebuffer.prepareRenderTransmittance())
					{
						setPhase(ClrwlRenderingPhase.OIT_COEFFICIENTS, isShadow);

						for (var group : cullingGroups.values())
						{
							group.submitOit(ClrwlPrograms.OitMode.GENERATE_COEFFICIENTS, pipelinePrograms, framebuffers, isShadow, currentRenderPhase);
						}
					}

	//				oitFramebuffer.renderDepthFromTransmittance();
	//
	//				// Need to bind this again because we just drew a full screen quad for OIT.
	//				vao.bindForDraw();

					setPhase(ClrwlRenderingPhase.OIT_ACCUMULATE, isShadow);

					oitFramebuffer.prepareAccumulate();
					for (var group : cullingGroups.values())
					{
						group.submitOit(ClrwlPrograms.OitMode.EVALUATE, pipelinePrograms, framebuffers, isShadow, currentRenderPhase);
					}

					setPhase(ClrwlRenderingPhase.OIT_COMPOSITE, isShadow);

					oitFramebuffer.composite(framebuffer, blendOverride, bufferBlendOverrides, isShadow);
				}
				else
				{
					for (var group : cullingGroups.values())
					{
						group.submitOitAsTranslucent(pipelinePrograms, framebuffers, isShadow);
					}
				}
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Got error while rendering translucent instances", e);
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderCrumbling(IrisRenderingPipeline irisPipeline, List<Engine.CrumblingBlock> crumblingBlocks)
	{
		var byType = doCrumblingSort(crumblingBlocks, ClrwlIndirectInstancer::fromState);

		if (byType.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var programs = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		var framebuffer = framebuffers.getFramebuffer(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);

		if (framebuffer == null)
		{
			return;
		}

		setPhase(ClrwlRenderingPhase.CRUMBLING, false);

		framebuffer.bind();

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(false);
		vao.bindForDraw();

		var blendOverride = framebuffers.getBlendModeOverride(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK).orElse(null);
		var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);


		var crumblingMaterial = SimpleMaterial.builder();

		// Scratch memory for writing draw commands.
		var block = MemoryBlock.malloc(IndirectBuffers.DRAW_COMMAND_STRIDE);

		// Set up the crumbling program buffers. Nothing changes here between draws.
		GlBufferType.DRAW_INDIRECT_BUFFER.bind(crumblingDrawBuffer.handle());
		glBindBufferRange(GL_SHADER_STORAGE_BUFFER, ClrwlBufferBindings.base() + ClrwlBufferBindings.DRAW, crumblingDrawBuffer.handle(), 0, ClrwlIndirectBuffers.DRAW_COMMAND_STRIDE);

		for (var groupEntry : byType.entrySet())
		{
			var byProgress = groupEntry.getValue();

			GroupKey<?> groupKey = groupEntry.getKey();
			ClrwlIndirectCullingGroup<?> cullingGroup = cullingGroups.get(groupKey.instanceType());

			if (cullingGroup == null)
			{
				continue;
			}

			for (var progressEntry : byProgress.int2ObjectEntrySet())
			{
				Samplers.CRUMBLING.makeActive();
				TextureBinder.bind(ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));

				for (var instanceHandlePair : progressEntry.getValue())
				{
					ClrwlIndirectInstancer<?> instancer = instanceHandlePair.getFirst();
					int instanceIndex = instanceHandlePair.getSecond().index;

					for (ClrwlIndirectDraw draw : instancer.draws())
					{
						// Transform the material to be suited for crumbling.
						CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());

						if (cullingGroup.bindForCrumbling(crumblingMaterial, programs, blendOverride))
						{
							ClrwlMaterialRenderState.setup(crumblingMaterial, blendOverride, bufferBlendOverrides);

							// Upload the draw command.
							draw.writeWithOverrides(block.ptr(), instanceIndex, crumblingMaterial);
							crumblingDrawBuffer.upload(block);

							// Submit! Everything is already bound by here.
							glDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0);
						}
					}
				}
			}
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();

		block.free();
	}

	private boolean useOcclusionCulling(boolean isShadow)
	{
		var directives = programSet.getClrwlPrograms().getClrwlDirectives();
		var programGroup = ClrwlProgramGroup.fromShadow(isShadow);

		return directives.getOcclusionCulling(programGroup);
	}

	private boolean useTwoPassCulling(boolean isShadow)
	{
		if (!TWO_PASS_CULL_ENABLED)
		{
			return false;
		}

		var directives = programSet.getClrwlPrograms().getClrwlDirectives();
		var programGroup = ClrwlProgramGroup.fromShadow(isShadow);

		return directives.getOcclusionCulling(programGroup) && directives.getFrustumCulling(programGroup);
	}

	private PipelineData getPipelineData(IrisRenderingPipeline pipeline)
	{
		deleteStalePipelinesData(pipeline);
		return pipelineData.computeIfAbsent(pipeline, this::createPipelineData);
	}

	private PipelineData createPipelineData(IrisRenderingPipeline irisPipeline)
	{
		ClrwlIndirectPrograms.PipelinePrograms pipelinePrograms = programs.createPipelinePrograms(irisPipeline);
		ClrwlFramebuffers framebuffers = new ClrwlFramebuffers(irisPipeline, programSet);
		ClrwlDepthPyramid depthPyramid = new ClrwlDepthPyramid(ClrwlProgramGroup.GBUFFERS, programs, irisPipeline);
		ClrwlDepthPyramid shadowDepthPyramid = new ClrwlDepthPyramid(ClrwlProgramGroup.SHADOW, programs, irisPipeline);

		Colorwheel.LOGGER.info("Created pipeline data for {}", irisPipeline);

		return new PipelineData(pipelinePrograms, framebuffers, depthPyramid, shadowDepthPyramid, new HashMap<>());
	}

	// An engine only renders its own dimension, so a different pipeline means the old one was destroyed by a reload.
	private void deleteStalePipelinesData(IrisRenderingPipeline current)
	{
		if (pipelineData.size() == 1 && pipelineData.containsKey(current))
		{
			return;
		}

		var it = pipelineData.entrySet().iterator();

		while (it.hasNext())
		{
			var entry = it.next();

			if (entry.getKey() != current)
			{
				entry.getValue().delete();
				it.remove();
				Colorwheel.LOGGER.info("Deleted pipeline data for {}", entry.getKey());
			}
		}
	}

	private void deletePipelinesData()
	{
		for (var entry : pipelineData.entrySet())
		{
			entry.getValue().delete();
			Colorwheel.LOGGER.info("Deleted pipeline data for {}", entry.getKey());
		}

		pipelineData.clear();
	}

	@Override
	public void delete()
	{
		super.delete();

		cullingGroups.values()
				.forEach(ClrwlIndirectCullingGroup::delete);
		cullingGroups.clear();

		stagingBuffer.delete();

		meshPool.delete();
		vao.delete();

		crumblingDrawBuffer.delete();

		lightBuffers.delete();
		matrixBuffer.delete();

		deletePipelinesData();
		programs.delete();
	}

	@Override
	public void triggerFallback()
	{
		ClrwlBackend.disableForCurrentPack();
		Minecraft.getInstance().levelRenderer.allChanged();
	}

	private static boolean TWO_PASS_CULL_ENABLED = true;
	private static boolean LATE_CULL_ENABLED = true;
	private static boolean SHADOW_CULL_DEBUG = false;

	public static void toggleTwoPassCull(boolean enabled)
	{
		TWO_PASS_CULL_ENABLED = enabled;
	}

	public static void toggleLateCull(boolean enabled)
	{
		LATE_CULL_ENABLED = enabled;
	}

	public static void toggleShadowCullDebug(boolean enabled)
	{
		SHADOW_CULL_DEBUG = enabled;
	}
}
