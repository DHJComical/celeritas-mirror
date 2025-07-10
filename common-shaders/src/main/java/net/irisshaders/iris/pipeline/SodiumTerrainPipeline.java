package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisTerrainPass;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
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

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

public class SodiumTerrainPipeline {
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

	public void patchShaders(RenderPassConfiguration<?> configuration) {
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
                IRIS_LOGGER.warn("Missing program source for pass {}", pass.name());
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

                Map<ShaderType, String> transformed = TransformPatcherBridge.patchSodium(
                        sources.getName(),
                        sources.getSourcesMap(),
                        passInfo.alphaTest.orElseThrow(), inputs, parent.getTextureMap(), pass.toTerrainPass(configuration));

                for (var type : ShaderType.values()) {
                    passInfo.sources.put(type, Optional.ofNullable(transformed.get(type)));
                }

                ShaderPrinter.printProgram(sources.getName() + "_sodium_" + pass.getName()).addSources(transformed).print();

            }, () -> {
                passInfo.blendModeOverride = null;
                passInfo.bufferOverrides = Collections.emptyList();
                if (pass != IrisTerrainPass.SHADOW && pass != IrisTerrainPass.SHADOW_CUTOUT) {
                    throw new UnsupportedOperationException("Fallback vertex/fragment shaders are no longer implemented, but are needed by pass " + pass.name());
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
