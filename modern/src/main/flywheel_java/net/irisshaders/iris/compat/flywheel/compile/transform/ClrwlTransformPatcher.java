package net.irisshaders.iris.compat.flywheel.compile.transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.foss_transform.ShaderTransformer;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.taumc.glsl.ShaderParser;
import org.taumc.glsl.ShaderPrinter;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlTransformPatcher
{
	public static final Pattern versionPattern = Pattern.compile("^.*#version\\h+(\\d+)\\V*", Pattern.DOTALL);
	public static final Pattern extensionPattern = Pattern.compile("^.*#extension\\s+([a-zA-Z0-9_]+)\\s+:\\s+([a-zA-Z0-9_]+)", Pattern.DOTALL);

	private static final Pattern layoutLocationPattern = Pattern.compile("location=(\\d+)");
	private static final Pattern outColorPattern = Pattern.compile("outColor(\\d+)");

	private static final String LIGHTMAP_SCALE = "1.0 / 240.0";
	private static final String LIGHTMAP_OFFSET = "0.03125";
	private static final String LIGHTMAP_MATRIX = String.format("mat4(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
				LIGHTMAP_SCALE, "0.0",			  "0.0", "0.0",
				"0.0", 			LIGHTMAP_SCALE,   "0.0", "0.0",
				"0.0", 			"0.0", 			  "1.0", "0.0",
				LIGHTMAP_OFFSET, LIGHTMAP_OFFSET, "0.0", "1.0"
			);

	/**
	 * Parses a pack shader with its {@code #version} and {@code #extension} directives removed; Flywheel's
	 * compilation supplies its own.
	 */
	static Transformer parse(String input, int version)
	{
		input = versionPattern.matcher(input).replaceAll("");
		input = extensionPattern.matcher(input).replaceAll("");
		input = ShaderTransformer.renameReservedWords(input, version);

		return new Transformer(ShaderParser.parseShader(input).full());
	}

	static String print(Transformer transformer)
	{
		StringBuilder out = new StringBuilder();
		transformer.mutateTree(tree -> out.append(ShaderPrinter.getFormattedShader(tree)));
		return out.toString();
	}

	static void replaceMatrices(Transformer t)
	{
		t.replaceExpression("gl_ModelViewMatrix", "flw_view");
		t.replaceExpression("modelViewMatrix", "flw_view");
		t.replaceExpression("gl_ModelViewMatrixInverse", "flw_viewInverse");
		t.replaceExpression("modelViewMatrixInverse", "flw_viewInverse");

		t.replaceExpression("gl_ProjectionMatrix", "flw_projection");
		t.replaceExpression("projectionMatrix", "flw_projection");
		t.replaceExpression("gl_ProjectionMatrixInverse", "flw_projectionInverse");
		t.replaceExpression("projectionMatrixInverse", "flw_projectionInverse");

		t.replaceExpression("gl_ModelViewProjectionMatrix", "flw_viewProjection");
		t.replaceExpression("modelViewProjectionMatrix", "flw_viewProjection");
		t.replaceExpression("gl_ModelViewProjectionMatrixInverse", "flw_viewProjectionInverse");
		t.replaceExpression("modelViewProjectionMatrixInverse", "flw_viewProjectionInverse");

		t.replaceExpression("gl_TextureMatrix[0]", "mat4(1.0)");
		t.replaceExpression("textureMatrix", "mat4(1.0)");
		t.replaceExpression("gl_TextureMatrix[1]", LIGHTMAP_MATRIX);
		t.replaceExpression("gl_TextureMatrix[2]", LIGHTMAP_MATRIX);

		t.replaceExpression("gl_NormalMatrix", "clrwl_normal");
		t.replaceExpression("normalMatrix", "clrwl_normal");

		t.replaceExpression("clrwl_view", "flw_view");
		t.replaceExpression("clrwl_viewInverse", "flw_viewInverse");
		t.replaceExpression("clrwl_viewPrev", "flw_viewPrev");
		t.replaceExpression("clrwl_projection", "flw_projection");
		t.replaceExpression("clrwl_projectionInverse", "flw_projectionInverse");
		t.replaceExpression("clrwl_projectionPrev", "flw_projectionPrev");
		t.replaceExpression("clrwl_viewProjection", "flw_viewProjection");
		t.replaceExpression("clrwl_viewProjectionInverse", "flw_viewProjectionInverse");
		t.replaceExpression("clrwl_viewProjectionPrev", "flw_viewProjectionPrev");
		t.replaceExpression("clrwl_renderOrigin", "flw_renderOrigin");
		t.replaceExpression("clrwl_cameraPos", "flw_cameraPos");
	}

	static void finish(Transformer t, ClrwlTransformParameters parameters)
	{
		ShaderTransformer.commonPatch(t, parameters, false);

		String diffuse = parameters.isCrumbling() ? "_flw_crumblingTex" : "flw_diffuseTex";
		t.replaceExpression("tex", diffuse);
		t.replaceExpression("gtexture", diffuse);
		t.replaceExpression("iris_renamed_texture", diffuse);

		t.rename("main", "_clrwl_shader_main");
	}

	private static ClrwlTransformOutput transform(String source, ClrwlTransformParameters parameters)
	{
		Matcher versionMatcher = versionPattern.matcher(source);

		if (!versionMatcher.find())
		{
			throw new IllegalArgumentException("No #version directive found in source code!");
		}

		Transformer t = parse(source, Integer.parseInt(versionMatcher.group(1)));
		Map<Integer, String> outputs = new HashMap<>();

		t.replaceExpression("gl_Vertex", "flw_vertexPos");
		t.replaceExpression("gl_Normal", "flw_vertexNormal");
		t.replaceExpression("gl_Color", "flw_vertexColor");
		replaceVec4WithVariableDimension(t, "at_midBlock", "clrwl_vertexMidMesh");
		replaceVec4WithVariableDimension(t, "at_tangent", "clrwl_vertexTangent");
		replaceVec2WithVariableDimension(t, "mc_midTexCoord", "clrwl_vertexMidTexCoord");
		replaceVec2WithVariableDimension(t, "mc_Entity", "clrwl_vertexEntity");

		t.replaceExpression("gl_MultiTexCoord0", "vec4(flw_vertexTexCoord, 0.0, 1.0)");
		t.replaceExpression("gl_MultiTexCoord1", "vec4(flw_vertexLight * 240.0, 0.0, 1.0)");
		t.replaceExpression("gl_MultiTexCoord2", "vec4(flw_vertexLight * 240.0, 0.0, 1.0)");

		t.rename("renderStage", "_clrwl_renderPhase");
		t.rename("blendFunc", "_clrwl_blendFunc");
		t.rename("atlasSize", "_clrwl_atlasSize");
		t.replaceExpression("blockEntityId", "_clrwl_blockEntityId");
		t.replaceExpression("entityId", "_clrwl_entityId");
		t.replaceExpression("entityColor", "clrwl_overlayColor");

		replaceMatrices(t);

		t.rename("clrwl_vertexPos", "flw_vertexPos");
		t.rename("clrwl_vertexColor", "flw_vertexColor");
		t.rename("clrwl_vertexTexCoord", "flw_vertexTexCoord");
		t.rename("clrwl_vertexOverlay", "flw_vertexOverlay");
		t.rename("clrwl_vertexLight", "flw_vertexLight");
		t.rename("clrwl_vertexNormal", "flw_vertexNormal");
		t.rename("clrwl_distance", "flw_distance");

		t.replaceExpression("clrwl_materialFragment", "_clrwl_materialFragment_hook");
		t.replaceExpression("clrwl_shaderLight", "_clrwl_shaderLight_hook");

		if (parameters.type == ShaderType.FRAGMENT)
		{
			patchFragmentOutputs(t, parameters.usesCustomOutputs(), outputs);
		}

		finish(t, parameters);

		return new ClrwlTransformOutput(print(t), outputs);
	}

	/**
	 * Collects the pack's fragment outputs by location. With custom outputs, Colorwheel writes the attachments
	 * itself, so the pack's outputs become plain globals.
	 */
	private static void patchFragmentOutputs(Transformer t, boolean customOutputs, Map<Integer, String> outputs)
	{
		if (t.containsCall("gl_FragColor"))
		{
			t.replaceExpression("gl_FragColor", "gl_FragData[0]", GLSLParser::unary_expression);
		}

		Map<String, Integer> declaredOutputs = new HashMap<>();

		for (var entry : t.findQualifiers(GLSLLexer.OUT).entrySet())
		{
			var declaration = entry.getValue();

			if (!declaration.fully_specified_type().type_specifier().getText().equals("vec4"))
			{
				continue;
			}

			Matcher location = layoutLocationPattern.matcher(declaration.getText());
			Matcher outColor = outColorPattern.matcher(entry.getKey());

			if (location.find())
			{
				declaredOutputs.put(entry.getKey(), Integer.parseInt(location.group(1)));
			}
			else if (outColor.matches())
			{
				declaredOutputs.put(entry.getKey(), Integer.parseInt(outColor.group(1)));
			}
		}

		String fragDataPrefix = customOutputs ? "clrwl_FragData" : "iris_FragData";
		Set<Integer> fragDataIndexes = new HashSet<>();
		t.renameArray("gl_FragData", fragDataPrefix, fragDataIndexes);

		for (int index : fragDataIndexes)
		{
			outputs.putIfAbsent(index, fragDataPrefix + index);

			if (customOutputs)
			{
				t.injectVariable("vec4 " + fragDataPrefix + index + ";");
			}
			else
			{
				t.injectFunction("layout (location = " + index + ") out vec4 " + fragDataPrefix + index + ";");
			}
		}

		declaredOutputs.forEach((name, index) ->
		{
			outputs.putIfAbsent(index, name);

			if (customOutputs)
			{
				t.removeVariable(name);
				t.injectVariable("vec4 " + name + ";");
			}
		});
	}

	private static int getAttributeDimensionAndDelete(Transformer t, String name, int defaultDimension)
	{
		int type = t.findType(name);

		if (type == 0)
		{
			return defaultDimension;
		}

		t.removeVariable(name);

		return switch (type)
		{
			case GLSLLexer.FLOAT -> 1;
			case GLSLLexer.VEC2 -> 2;
			case GLSLLexer.VEC3 -> 3;
			case GLSLLexer.VEC4 -> 4;
			default -> defaultDimension;
		};
	}

	private static void replaceVec2WithVariableDimension(Transformer t, String name, String vec2)
	{
		int dimension = getAttributeDimensionAndDelete(t, name, 2);

		if (dimension == 1)
		{
			t.replaceExpression(name, vec2 + ".x");
		}
		else if (dimension == 2)
		{
			t.replaceExpression(name, vec2);
		}
		else if (dimension == 3)
		{
			t.replaceExpression(name, "vec3(" + vec2 + ", 0.0)");
		}
		else
		{
			t.replaceExpression(name, "vec4(" + vec2 + ", 0.0, 1.0)");
		}
	}

	private static void replaceVec4WithVariableDimension(Transformer t, String name, String vec4)
	{
		int dimension = getAttributeDimensionAndDelete(t, name, 4);

		if (dimension == 1)
		{
			t.replaceExpression(name, vec4 + ".x");
		}
		else if (dimension == 2)
		{
			t.replaceExpression(name, vec4 + ".xy");
		}
		else if (dimension == 3)
		{
			t.replaceExpression(name, vec4 + ".xyz");
		}
		else
		{
			t.replaceExpression(name, vec4);
		}
	}

	public static String patchVertex(String vertex, boolean isCrumbling, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		return transform(vertex, new ClrwlTransformParameters(ShaderType.VERTEX, isCrumbling, false, textureMap)).code();
	}

	public static String patchGeometry(String geometry, boolean isCrumbling, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		return transform(geometry, new ClrwlTransformParameters(ShaderType.GEOM, isCrumbling, false, textureMap)).code();
	}

	public static ClrwlTransformOutput patchFragment(String fragment, boolean isCrumbling, boolean customOutputs, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		return transform(fragment, new ClrwlTransformParameters(ShaderType.FRAGMENT, isCrumbling, customOutputs, textureMap));
	}
}
