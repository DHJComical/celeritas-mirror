package net.irisshaders.iris.compat.flywheel.compile;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlShaderSources;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClrwlInstancedPrograms
{
	private interface PipelineProgramsFactory
	{
		PipelinePrograms build(IrisRenderingPipeline irisPipeline);
	}

	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);

	private final ClrwlOitPrograms oitPrograms;
	private final PipelineProgramsFactory programsFactory;

	private ClrwlInstancedPrograms(ClrwlOitPrograms oitPrograms, PipelineProgramsFactory programsFactory)
	{
		this.oitPrograms = oitPrograms;
		this.programsFactory = programsFactory;
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();

		if (glslVersion.compareTo(GlslVersion.V330) < 0)
		{
			extensions.add("GL_ARB_shader_bit_encoding");
		}

		return extensions.build();
	}

	public static ClrwlInstancedPrograms build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean fallback)
	{
		if (!GlCompat.SUPPORTS_INSTANCING)
		{
			return null;
		}

		var pipeline = fallback
				? ClrwlPipelines.INSTANCING_FALLBACK
				: ClrwlPipelines.INSTANCING;

		var oitPrograms = new ClrwlOitPrograms(sources, pipeline);
		PipelineProgramsFactory programsFactory = (irisPipeline) ->
		{
			var clrwlSources = new ClrwlShaderSources(sources, programSet, irisPipeline);
			return new PipelinePrograms(clrwlSources, pipeline);
		};

		return new ClrwlInstancedPrograms(oitPrograms, programsFactory);
	}

	public PipelinePrograms createPipelinePrograms(IrisRenderingPipeline irisPipeline)
	{
		return programsFactory.build(irisPipeline);
	}

	public ClrwlOitPrograms getOitPrograms()
	{
		return oitPrograms;
	}

	public void delete()
	{
		oitPrograms.delete();
	}

	public static class PipelinePrograms
	{
		private final ClrwlPrograms clrwlPrograms;

		private PipelinePrograms(ClrwlShaderSources sources, ClrwlPrograms.Pipeline pipeline)
		{
			this.clrwlPrograms = new ClrwlPrograms(sources, pipeline);
		}

		@Nullable
		public ClrwlProgram get(ClrwlShaderKey key)
		{
			return clrwlPrograms.get(key);
		}

		public void delete()
		{
			clrwlPrograms.delete();
		}
	}
}
