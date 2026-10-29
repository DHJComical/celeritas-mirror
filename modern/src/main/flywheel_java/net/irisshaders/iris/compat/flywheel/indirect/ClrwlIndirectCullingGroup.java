package net.irisshaders.iris.compat.flywheel.indirect;

import net.irisshaders.iris.compat.flywheel.compile.ClrwlIndirectPrograms;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlProgram;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlPrograms;
import net.irisshaders.iris.compat.flywheel.compile.ClrwlShaderKey;
import net.irisshaders.iris.compat.flywheel.engine.*;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramId;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.engine.MaterialRenderState;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.math.MoreMath;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

public class ClrwlIndirectCullingGroup<I extends Instance>
{
	private static final Comparator<ClrwlIndirectDraw> DRAW_COMPARATOR = Comparator.comparing(ClrwlIndirectDraw::isEmbedded)
			.thenComparingInt(ClrwlIndirectDraw::bias)
			.thenComparingInt(ClrwlIndirectDraw::indexOfMeshInModel)
			.thenComparing(ClrwlIndirectDraw::material, MaterialRenderState.COMPARATOR);

	private final InstanceType<I> instanceType;
	private final long instanceStride;
	private final ClrwlIndirectBuffers buffers;
	private final List<ClrwlIndirectInstancer<I>> instancers = new ArrayList<>();
	private final List<ClrwlIndirectDraw> indirectDraws = new ArrayList<>();
	private final List<MultiDraw> solidDraws = new ArrayList<>();
	private final List<MultiDraw> translucentDraws = new ArrayList<>();
	private final List<MultiDraw> oitDraws = new ArrayList<>();

	private final ProgramSet programSet;
	private final GlProgram transformSphereProgram;

	private boolean needsDrawSort;
	private int instanceCountThisFrame;

	ClrwlIndirectCullingGroup(InstanceType<I> instanceType, ClrwlIndirectPrograms programs, ProgramSet programSet)
	{
		this.instanceType = instanceType;
		instanceStride = MoreMath.align4(instanceType.layout().byteSize());
		// Compiled first: if it throws, the group is never registered, so buffers allocated before it would leak.
		transformSphereProgram = programs.getTransformProgram(instanceType);
		buffers = new ClrwlIndirectBuffers(instanceStride);

		this.programSet = programSet;
	}

	public boolean flushInstancers()
	{
		instanceCountThisFrame = 0;
		int modelIndex = 0;
        for (var iterator = instancers.iterator(); iterator.hasNext();)
		{
            var instancer = iterator.next();
			var instanceCount = instancer.instanceCount();

			if (instanceCount == 0)
			{
				iterator.remove();
				instancer.delete();
				continue;
			}

			instancer.update(modelIndex, instanceCountThisFrame);
			instanceCountThisFrame += instanceCount;

			modelIndex++;
        }

        if (indirectDraws.removeIf(ClrwlIndirectDraw::deleted))
		{
			needsDrawSort = true;
		}

		var out = indirectDraws.isEmpty();

		if (out)
		{
			delete();
		}

		return out;
	}

	public void upload(StagingBuffer stagingBuffer)
	{
		buffers.updateCounts(instanceCountThisFrame, instancers.size(), indirectDraws.size());

		// Upload only instances that have changed.
		uploadInstances(stagingBuffer);

		buffers.objectStorage.uploadDescriptors(stagingBuffer);

		// We need to upload the models every frame to reset the instance count.
		uploadModels(stagingBuffer);

		if (needsDrawSort)
		{
			sortDraws();
			needsDrawSort = false;
		}

		uploadDraws(stagingBuffer);
	}

	public void dispatchTransform()
	{
		transformSphereProgram.bind();

		buffers.bindForTransform();
		glDispatchCompute(buffers.objectStorage.capacity(), 1, 1);
	}

	public void dispatchCull()
	{
		buffers.bindForCull();
		glDispatchCompute(buffers.objectStorage.capacity(), 1, 1);
	}

	public void dispatchApply()
	{
		buffers.bindForApply();
		glDispatchCompute(GlCompat.getComputeGroupCount(indirectDraws.size()), 1, 1);
	}

	public void dispatchModelReset()
	{
		buffers.bindForModelReset();
		glDispatchCompute(GlCompat.getComputeGroupCount(indirectDraws.size()), 1, 1);
	}

	public boolean hasSolidDraws()
	{
		return !solidDraws.isEmpty();
	}

	public boolean hasTranslucentDraws()
	{
		return !translucentDraws.isEmpty();
	}

	public boolean hasOitDraws()
	{
		return !oitDraws.isEmpty();
	}

	public int materialFilter()
	{
		int res = 0;

		if (!solidDraws.isEmpty())
		{
			res |= ClrwlIndirectPrograms.Culling.MaterialFilter.SOLID;
		}

		if (!translucentDraws.isEmpty() || !oitDraws.isEmpty())
		{
			res |= ClrwlIndirectPrograms.Culling.MaterialFilter.TRANSLUCENT;
		}

		return res;
	}

	private void sortDraws()
	{
		solidDraws.clear();
		translucentDraws.clear();
		oitDraws.clear();
		// sort by visual type, then material
		indirectDraws.sort(DRAW_COMPARATOR);

		for (int start = 0, i = 0; i < indirectDraws.size(); i++)
		{
			var draw1 = indirectDraws.get(i);

			// if the next draw call has a different VisualType or Material, start a new MultiDraw
			if (i == indirectDraws.size() - 1 || incompatibleDraws(draw1, indirectDraws.get(i + 1)))
			{
				List<ClrwlIndirectCullingGroup.MultiDraw> dst;
				switch (draw1.material().transparency())
				{
					case TRANSLUCENT -> dst = translucentDraws;
					case ORDER_INDEPENDENT -> dst = oitDraws;
					default -> dst = solidDraws;
				}

				dst.add(new MultiDraw(draw1.material(), draw1.isEmbedded(), start, i + 1));
				start = i + 1;
			}
		}
	}

	private boolean incompatibleDraws(ClrwlIndirectDraw draw1, ClrwlIndirectDraw draw2)
	{
		if (draw1.isEmbedded() != draw2.isEmbedded())
		{
			return true;
		}

		return !MaterialRenderState.materialEquals(draw1.material(), draw2.material());
	}

	public void add(ClrwlIndirectInstancer<I> instancer, ClrwlInstancerKey<I> key, ClrwlMeshPool meshPool)
	{
		instancer.mapping = buffers.objectStorage.createMapping();
		instancer.update(instancers.size(), -1);

		instancers.add(instancer);

        List<Model.ConfiguredMesh> meshes = key.model()
				.meshes();
        for (int i = 0; i < meshes.size(); i++)
		{
            var entry = meshes.get(i);

            ClrwlMeshPool.PooledMesh mesh = meshPool.alloc(entry.mesh());
			var draw = new ClrwlIndirectDraw(instancer, entry.material(), mesh, key.bias(), i);
            indirectDraws.add(draw);
            instancer.addDraw(draw);
        }

		needsDrawSort = true;
	}

	public void submitSolid(ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlFramebuffers framebuffers, boolean isShadow)
	{
		if (solidDraws.isEmpty())
		{
			return;
		}

		buffers.bindForDraw();

		ClrwlProgram prevProgram = null;
		GlFramebuffer prevFramebuffer = null;

		for (var multiDraw : solidDraws)
		{
			var key = ClrwlShaderKey.fromMaterial(instanceType,
												  multiDraw.material,
												  multiDraw.embedded ? ContextShader.EMBEDDED : ContextShader.DEFAULT,
												  isShadow,
												  ClrwlPrograms.OitMode.OFF);

			var program = programs.get(key);

			if (program == null)
			{
				continue;
			}

			var programId = ClrwlProgramId.fromTransparency(multiDraw.material.transparency(), isShadow);
			var framebuffer = framebuffers.getFramebuffer(programId);

			if (framebuffer == null)
			{
				continue;
			}

			if (prevProgram != program)
			{
				if (prevProgram != null)
				{
					ClrwlProgram.unbind();
				}

				program.bind();
			}

			var blendOverride = framebuffers.getBlendModeOverride(programId).orElse(null);
			var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(programId);

			program.setClrwlCommonUniforms(multiDraw.material, blendOverride, ClrwlRenderingPhase.SOLID);
			ClrwlMaterialRenderState.setup(multiDraw.material, blendOverride, bufferBlendOverrides);

			if (prevFramebuffer != framebuffer)
			{
				prevFramebuffer = framebuffer;
				framebuffer.bind();
			}

			multiDraw.submit(program);

			prevProgram = program;
		}
	}

	private void drawTranslucent(List<MultiDraw> draws, ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlFramebuffers framebuffers, boolean isShadow)
	{
		if (draws.isEmpty())
		{
			return;
		}

		var programId = !isShadow
				? ClrwlProgramId.GBUFFERS_TRANSLUCENT
				: ClrwlProgramId.SHADOW_TRANSLUCENT;

		var framebuffer = framebuffers.getFramebuffer(programId);
		var blendOverride = framebuffers.getBlendModeOverride(programId).orElse(null);
		var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(programId);

		if (framebuffer == null)
		{
			return;
		}

		buffers.bindForDraw();

		ClrwlProgram prevProgram = null;
		GlFramebuffer prevFramebuffer = null;

		for (var multiDraw : draws)
		{
			var key = ClrwlShaderKey.fromMaterial(instanceType,
												  multiDraw.material,
												  multiDraw.embedded ? ContextShader.EMBEDDED : ContextShader.DEFAULT,
												  isShadow,
												  ClrwlPrograms.OitMode.OFF);

			var program = programs.get(key);

			if (program == null)
			{
				continue;
			}

			if (prevProgram != program)
			{
				if (prevProgram != null)
				{
					ClrwlProgram.unbind();
				}

				program.bind();
			}

			program.setClrwlCommonUniforms(multiDraw.material, blendOverride, ClrwlRenderingPhase.TRANSLUCENT);
			ClrwlMaterialRenderState.setup(multiDraw.material, blendOverride, bufferBlendOverrides);

			if (prevFramebuffer != framebuffer)
			{
				prevFramebuffer = framebuffer;
				framebuffer.bind();
			}

			multiDraw.submit(program);

			prevProgram = program;
		}
	}

	public void submitTranslucent(ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlFramebuffers framebuffers, boolean isShadow)
	{
		drawTranslucent(translucentDraws, programs, framebuffers, isShadow);
	}

	public void submitOitAsTranslucent(ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlFramebuffers framebuffers, boolean isShadow)
	{
		drawTranslucent(oitDraws, programs, framebuffers, isShadow);
	}

	public void submitOit(ClrwlPrograms.OitMode oit, ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlFramebuffers framebuffers, boolean isShadow, ClrwlRenderingPhase renderingPhase)
	{
		if (oitDraws.isEmpty())
		{
			return;
		}

		var programId = !isShadow
				? ClrwlProgramId.GBUFFERS_TRANSLUCENT
				: ClrwlProgramId.SHADOW_TRANSLUCENT;

		var blendOverride = framebuffers.getBlendModeOverride(programId).orElse(null);

		buffers.bindForDraw();

		ClrwlProgram prevProgram = null;

		for (var multiDraw : oitDraws)
		{
			var key = ClrwlShaderKey.fromMaterial(instanceType,
												  multiDraw.material,
												  multiDraw.embedded ? ContextShader.EMBEDDED : ContextShader.DEFAULT,
												  isShadow,
												  oit);

			var program = programs.get(key);

			if (program == null)
			{
				continue;
			}

			if (prevProgram != program)
			{
				if (prevProgram != null)
				{
					ClrwlProgram.unbind();
				}

				program.bind();
			}

			program.setClrwlCommonUniforms(multiDraw.material, blendOverride, renderingPhase);
			ClrwlMaterialRenderState.setupOit(multiDraw.material);

			multiDraw.submit(program);

			prevProgram = program;
		}
	}

	public boolean bindForCrumbling(Material material, ClrwlIndirectPrograms.PipelinePrograms programs, ClrwlBlendModeOverride blendModeOverride)
	{
		var key = ClrwlShaderKey.fromMaterial(instanceType, material, ContextShader.CRUMBLING, false, ClrwlPrograms.OitMode.OFF);
		var program = programs.get(key);

		if (program == null)
		{
			return false;
		}

		program.bind();
		buffers.bindForCrumbling();

		program.setClrwlCommonUniforms(material, blendModeOverride, ClrwlRenderingPhase.CRUMBLING);
		program.setBaseDrawUniform(0);

		return true;
	}

	private void uploadInstances(StagingBuffer stagingBuffer)
	{
		for (var instancer : instancers)
		{
			instancer.uploadInstances(stagingBuffer, buffers.objectStorage.objectBuffer.handle());
		}
	}

	private void uploadModels(StagingBuffer stagingBuffer)
	{
		var totalSize = instancers.size() * ClrwlIndirectBuffers.MODEL_STRIDE;
		var handle = buffers.model.handle();

		stagingBuffer.enqueueCopy(totalSize, handle, 0, this::writeModels);
	}

	private void uploadDraws(StagingBuffer stagingBuffer)
	{
		var totalSize = indirectDraws.size() * ClrwlIndirectBuffers.DRAW_COMMAND_STRIDE;
		var handle = buffers.draw.handle();

		stagingBuffer.enqueueCopy(totalSize, handle, 0, this::writeCommands);
	}

	private void writeModels(long writePtr)
	{
		for (var model : instancers)
		{
			model.writeModel(writePtr);
			writePtr += ClrwlIndirectBuffers.MODEL_STRIDE;
		}
	}

	private void writeCommands(long writePtr)
	{
		for (var draw : indirectDraws)
		{
			draw.write(writePtr);
			writePtr += ClrwlIndirectBuffers.DRAW_COMMAND_STRIDE;
		}
	}

	public ClrwlIndirectBuffers.PipelineBuffers makePipelineBuffers()
	{
		return buffers.makePipelineBuffers();
	}

	public ClrwlIndirectBuffers.DrawSnapshot makeDrawSnapshot()
	{
		return buffers.makeDrawSnapshot();
	}

	public void delete()
	{
		buffers.delete();
	}

	private record MultiDraw(Material material, boolean embedded, int start, int end)
	{
		private void submit(ClrwlProgram drawProgram)
		{
			GlCompat.safeMultiDrawElementsIndirect(drawProgram, GL_TRIANGLES, GL_UNSIGNED_INT, this.start, this.end, ClrwlIndirectBuffers.DRAW_COMMAND_STRIDE);
		}
	}
}
