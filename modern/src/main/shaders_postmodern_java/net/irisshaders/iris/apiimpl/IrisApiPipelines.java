package net.irisshaders.iris.apiimpl;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.api.v0.IrisShadowProgram;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTestFunction;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

/**
 * Turns the API's program enums into the {@link ShaderKey} a mod's pipeline draws with.
 */
final class IrisApiPipelines {
	private IrisApiPipelines() {
	}

	static void assign(RenderPipeline pipeline, IrisProgram program) {
		ShaderKey key = findBestMatch(pipeline, toProgramId(program));

		if (key != null) {
			IrisPipelines.assignPipeline(pipeline, key);
		}
	}

	static void assignShadow(RenderPipeline pipeline, IrisShadowProgram program) {
		ShaderKey key = findBestMatch(pipeline, toProgramId(program));

		if (key != null) {
			IrisPipelines.assignPipelineShadow(pipeline, key);
		}
	}

	private static ProgramId toProgramId(IrisProgram program) {
		return switch (program) {
			case BASIC -> ProgramId.Basic;
			case TEXTURED -> ProgramId.Textured;
			case TERRAIN -> ProgramId.Terrain;
			case TERRAIN_SOLID -> ProgramId.TerrainSolid;
			case TERRAIN_CUTOUT -> ProgramId.TerrainCutout;
			case TRANSLUCENT -> ProgramId.Water;
			case SKY_BASIC -> ProgramId.SkyBasic;
			case SKY_TEXTURED -> ProgramId.SkyTextured;
			case ARMOR_GLINT -> ProgramId.ArmorGlint;
			case ENTITIES -> ProgramId.Entities;
			case ENTITIES_TRANSLUCENT -> ProgramId.EntitiesTrans;
			case CLOUDS -> ProgramId.Clouds;
			case BLOCK -> ProgramId.Block;
			case BLOCK_TRANSLUCENT -> ProgramId.BlockTrans;
			case HAND -> ProgramId.Hand;
			case HAND_TRANSLUCENT -> ProgramId.HandWater;
			case PARTICLES -> ProgramId.Particles;
			case PARTICLES_TRANSLUCENT -> ProgramId.ParticlesTrans;
			case EMISSIVE_ENTITIES -> ProgramId.SpiderEyes;
			case BEACON_BEAM -> ProgramId.BeaconBeam;
			case LINES -> ProgramId.Line;
		};
	}

	private static ProgramId toProgramId(IrisShadowProgram program) {
		return switch (program) {
			case SHADOW -> ProgramId.Shadow;
			case SHADOW_TERRAIN_CUTOUT -> ProgramId.ShadowCutout;
			case SHADOW_TRANSLUCENT -> ProgramId.ShadowWater;
			case SHADOW_ENTITIES -> ProgramId.ShadowEntities;
			case SHADOW_LIGHTNING -> ProgramId.ShadowLightning;
			case SHADOW_BLOCK -> ProgramId.ShadowBlock;
		};
	}

	/**
	 * Picks a key with the program, preferring one built for the pipeline's vertex format, and then one that alpha
	 * tests if the pipeline cuts out. Returns null if no key uses the program.
	 */
	@Nullable
	private static ShaderKey findBestMatch(RenderPipeline pipeline, ProgramId program) {
		boolean cutout = pipeline.getShaderDefines().flags().contains("ALPHA_CUTOUT")
			|| pipeline.getShaderDefines().values().containsKey("ALPHA_CUTOUT");
		ShaderKey best = null;
		int bestScore = -1;

		for (ShaderKey key : ShaderKey.values()) {
			if (key.getProgram() != program) {
				continue;
			}

			int score = (key.getVertexFormat() == pipeline.getVertexFormat() ? 2 : 0)
				+ (cutout && alphaTests(key.getAlphaTest()) ? 1 : 0);

			if (score > bestScore) {
				best = key;
				bestScore = score;
			}
		}

		if (best == null) {
			CeleritasShaders.logger().warn("No Iris program {} for pipeline {}, so it keeps drawing with its own shaders", program, pipeline.getLocation());
		} else if (best.getVertexFormat() != pipeline.getVertexFormat()) {
			CeleritasShaders.logger().warn("Pipeline {} was assigned {}, which expects a different vertex format", pipeline.getLocation(), best);
		}

		return best;
	}

	private static boolean alphaTests(AlphaTest test) {
		return test.reference() > 0.01f && test.function() != AlphaTestFunction.NEVER;
	}
}
