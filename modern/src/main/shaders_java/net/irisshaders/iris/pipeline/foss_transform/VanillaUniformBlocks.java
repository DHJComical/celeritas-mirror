package net.irisshaders.iris.pipeline.foss_transform;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.taumc.glsl.Transformer;

import java.util.List;

/**
 * The uniform blocks vanilla provides its values in on 26.1, declared with {@code iris_} names for the block and its
 * members. Blocks are matched by binding and std140 layout, not by name, so the members can be renamed freely, but each
 * must be declared in vanilla's order.
 */
public enum VanillaUniformBlocks {
	DYNAMIC_TRANSFORMS("DynamicTransforms",
		"mat4 iris_ModelViewMat",
		"vec4 iris_ColorModulator",
		"vec3 iris_ChunkOffset",
		"mat4 iris_TextureMat"),
	PROJECTION("Projection",
		"mat4 iris_ProjMat"),
	GLOBALS("Globals",
		"ivec3 iris_CameraBlockPos",
		"vec3 iris_CameraOffset",
		"vec2 iris_ScreenSize",
		"float iris_GlintAlpha",
		"float iris_GameTime",
		"int iris_MenuBlurRadius",
		"int iris_UseRgss"),
	FOG("Fog",
		"vec4 iris_FogColor",
		"float iris_FogEnvironmentalStart",
		"float iris_FogEnvironmentalEnd",
		"float iris_FogRenderDistanceStart",
		"float iris_FogRenderDistanceEnd",
		"float iris_FogSkyEnd",
		"float iris_FogCloudsEnd"),
	LIGHTING("Lighting",
		"vec3 iris_Light0_Direction",
		"vec3 iris_Light1_Direction"),
	CLOUD_INFO("CloudInfo",
		"vec4 iris_CloudColor",
		"vec3 iris_CloudOffset",
		"vec3 iris_CloudCellSize");

	/**
	 * The blocks a shader pack's programs read. They get fog from Iris's own uniforms instead.
	 */
	private static final List<VanillaUniformBlocks> PACK_BLOCKS = List.of(DYNAMIC_TRANSFORMS, PROJECTION, GLOBALS);
	/**
	 * The blocks the programs synthesized for packs that lack one read.
	 */
	public static final List<VanillaUniformBlocks> FALLBACK_BLOCKS = List.of(DYNAMIC_TRANSFORMS, PROJECTION, GLOBALS, FOG, LIGHTING);

	private final String vanillaName;
	private final String[] members;
	private final String declaration;

	VanillaUniformBlocks(String vanillaName, String... members) {
		this.vanillaName = vanillaName;
		this.members = members;
		this.declaration = "layout(std140) uniform " + blockName(vanillaName) + " { " + String.join("; ", members) + "; };";
	}

	private static String blockName(String vanillaName) {
		return "iris_" + vanillaName;
	}

	public String getVanillaName() {
		return vanillaName;
	}

	public String getBlockName() {
		return blockName(vanillaName);
	}

	public String getDeclaration() {
		return declaration;
	}

	/**
	 * Replaces the plain uniforms the transformer declared for vanilla's values with vanilla's blocks, and computes the
	 * matrices vanilla doesn't provide at the start of {@code main}.
	 */
	public static void declareForPack(Transformer translationUnit, ShaderType type) {
		for (VanillaUniformBlocks block : PACK_BLOCKS) {
			for (String member : block.members) {
				translationUnit.removeVariable(member.substring(member.indexOf(' ') + 1));
			}

			translationUnit.injectVariable(block.declaration);
		}

		derive(translationUnit, "mat4", "iris_ModelViewMatInverse", "inverse(iris_ModelViewMat)");
		derive(translationUnit, "mat4", "iris_ProjMatInverse", "inverse(iris_ProjMat)");
		derive(translationUnit, "mat3", "iris_NormalMat", "transpose(inverse(mat3(iris_ModelViewMat)))");

		// Line width is a vertex attribute on 26.1.
		if (type == ShaderType.VERTEX && translationUnit.hasVariable("iris_LineWidth")) {
			translationUnit.removeVariable("iris_LineWidth");
			translationUnit.injectVariable("in float iris_LineWidth;");
		}
	}

	private static void derive(Transformer translationUnit, String type, String name, String value) {
		translationUnit.removeVariable(name);

		if (translationUnit.containsCall(name)) {
			translationUnit.injectVariable(type + " " + name + ";");
			translationUnit.prependMain(name + " = " + value + ";");
		}
	}
}
