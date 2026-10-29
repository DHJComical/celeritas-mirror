package net.irisshaders.iris.compat.flywheel.compile;

import net.irisshaders.iris.compat.flywheel.indirect.ClrwlBufferBindings;
import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.compile.component.IrisShaderComponent;
import net.irisshaders.iris.compat.flywheel.compile.component.SsboInstanceComponent;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlCompilationHarness;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlCompile;
import net.irisshaders.iris.compat.flywheel.compile.core.ClrwlShaderSources;
import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlUniforms;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlShaderType;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlPackDirectives;
import net.irisshaders.iris.compat.flywheel.shaderpack.ClrwlProgramGroup;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.OitPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.compile.component.InstanceStructComponent;
import dev.engine_room.flywheel.backend.compile.core.CompilationHarness;
import dev.engine_room.flywheel.backend.compile.core.Compile;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ClrwlIndirectPrograms
{
	private interface PipelineProgramsFactory
	{
		PipelinePrograms build(IrisRenderingPipeline irisPipeline);
	}

	public enum Culling
	{
		GBUFFERS_EARLY("gbuffers_early", Colorwheel.rl("internal/indirect/cull/gbuffers_early.glsl"), ClrwlProgramGroup.GBUFFERS, false, true),
		GBUFFERS_LATE("gbuffers_late", Colorwheel.rl("internal/indirect/cull/gbuffers_late.glsl"), ClrwlProgramGroup.GBUFFERS, true, true),
		GBUFFERS_FULL("gbuffers_full", Colorwheel.rl("internal/indirect/cull/gbuffers_full.glsl"), ClrwlProgramGroup.GBUFFERS, true, true),
		SHADOW_EARLY("shadow_early", Colorwheel.rl("internal/indirect/cull/shadow_early.glsl"), ClrwlProgramGroup.SHADOW, false, true),
		SHADOW_LATE("shadow_late", Colorwheel.rl("internal/indirect/cull/shadow_late.glsl"), ClrwlProgramGroup.SHADOW, true, true),
		SHADOW_FULL("shadow_full", Colorwheel.rl("internal/indirect/cull/shadow_full.glsl"), ClrwlProgramGroup.SHADOW, true, true);

		private final String name;
		private final ResourceLocation shader;
		private final ClrwlProgramGroup group;
		private final boolean occlusion;
		private final boolean frustum;

		Culling(String name, ResourceLocation shader, ClrwlProgramGroup group, boolean occlusion, boolean frustum)
		{
			this.name = name;
			this.shader = shader;
			this.group = group;
			this.occlusion = occlusion;
			this.frustum = frustum;
		}

		public String shaderName()
		{
			return this.name;
		}

		public ResourceLocation shader()
		{
			return this.shader;
		}

		public boolean useOcclusion()
		{
			return occlusion;
		}

		public boolean useFrustum()
		{
			return frustum;
		}

		public ClrwlProgramGroup programGroup()
		{
			return group;
		}

		public static class MaterialFilter
		{
			public static final int SOLID = 0b01;
			public static final int TRANSLUCENT = 0b10;

			public static final int ALL = SOLID | TRANSLUCENT;
		}
	}

	private static final ResourceLocation CULL_SHADER_API_IMPL = Colorwheel.rl("internal/indirect/cull/api_impl.glsl");
	private static final ResourceLocation TRANSFORM_SHADER_MAIN = Colorwheel.rl("internal/indirect/transform.glsl");
	private static final ResourceLocation APPLY_SHADER_MAIN = Colorwheel.rl("internal/indirect/apply.glsl");
	private static final ResourceLocation ZERO_SHADER_MAIN = Colorwheel.rl("internal/indirect/zero_models.glsl");
	private static final ResourceLocation DOWNSAMPLE_FIRST = Colorwheel.rl("internal/indirect/downsample_first.glsl");
	private static final ResourceLocation DOWNSAMPLE_SECOND = Colorwheel.rl("internal/indirect/downsample_second.glsl");

	public static final List<String> EXTENSIONS = getExtensions(GlCompat.MAX_GLSL_VERSION);
	private static final List<String> COMPUTE_EXTENSIONS = getComputeExtensions(GlCompat.MAX_GLSL_VERSION);

	private static final Compile<InstanceType<?>> TRANSFORM = new Compile<>();
	private static final ClrwlCompile<Culling, GlProgram> CULL = new ClrwlCompile<>();
	private static final Compile<ResourceLocation> UTIL = new Compile<>();

	private final ClrwlOitPrograms oitPrograms;
	private final CompilationHarness<InstanceType<?>> transform;
	private final CompilationHarness<ResourceLocation> utils;
	private final PipelineProgramsFactory programsFactory;

	// WARNING: this can ONLY be used for utils ! (otherwise, kaboom)
	private final IndirectPrograms flwPrograms;

	private ClrwlIndirectPrograms(ClrwlOitPrograms oitPrograms,
	                              CompilationHarness<InstanceType<?>> transform, CompilationHarness<ResourceLocation> utils,
	                              PipelineProgramsFactory programsFactory)
	{
		this.oitPrograms = oitPrograms;
		this.transform = transform;
		this.utils = utils;
		this.programsFactory = programsFactory;

		try
		{
			var constructor = IndirectPrograms.class.getDeclaredConstructor(PipelineCompiler.class, CompilationHarness.class, CompilationHarness.class, OitPrograms.class);
			constructor.setAccessible(true);
			this.flwPrograms = constructor.newInstance(null, null, utils, null);
		}
		catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static List<String> getExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();
		if (glslVersion.compareTo(GlslVersion.V400) < 0)
		{
			extensions.add("GL_ARB_gpu_shader5");
		}
		if (glslVersion.compareTo(GlslVersion.V420) < 0)
		{
			extensions.add("GL_ARB_shading_language_420pack");
			extensions.add("GL_ARB_shader_image_load_store");
		}
		if (glslVersion.compareTo(GlslVersion.V430) < 0)
		{
			extensions.add("GL_ARB_shader_storage_buffer_object");
			extensions.add("GL_ARB_shader_image_size");
		}
		if (glslVersion.compareTo(GlslVersion.V460) < 0)
		{
			extensions.add("GL_ARB_shader_draw_parameters");
		}
		return extensions.build();
	}

	private static List<String> getComputeExtensions(GlslVersion glslVersion)
	{
		var extensions = ImmutableList.<String>builder();

		extensions.addAll(EXTENSIONS);

		if (glslVersion.compareTo(GlslVersion.V430) < 0) {
			extensions.add("GL_ARB_compute_shader");
		}
		return extensions.build();
	}

	public static ClrwlIndirectPrograms build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean fallback)
	{
		if (!GlCompat.SUPPORTS_INDIRECT)
		{
			return null;
		}

		var pipeline = fallback
				? ClrwlPipelines.INDIRECT_FALLBACK
				: ClrwlPipelines.INDIRECT;

		var oitPrograms = new ClrwlOitPrograms(sources, pipeline);
		var transform = createTransformCompiler(sources);
		var util = createUtilCompiler(sources);

		PipelineProgramsFactory programsFactory = (irisPipeline) ->
		{
			var clrwlSources = new ClrwlShaderSources(sources, programSet, irisPipeline);
			return new PipelinePrograms(clrwlSources, pipeline);
		};

        return new ClrwlIndirectPrograms(oitPrograms, transform, util, programsFactory);
	}

	private static CompilationHarness<InstanceType<?>> createTransformCompiler(ShaderSources sources)
	{
		var shader = TRANSFORM.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
				.nameMapper(instanceType -> "colorwheel/transform_bounding_spheres/" + ResourceUtil.toDebugFileNameNoExtension(instanceType.cullShader()))
				.requireExtensions(COMPUTE_EXTENSIONS)
				.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
				.define("_CLRWL_IS_GBUFFERS_PASS", 1)
				.onCompile(($, c) -> c.define("_CLRWL_SSBO_BASE", Integer.toString(ClrwlBufferBindings.base())));

		shader = shader
				.withResource(CULL_SHADER_API_IMPL)
				.withComponent(InstanceStructComponent::new)
				.withResource(InstanceType::cullShader)
				.withComponent(SsboInstanceComponent::new)
				.withResource(TRANSFORM_SHADER_MAIN);

		return TRANSFORM.program()
				.link(shader)
				.postLink((key, program) -> ClrwlUniforms.setUniformsBlockBindings(program))
				.harness("transform_bounding_spheres", sources);
	}

	/**
	 * A compiler for cull shaders, parameterized by the instance type.
	 */
	private static ClrwlCompilationHarness<Culling, GlProgram> createCullingCompiler(ClrwlShaderSources sources, ClrwlPrograms.Pipeline pipeline)
	{
		var shader = CULL.shader(GlCompat.MAX_GLSL_VERSION, ClrwlShaderType.COMPUTE)
				.nameMapper(cull -> "cull/" + cull.shaderName())
				.requireExtensions(COMPUTE_EXTENSIONS)
				.enableExtension("GL_KHR_shader_subgroup_basic")
				.enableExtension("GL_KHR_shader_subgroup_ballot")
				.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
				.onCompile((k, c) -> ClrwlPrograms.defineClrwlPass(k.programGroup() == ClrwlProgramGroup.SHADOW, c));

		if (FORCE_DISABLE_SUBGROUP_BALLOT)
		{
			shader = shader.define("_CLRWL_FORCE_DISABLE_SUBGROUP_BALLOT", 1);
		}

		shader.onCompile((k, c) ->
		{
			ClrwlPackDirectives directives = sources.programSet().getClrwlPrograms().getClrwlDirectives();

			if (directives.getOcclusionCulling(k.programGroup()) && k.useOcclusion())
			{
				c.define("_CLRWL_OCCLUSION_CULLING", "1");
			}

			if (directives.getFrustumCulling(k.programGroup()) && k.useFrustum())
			{
				c.define("_CLRWL_FRUSTUM_CULLING", "1");
			}
		});

		shader = shader
				.withResource(CULL_SHADER_API_IMPL)
				.onCompile((k, c) ->
				{
					ClrwlPackDirectives directives = sources.programSet().getClrwlPrograms().getClrwlDirectives();

					if (k.programGroup() == ClrwlProgramGroup.SHADOW && directives.getOcclusionCulling(ClrwlProgramGroup.SHADOW) && k.useOcclusion())
					{
						var computeSrc = sources.programSet().getClrwlPrograms().getShadowDistortSource().orElseThrow();
						var src = sources.clrwlSources().getComputeSource(computeSrc);
						c.appendComponent(new IrisShaderComponent(computeSrc.getName(), src.shader()));
					}
				})
				.withResource(Culling::shader);

		return CULL.program()
				.link(shader)
				.postLink((key, program) -> ClrwlUniforms.setUniformsBlockBindings(program))
				.harness("culling", sources, ($, h) -> new GlProgram(h));
	}

	/**
	 * A compiler for utility shaders, directly compiles the shader at the resource location specified by the parameter.
	 */
	private static CompilationHarness<ResourceLocation> createUtilCompiler(ShaderSources sources)
	{
		return UTIL.program()
				.link(UTIL.shader(GlCompat.MAX_GLSL_VERSION, ShaderType.COMPUTE)
						.nameMapper(resourceLocation -> "colorwheel/utilities/" + ResourceUtil.toDebugFileNameNoExtension(resourceLocation))
						.requireExtensions(COMPUTE_EXTENSIONS)
						.define("_FLW_SUBGROUP_SIZE", GlCompat.SUBGROUP_SIZE)
						.onCompile(($, c) -> c.define("_CLRWL_SSBO_BASE", Integer.toString(ClrwlBufferBindings.base())))
						.withResource(s -> s))
				.harness("utilities", sources);
	}


	public PipelinePrograms createPipelinePrograms(IrisRenderingPipeline irisPipeline)
	{
		return programsFactory.build(irisPipeline);
	}

	public ClrwlOitPrograms getOitPrograms() { return oitPrograms; }

	public GlProgram getTransformProgram(InstanceType<?> instanceType)
	{
		return this.transform.get(instanceType);
	}

	public GlProgram getApplyProgram()
	{
		return utils.get(APPLY_SHADER_MAIN);
	}

	public GlProgram getZeroModelsProgram()
	{
		return utils.get(ZERO_SHADER_MAIN);
	}

	public GlProgram getScatterProgram()
	{
		return flwPrograms.getScatterProgram();
	}

	public GlProgram getDownsampleFirstProgram()
	{
		return utils.get(DOWNSAMPLE_FIRST);
	}

	public GlProgram getDownsampleSecondProgram()
	{
		return utils.get(DOWNSAMPLE_SECOND);
	}

	// WARNING: Should ONLY be used for utils programs
	public IndirectPrograms getFlwPrograms()
	{
		return flwPrograms;
	}

	public void delete()
	{
		oitPrograms.delete();
		transform.delete();
		utils.delete();

		// flwPrograms is not deleted as it contains null references (=> kaboom)
	}

	public static class PipelinePrograms
	{
		ClrwlPrograms clrwlPrograms;
		ClrwlCompilationHarness<Culling, GlProgram> cullPrograms;

		public PipelinePrograms(ClrwlShaderSources sources, ClrwlPrograms.Pipeline pipeline)
		{
			this.clrwlPrograms = new ClrwlPrograms(sources, pipeline);
			this.cullPrograms = createCullingCompiler(sources, pipeline);
		}

		@Nullable
		public ClrwlProgram get(ClrwlShaderKey key)
		{
			return clrwlPrograms.get(key);
		}

		public GlProgram getCullingProgram(Culling culling)
		{
			return cullPrograms.get(culling);
		}

		public void delete()
		{
			clrwlPrograms.delete();
			cullPrograms.delete();
		}
	}

	private static boolean FORCE_DISABLE_SUBGROUP_BALLOT = false;

	public static void toggleSubgroupBallot(boolean enabled)
	{
		FORCE_DISABLE_SUBGROUP_BALLOT = !enabled;
	}
}
