package net.irisshaders.iris.pipeline.programs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer;
import net.irisshaders.iris.pipeline.foss_transform.CloudVertices;
import net.irisshaders.iris.pipeline.foss_transform.TransformPatcherBridge;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.VanillaUniforms;
import net.irisshaders.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ShaderCreator {
	// Gbuffer programs see these vertex attributes with an iris_ prefix, so that they can't clash with names in the
	// shader pack's code.
	//? if >=26.1 {
	/*// The integer attributes are read through iris_-prefixed inputs, as declared by ShaderTransformer.patchIntegerAttribute.
	private static final Set<String> PREFIXED_ATTRIBUTES = Set.of("Position", "Color", "Normal", "UV0", "UV1", "UV2", "LineWidth", "mc_Entity", "at_midBlock");
	*///?} else
	private static final Set<String> PREFIXED_ATTRIBUTES = Set.of("Position", "Color", "Normal", "UV0", "UV1", "UV2", "LineWidth");

	public static PendingGbufferProgram create(IrisRenderingPipeline parent, String name, ProgramSource source, ProgramId programId,
											   GlFramebuffer writingToBeforeTranslucent, GlFramebuffer writingToAfterTranslucent,
											   AlphaTest fallbackAlpha, VertexFormat vertexFormat, ShaderAttributeInputs inputs,
											   Supplier<ImmutableSet<Integer>> flipped, boolean isIntensity, boolean isShadowPass,
											   boolean isLines, boolean isClouds, CustomUniforms customUniforms) {
		AlphaTest alpha = source.getDirectives().getAlphaTestOverride().orElse(fallbackAlpha);
		BlendModeOverride blendModeOverride = source.getDirectives().getBlendModeOverride().orElse(programId.getBlendModeOverride());

		List<BufferBlendOverride> bufferBlendOverrides = new ArrayList<>();
		source.getDirectives().getBufferBlendOverrides().forEach(information -> {
			int index = Ints.indexOf(source.getDirectives().getDrawBuffers(), information.index());
			if (index > -1) {
				bufferBlendOverrides.add(new BufferBlendOverride(index, information.blendMode()));
			}
		});

		CompletableFuture<Map<ShaderType, String>> transformed = CompletableFuture.supplyAsync(() -> {
			Preconditions.checkArgument(source.isValid());
			return TransformPatcherBridge.patchVanilla(name, source.getSourcesMap(), alpha, isLines, isClouds, true, inputs, parent.getTextureMap());
		}, TransformPatcherBridge.EXECUTOR);

		//? if >=26.1 {
		/*boolean generatesClouds = isClouds;
		*///?} else
		boolean generatesClouds = false;
		ImmutableSet<Integer> reservedTextureUnits = generatesClouds ? IrisSamplers.CLOUDS_RESERVED_TEXTURE_UNITS : IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS;

		return new PendingGbufferProgram(name, attributeLocations(vertexFormat), reservedTextureUnits, transformed, (builder, sources) -> {
			if (generatesClouds) {
				builder.addExternalSampler(IrisSamplers.CLOUD_FACES_TEXTURE_UNIT, CloudVertices.CLOUD_FACES_SAMPLER);
			}

			CommonUniforms.addDynamicUniforms(builder, FogMode.PER_VERTEX);
			customUniforms.assignTo(builder);
			BuiltinReplacementUniforms.addBuiltinReplacementUniforms(builder);
			VanillaUniforms.addVanillaUniforms(builder);
			parent.addGbufferOrShadowSamplers(builder, builder, flipped, isShadowPass, inputs.hasTex(), inputs.hasLight(), inputs.hasOverlay());

			boolean usesTessellation = sources.get(ShaderType.TESS_CTRL) != null || sources.get(ShaderType.TESS_EVALUATE) != null;

			return new GbufferProgram(builder, customUniforms, parent, writingToBeforeTranslucent, writingToAfterTranslucent,
				blendModeOverride, bufferBlendOverrides, alpha.reference(), isIntensity, usesTessellation);
		});
	}

	public static PendingGbufferProgram createFallback(String name, GlFramebuffer writingToBeforeTranslucent,
													   GlFramebuffer writingToAfterTranslucent, AlphaTest alpha,
													   VertexFormat vertexFormat, @Nullable BlendModeOverride blendModeOverride,
													   IrisRenderingPipeline parent, FogMode fogMode, boolean entityLighting,
													   boolean isGlint, boolean isText, boolean intensityTex, boolean isFullbright) {
		ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright, false, isGlint, isText);

		// TODO: Is this check sound in newer versions?
		boolean isLeash = vertexFormat == DefaultVertexFormat.POSITION_COLOR_LIGHTMAP;
		Map<ShaderType, String> sources = new EnumMap<>(ShaderType.class);
		sources.put(ShaderType.VERTEX, ShaderSynthesizer.vsh(true, inputs, fogMode, entityLighting, isLeash));
		sources.put(ShaderType.FRAGMENT, ShaderSynthesizer.fsh(inputs, fogMode, alpha, intensityTex, isLeash));

		return new PendingGbufferProgram(name, attributeLocations(vertexFormat), IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS, CompletableFuture.completedFuture(sources), (builder, s) -> {
			builder.addExternalSampler(IrisSamplers.ALBEDO_TEXTURE_UNIT, "gtexture");
			builder.addExternalSampler(IrisSamplers.OVERLAY_TEXTURE_UNIT, "overlay");
			builder.addExternalSampler(IrisSamplers.LIGHTMAP_TEXTURE_UNIT, "lightmap");

			VanillaUniforms.addVanillaUniforms(builder);
			builder.uniform1f(UniformUpdateFrequency.ONCE, "iris_AlphaTestValue", alpha::reference);
			builder.uniform1f("iris_FogDensity", () -> Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity()), listener -> {
			});
			builder.uniform1i("iris_FogIsExp2", () -> CapturedRenderingState.INSTANCE.getFogDensity() >= 0.0F ? 1 : 0, listener -> {
			});

			return new GbufferProgram(builder, null, parent, writingToBeforeTranslucent, writingToAfterTranslucent,
				blendModeOverride, List.of(), alpha.reference(), false, false);
		});
	}

	/**
	 * Binds each attribute to its index in the vertex format, which is where vanilla's buffers provide it.
	 */
	private static Map<String, Integer> attributeLocations(VertexFormat vertexFormat) {
		List<String> names = vertexFormat.getElementAttributeNames();
		Map<String, Integer> locations = new HashMap<>();

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			locations.put(PREFIXED_ATTRIBUTES.contains(name) ? "iris_" + name : name, i);
		}

		return locations;
	}
}
