package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisTerrainPass;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.compat.mc.MCResourceLocation;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.pipeline.foss_transform.TransformPatcherBridge;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class SodiumTerrainPipeline {
	private static final String defaultVertex = """
		#version 330 core

		in ivec2 a_LightCoord;
		in vec4 a_Color;
		in vec2 a_TexCoord;
		in uvec4 a_PosId;
		uniform mat4 iris_ProjectionMatrix;
		uniform int fogShape;
		uniform mat4 iris_ModelViewMatrix;
		uniform vec3 u_RegionOffset;
		vec3 _vert_position;
		vec2 _vert_tex_diffuse_coord;
		ivec2 _vert_tex_light_coord;
		vec4 _vert_color;
		uint _draw_id;
		uint _material_params;
		out float v_FragDistance;


		const int FOG_SHAPE_SPHERICAL = 0;
		const int FOG_SHAPE_CYLINDRICAL = 1;

		vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
		    float factor = smoothstep(fogStart, fogEnd, fragDistance * fogColor.a); // alpha value of fog is used as a weight
		    vec3 blended = mix(fragColor.rgb, fogColor.rgb, factor);

		    return vec4(blended, fragColor.a); // alpha value of fragment cannot be modified
		}

		float getFragDistance(int fogShape, vec3 position) {
		    // Use the maximum of the horizontal and vertical distance to get cylindrical fog if fog shape is cylindrical
		    switch (fogShape) {
		        case FOG_SHAPE_SPHERICAL: return length(position);
		        case FOG_SHAPE_CYLINDRICAL: return max(length(position.xz), abs(position.y));
		        default: return length(position); // This shouldn't be possible to get, but return a sane value just in case
		    }
		}

		out vec4 v_ColorModulator;
		out vec2 v_TexCoord;
		out float v_MaterialMipBias;
		out float v_MaterialAlphaCutoff;
		const uint MATERIAL_USE_MIP_OFFSET = 0u;
		const uint MATERIAL_ALPHA_CUTOFF_OFFSET = 1u;

		float _material_mip_bias(uint material) {
			return ((material >> MATERIAL_USE_MIP_OFFSET) & 1u) != 0u ? 0.0f : -4.0f;
		}

		const float[4] ALPHA_CUTOFF = float[4](0.0, 0.1, 0.5, 1.0);

		float _material_alpha_cutoff(uint material) {
		    return ALPHA_CUTOFF[(material >> MATERIAL_ALPHA_CUTOFF_OFFSET) & 3u];
		}

		void _vert_init() {
			_vert_position = (vec3(a_PosId.xyz) * 4.8828125E-4f + -8.0f);

		""" +
		"_vert_tex_diffuse_coord = (a_TexCoord * " + (1.0f / 32768.0f) + ");" +
		"""
				_vert_tex_light_coord = a_LightCoord;
				_vert_color = a_Color;
				_draw_id = (a_PosId.w >> 8u) & 0xffu;
				_material_params = (a_PosId.w >> 0u) & 0xFFu;
			}
			uvec3 _get_relative_chunk_coord(uint pos) {
				return uvec3(pos) >> uvec3(5u, 0u, 2u) & uvec3(7u, 3u, 7u);
			}
			vec3 _get_draw_translation(uint pos) {
				return _get_relative_chunk_coord(pos) * vec3(16.0f);
			}
			vec4 getVertexPosition() {
				return vec4(_vert_position + u_RegionOffset + _get_draw_translation(_draw_id), 1.0f);
			}

			uniform sampler2D lightmap; // The light map texture

			vec3 _sample_lightmap(ivec2 uv) {
			    return texture(lightmap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0))).rgb;
			}


			void main() {
			    _vert_init();

			    // Transform the chunk-local vertex position into world model space
			    vec3 translation = u_RegionOffset + _get_draw_translation(_draw_id);
			    vec3 position = _vert_position + translation;

			    v_FragDistance = getFragDistance(fogShape, position);

			    // Transform the vertex position into model-view-projection space
			    gl_Position = iris_ProjectionMatrix * iris_ModelViewMatrix * vec4(position, 1.0);

			    v_ColorModulator = vec4((_vert_color.rgb * _vert_color.a), 1) * vec4(_sample_lightmap(_vert_tex_light_coord), 1.0);
			    v_TexCoord = _vert_tex_diffuse_coord;

			    v_MaterialMipBias = _material_mip_bias(_material_params);
			    v_MaterialAlphaCutoff = _material_alpha_cutoff(_material_params);
			}
			""";
	private static final String defaultFragment = """
		#version 330 core

		const int FOG_SHAPE_SPHERICAL = 0;
		const int FOG_SHAPE_CYLINDRICAL = 1;

		vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
		    float factor = smoothstep(fogStart, fogEnd, fragDistance * fogColor.a); // alpha value of fog is used as a weight
		    vec3 blended = mix(fragColor.rgb, fogColor.rgb, factor);

		    return vec4(blended, fragColor.a); // alpha value of fragment cannot be modified
		}

		in vec4 v_ColorModulator; // The interpolated vertex color
		in vec2 v_TexCoord; // The interpolated block texture coordinates

		in float v_FragDistance; // The fragment's distance from the camera

		in float v_MaterialMipBias;
		in float v_MaterialAlphaCutoff;

		uniform sampler2D gtexture; // The block atlas texture

		uniform vec4 iris_FogColor; // The color of the shader fog
		uniform float iris_FogStart; // The starting position of the shader fog
		uniform float iris_FogEnd; // The ending position of the shader fog

		out vec4 out_FragColor; // The output fragment for the color framebuffer

		void main() {
		    vec4 diffuseColor = texture(gtexture, v_TexCoord, v_MaterialMipBias);

		    if (diffuseColor.a < 0.1) {
		        discard;
		    }

		    // Modulate the color (used by ambient occlusion and per-vertex colouring)
		    diffuseColor.rgb *= v_ColorModulator.rgb;

		    out_FragColor = _linearFog(diffuseColor, v_FragDistance, iris_FogColor, iris_FogStart, iris_FogEnd);
		}
		""";
	private final WorldRenderingPipeline parent;
	private final CustomUniforms customUniforms;
	private final IntFunction<ProgramSamplers> createTerrainSamplers;
	private final IntFunction<ProgramSamplers> createShadowSamplers;
	private final IntFunction<ProgramImages> createTerrainImages;
	private final IntFunction<ProgramImages> createShadowImages;

    @Getter
    @Accessors(fluent = true)
    public static final class PassInfo {
        private final Map<ShaderType, Optional<String>> sources = new EnumMap<>(ShaderType.class);
        private GlFramebuffer framebuffer;
        private BlendModeOverride blendModeOverride;
        private List<BufferBlendOverride> bufferOverrides;
        private Optional<AlphaTest> alphaTest;

        private PassInfo() {
            for (var shaderType : ShaderType.values()) {
                sources.put(shaderType, Optional.empty());
            }
        }
    }

    private final Map<IrisTerrainPass, PassInfo> passInfoMap;
    private final Map<IrisTerrainPass, Optional<ProgramSource>> gbufferProgramSource;

	ProgramSet programSet;

	public SodiumTerrainPipeline(WorldRenderingPipeline parent, ProgramSet programSet, IntFunction<ProgramSamplers> createTerrainSamplers,
								 IntFunction<ProgramSamplers> createShadowSamplers, IntFunction<ProgramImages> createTerrainImages, IntFunction<ProgramImages> createShadowImages,
								 RenderTargets targets,
								 ImmutableSet<Integer> flippedAfterPrepare,
								 ImmutableSet<Integer> flippedAfterTranslucent, GlFramebuffer shadowFramebuffer, CustomUniforms customUniforms) {
		this.parent = Objects.requireNonNull(parent);
		this.customUniforms = customUniforms;

        this.gbufferProgramSource = new EnumMap<>(IrisTerrainPass.class);
        gbufferProgramSource.put(IrisTerrainPass.GBUFFER_SOLID, first(programSet.getGbuffersTerrainSolid(), programSet.getGbuffersTerrain(), programSet.getGbuffersTexturedLit(), programSet.getGbuffersTextured(), programSet.getGbuffersBasic()));
        gbufferProgramSource.put(IrisTerrainPass.GBUFFER_CUTOUT, first(programSet.getGbuffersTerrainCutout(), programSet.getGbuffersTerrain(), programSet.getGbuffersTexturedLit(), programSet.getGbuffersTextured(), programSet.getGbuffersBasic()));
        gbufferProgramSource.put(IrisTerrainPass.GBUFFER_TRANSLUCENT, first(programSet.getGbuffersWater(), gbufferProgramSource.get(IrisTerrainPass.GBUFFER_CUTOUT)));
        gbufferProgramSource.put(IrisTerrainPass.SHADOW, programSet.getShadow());

		this.programSet = programSet;
        this.passInfoMap = new EnumMap<>(IrisTerrainPass.class);

        for (var pass : IrisTerrainPass.values()) {
            var passInfo = new PassInfo();
            this.passInfoMap.put(pass, passInfo);

            if (pass == IrisTerrainPass.SHADOW || pass == IrisTerrainPass.SHADOW_CUTOUT) {
                passInfo.framebuffer = shadowFramebuffer;
            } else {
                var programSource = gbufferProgramSource.get(pass);

                if (programSource != null) {
                    // embeddedt - buffers to use, I think?
                    ImmutableSet<Integer> flipped = pass == IrisTerrainPass.GBUFFER_TRANSLUCENT ? flippedAfterTranslucent : flippedAfterPrepare;
                    programSource.ifPresentOrElse(
                            sources -> passInfo.framebuffer = targets.createGbufferFramebuffer(flipped, sources.getDirectives().getDrawBuffers()),
                            () -> passInfo.framebuffer = targets.createGbufferFramebuffer(flipped, new int[] {0}));
                }
            }
        }

		this.createTerrainSamplers = createTerrainSamplers;
		this.createShadowSamplers = createShadowSamplers;
		this.createTerrainImages = createTerrainImages;
		this.createShadowImages = createShadowImages;
	}

	@SafeVarargs
	private static <T> Optional<T> first(Optional<T>... candidates) {
		for (Optional<T> candidate : candidates) {
			if (candidate.isPresent()) {
				return candidate;
			}
		}

		return Optional.empty();
	}

	public static String parseSodiumImport(String shader) {
		Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<namespace>.*):(?<path>.*)>");
		Matcher matcher = IMPORT_PATTERN.matcher(shader);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Malformed import statement (expected format: " + IMPORT_PATTERN + ")");
		}

		String namespace = matcher.group("namespace");
		String path = matcher.group("path");

        MCResourceLocation identifier = MINECRAFT_SHIM.makeResourceLocation(namespace, path);
		return "";
	}

	public void patchShaders(RenderPassConfiguration configuration) {
		ShaderAttributeInputs inputs = new ShaderAttributeInputs(true, true, false, true, true);

        for (var pass : IrisTerrainPass.values()) {
            var passInfo = passInfoMap.get(pass);
            var programId = switch (pass) {
                case GBUFFER_TRANSLUCENT -> ProgramId.Water;
                case SHADOW -> ProgramId.Shadow;
                default -> ProgramId.Terrain;
            };
            var programSource = gbufferProgramSource.get(pass == IrisTerrainPass.SHADOW_CUTOUT ? IrisTerrainPass.SHADOW : pass);
            if (programSource == null) {
                Celeritas.logger().warn("Missing program source for pass {}", pass.name());
                continue;
            }
            programSource.ifPresentOrElse(sources -> {
                passInfo.blendModeOverride = sources.getDirectives().getBlendModeOverride().orElse(programId.getBlendModeOverride());
                passInfo.bufferOverrides = new ArrayList<>();
                sources.getDirectives().getBufferBlendOverrides().forEach(information -> {
                    int index = Ints.indexOf(sources.getDirectives().getDrawBuffers(), information.index());
                    if (index > -1) {
                        passInfo.bufferOverrides.add(new BufferBlendOverride(index, information.blendMode()));
                    }
                });

                AlphaTest defaultPassAlpha = switch (pass) {
                    case SHADOW_CUTOUT, GBUFFER_CUTOUT -> AlphaTests.ONE_TENTH_ALPHA;
                    default -> AlphaTest.ALWAYS;
                };

                passInfo.alphaTest = sources.getDirectives().getAlphaTestOverride().or(() -> Optional.of(defaultPassAlpha));

                Map<PatchShaderType, String> transformed = TransformPatcherBridge.patchSodium(
                        sources.getName(),
                        sources.getVertexSource().orElse(null),
                        sources.getGeometrySource().orElse(null),
                        sources.getTessControlSource().orElse(null),
                        sources.getTessEvalSource().orElse(null),
                        sources.getFragmentSource().orElse(null),
                        passInfo.alphaTest.orElseThrow(), inputs, parent.getTextureMap());

                for (var type : PatchShaderType.values()) {
                    passInfo.sources.put(type.glShaderType, Optional.ofNullable(transformed.get(type)));
                }

                ShaderPrinter.printProgram(sources.getName() + "_sodium_" + pass.getName()).addSources(transformed).print();

            }, () -> {
                passInfo.blendModeOverride = null;
                passInfo.bufferOverrides = Collections.emptyList();
                if (pass != IrisTerrainPass.SHADOW && pass != IrisTerrainPass.SHADOW_CUTOUT) {
                    passInfo.sources.put(ShaderType.VERTEX, Optional.of(defaultVertex));
                    passInfo.sources.put(ShaderType.FRAGMENT, Optional.of(defaultFragment));
                }
            });
        }
	}

	public ProgramUniforms.Builder initUniforms(int programId) {
		ProgramUniforms.Builder uniforms = ProgramUniforms.builder("<sodium shaders>", programId);

		CommonUniforms.addDynamicUniforms(uniforms, FogMode.PER_VERTEX);
		customUniforms.assignTo(uniforms);

		BuiltinReplacementUniforms.addBuiltinReplacementUniforms(uniforms);

		return uniforms;
	}

	public boolean hasShadowPass() {
		return createShadowSamplers != null;
	}

    public PassInfo getPassInfo(IrisTerrainPass pass) {
        var info = this.passInfoMap.get(pass);
        if (info == null) {
            throw new IllegalArgumentException("Unknown pass type " + pass);
        }
        return info;
    }

	public ProgramSamplers initTerrainSamplers(int programId) {
		return createTerrainSamplers.apply(programId);
	}

	public ProgramSamplers initShadowSamplers(int programId) {
		return createShadowSamplers.apply(programId);
	}

	public ProgramImages initTerrainImages(int programId) {
		return createTerrainImages.apply(programId);
	}

	public ProgramImages initShadowImages(int programId) {
		return createShadowImages.apply(programId);
	}

	public CustomUniforms getCustomUniforms() {
		return customUniforms;
	}
}
