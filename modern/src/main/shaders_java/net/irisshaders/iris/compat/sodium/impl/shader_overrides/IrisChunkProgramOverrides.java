package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import org.embeddedt.embeddium.impl.gl.GlObject;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL43C;

import java.util.*;

public class IrisChunkProgramOverrides {
    private static final List<ShaderType> RELEVANT_SHADER_TYPES = List.of(ShaderType.VERTEX, ShaderType.GEOM, ShaderType.TESS_CTRL, ShaderType.TESS_EVALUATE, ShaderType.FRAGMENT);

	private final EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> programs = new EnumMap<>(IrisTerrainPass.class);
	private boolean shadersCreated = false;
	private int versionCounterForSodiumShaderReload = -1;

	private GlShader createShader(ShaderType type, IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		Optional<String> irisVertexShader;

        if (pass == IrisTerrainPass.SHADOW_CUTOUT && type != ShaderType.FRAGMENT) {
            pass = IrisTerrainPass.SHADOW;
        }

        var info = pipeline.getPassInfo(pass);

        if (info == null) {
            throw new IllegalArgumentException("Unknown pass type " + pass);
        }

        irisVertexShader = info.sources().get(type);

		String source = irisVertexShader.orElse(null);

		if (source == null) {
			return null;
		}

		return new GlShader(type, "iris:" +
			"sodium-terrain-" + pass.toString().toLowerCase(Locale.ROOT) + "." + type.fileExtension, source);
	}

	private BlendModeOverride getBlendOverride(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
        return pipeline.getPassInfo(pass).blendModeOverride();
	}

	private List<BufferBlendOverride> getBufferBlendOverride(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
        return pipeline.getPassInfo(pass).bufferOverrides();
	}

	@Nullable
	private GlProgram<IrisChunkShaderInterface> createShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline, RenderPassConfiguration configuration) {
        Map<ShaderType, GlShader> createdShaders = new EnumMap<>(ShaderType.class);

        for (var type : RELEVANT_SHADER_TYPES) {
            var shader = createShader(type, pass, pipeline);
            if (shader != null) {
                createdShaders.put(type, shader);
            }
        }

		BlendModeOverride blendOverride = getBlendOverride(pass, pipeline);
		List<BufferBlendOverride> bufferOverrides = getBufferBlendOverride(pass, pipeline);
		float alpha = getAlphaReference(pass, pipeline);

		if (createdShaders.get(ShaderType.VERTEX) == null || createdShaders.get(ShaderType.FRAGMENT) == null) {
            createdShaders.values().forEach(GlShader::delete);

			// TODO: Partial shader programs?
			return null;
		}

		try {
			GlProgram.Builder builder = GlProgram.builder("sodium:chunk_shader_for_"
				+ pass.getName());

            createdShaders.values().forEach(builder::attachShader);

            int i = 0;
            var vertexType = configuration.getVertexTypeForPass(pass.toTerrainPass(configuration));
            for (var attr : vertexType.getVertexFormat().getAttributes()) {
                builder.bindAttribute(attr.getName(), i++);
            }
			return builder
				.link((shader) -> {
					int handle = ((GlObject) shader).handle();
					ShaderBindingContext contextExt = shader;
					GLDebug.nameObject(GL43C.GL_PROGRAM, handle, "sodium-terrain-" + pass.toString().toLowerCase(Locale.ROOT));
					return new IrisChunkShaderInterface(handle, contextExt, pipeline, new ChunkShaderOptions(List.of(ChunkFogMode.SMOOTH), pass.toTerrainPass(configuration)),
						createdShaders.get(ShaderType.TESS_CTRL) != null || createdShaders.get(ShaderType.TESS_EVALUATE) != null, pass == IrisTerrainPass.SHADOW || pass == IrisTerrainPass.SHADOW_CUTOUT, blendOverride, bufferOverrides, alpha, pipeline.getCustomUniforms());
				});
		} finally {
            createdShaders.values().forEach(GlShader::delete);
		}
	}

	private float getAlphaReference(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
        return pipeline.getPassInfo(pass).alphaTest().orElseThrow(() -> new IllegalStateException("Expected alpha test to be available")).reference();
	}

	public void createShaders(SodiumTerrainPipeline pipeline, RenderPassConfiguration configuration) {
		if (pipeline != null) {
			pipeline.patchShaders(configuration);
			for (IrisTerrainPass pass : IrisTerrainPass.values()) {
				if (pass.isShadow() && !pipeline.hasShadowPass()) {
					this.programs.put(pass, null);
					continue;
				}

				this.programs.put(pass, createShader(pass, pipeline, configuration));
			}
		} else {
			for (GlProgram<?> program : this.programs.values()) {
				if (program != null) {
					program.delete();
				}
			}
			this.programs.clear();
		}

		shadersCreated = true;
	}

	@Nullable
	public GlProgram<IrisChunkShaderInterface> getProgramOverride(TerrainRenderPass pass, RenderPassConfiguration configuration) {
		if (versionCounterForSodiumShaderReload != Iris.getPipelineManager().getVersionCounterForSodiumShaderReload()) {
			versionCounterForSodiumShaderReload = Iris.getPipelineManager().getVersionCounterForSodiumShaderReload();
			deleteShaders();
		}

		WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
		SodiumTerrainPipeline sodiumTerrainPipeline = null;

		if (worldRenderingPipeline != null) {
			sodiumTerrainPipeline = worldRenderingPipeline.getSodiumTerrainPipeline();
		}

		if (!shadersCreated) {
			createShaders(sodiumTerrainPipeline, configuration);
		}

		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			if (sodiumTerrainPipeline != null && !sodiumTerrainPipeline.hasShadowPass()) {
				throw new IllegalStateException("Shadow program requested, but the pack does not have a shadow pass?");
			}

			if (pass.supportsFragmentDiscard()) {
				return this.programs.get(IrisTerrainPass.SHADOW_CUTOUT);
			} else {
				return this.programs.get(IrisTerrainPass.SHADOW);
			}
		} else {
			if (pass.supportsFragmentDiscard()) {
				return this.programs.get(IrisTerrainPass.GBUFFER_CUTOUT);
			} else if (pass.isReverseOrder()) {
				return this.programs.get(IrisTerrainPass.GBUFFER_TRANSLUCENT);
			} else {
				return this.programs.get(IrisTerrainPass.GBUFFER_SOLID);
			}
		}
	}

	public void deleteShaders() {
		for (GlProgram<?> program : this.programs.values()) {
			if (program != null) {
				program.delete();
			}
		}

		this.programs.clear();
		shadersCreated = false;
	}
}
