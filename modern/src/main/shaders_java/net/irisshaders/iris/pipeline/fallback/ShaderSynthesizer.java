package net.irisshaders.iris.pipeline.fallback;

import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;

public class ShaderSynthesizer {
	public static String vsh(boolean hasChunkOffset, ShaderAttributeInputs inputs, FogMode fogMode,
							 boolean entityLighting, boolean isLeash) {
		StringBuilder shader = new StringBuilder();
		StringBuilder main = new StringBuilder();

		shader.append("#version 150 core\n");

		// Vertex Position
		shader.append("uniform mat4 iris_ModelViewMat;\n");
		shader.append("uniform mat4 iris_ProjMat;\n");
		shader.append("in vec3 iris_Position;\n");

		String position;

		if (hasChunkOffset) {
			shader.append("uniform vec3 iris_ChunkOffset;\n");
			position = "iris_Position + iris_ChunkOffset";
		} else {
			position = "iris_Position";
		}

		if (inputs.isNewLines()) {
			shader.append("const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);\n" +
				"const mat4 VIEW_SCALE = mat4(\n" +
				"    VIEW_SHRINK, 0.0, 0.0, 0.0,\n" +
				"    0.0, VIEW_SHRINK, 0.0, 0.0,\n" +
				"    0.0, 0.0, VIEW_SHRINK, 0.0,\n" +
				"    0.0, 0.0, 0.0, 1.0\n" +
				");\n");

			shader.append("uniform float iris_LineWidth;\n" +
				"uniform vec2 iris_ScreenSize;\n");

			main.append("vec4 linePosStart = iris_ProjMat * VIEW_SCALE * iris_ModelViewMat * vec4(" + position + ", 1.0);\n" +
				"    vec4 linePosEnd = iris_ProjMat * VIEW_SCALE * iris_ModelViewMat * vec4(" + position + " + iris_Normal, 1.0);\n" +
				"\n" +
				"    vec3 ndc1 = linePosStart.xyz / linePosStart.w;\n" +
				"    vec3 ndc2 = linePosEnd.xyz / linePosEnd.w;\n" +
				"\n" +
				"    vec2 lineScreenDirection = normalize((ndc2.xy - ndc1.xy) * iris_ScreenSize);\n" +
				"    vec2 lineOffset = vec2(-lineScreenDirection.y, lineScreenDirection.x) * iris_LineWidth / iris_ScreenSize;\n" +
				"\n" +
				"    if (lineOffset.x < 0.0) {\n" +
				"        lineOffset *= -1.0;\n" +
				"    }\n" +
				"\n" +
				"    if (gl_VertexID % 2 == 0) {\n" +
				"        gl_Position = vec4((ndc1 + vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);\n" +
				"    } else {\n" +
				"        gl_Position = vec4((ndc1 - vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);\n" +
				"    }\n");
		} else {
			main.append("    gl_Position = iris_ProjMat * iris_ModelViewMat * vec4(");
			main.append(position);
			main.append(", 1.0);\n");
		}

		// Vertex Color
		if (isLeash) {
			shader.append("flat ");
		}
		shader.append("out vec4 iris_vertexColor;\n");
		shader.append("uniform vec4 iris_ColorModulator;\n");

		// Vertex Normal
		if (inputs.hasNormal() && inputs.hasColor()) {
			shader.append("in vec4 iris_Color;\n");

			// TODO: Entity lighting without color? Only a theoretical possibility since all
			//       entity shaders use vertex color.
			if (entityLighting) {
				shader.append("uniform vec3 iris_Light0_Direction;\n");
				shader.append("uniform vec3 iris_Light1_Direction;\n");

				// Copied from Mojang code.
				shader.append("vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {\n" +
					"    lightDir0 = normalize(lightDir0);\n" +
					"    lightDir1 = normalize(lightDir1);\n" +
					"    float light0 = max(0.0, dot(lightDir0, normal));\n" +
					"    float light1 = max(0.0, dot(lightDir1, normal));\n" +
					"    float lightAccum = min(1.0, (light0 + light1) * 0.6 + 0.4);\n" +
					"    return vec4(color.rgb * lightAccum, color.a);\n" +
					"}\n");

				shader.append("in vec3 iris_Normal;\n");

				// minecraft_mix_light just passes through the original alpha value, so it's safe here.
				main.append("    iris_vertexColor = minecraft_mix_light(iris_Light0_Direction, iris_Light1_Direction, iris_Normal, iris_Color * iris_ColorModulator);\n");
			} else if (inputs.isNewLines()) {
				shader.append("in vec3 iris_Normal;\n");
				main.append("    iris_vertexColor = iris_Color * iris_ColorModulator;\n");
			} else {
				main.append("    iris_vertexColor = iris_Color * iris_ColorModulator;\n");
			}
		} else if (inputs.hasColor()) {
			shader.append("in vec4 iris_Color;\n");

			main.append("    iris_vertexColor = iris_Color * iris_ColorModulator;\n");
		} else {
			main.append("    iris_vertexColor = iris_ColorModulator;\n");
		}

		// Overlay Color
		if (inputs.hasOverlay()) {
			shader.append("uniform sampler2D overlay;\n");
			shader.append("in ivec2 iris_UV1;\n");
			shader.append("out vec4 overlayColor;\n");

			main.append("    overlayColor = texelFetch(overlay, iris_UV1, 0);\n");
		}

		// Vertex Texture
		if (inputs.hasTex()) {
			shader.append("in vec2 iris_UV0;\n");
			shader.append("out vec2 texCoord;\n");

			main.append("    texCoord = iris_UV0;\n");
		}

		// Fog
		if (fogMode == FogMode.PER_VERTEX) {
			shader.append("out float vertexDistance;\n");

			main.append("    vertexDistance = length((iris_ModelViewMat * vec4(");
			main.append(position);
			main.append(", 1.0)).xyz);\n");
		}

		// Vertex Light
		if (inputs.hasLight()) {
			shader.append("in ivec2 iris_UV2;\n");
			shader.append("out vec2 lightCoord;\n");

			main.append("    lightCoord = clamp(iris_UV2 / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0));\n");
		}


		// void main
		shader.append("void main() {\n");
		shader.append(main);
		shader.append("}\n");

		return shader.toString();
	}

	public static String fsh(ShaderAttributeInputs inputs, FogMode fogMode, AlphaTest alphaTest, boolean intensityTex, boolean isLeash) {
		StringBuilder shader = new StringBuilder();
		StringBuilder main = new StringBuilder();

		shader.append("#version 150 core\n");

		shader.append("out vec4 fragColor;\n");
		shader.append("uniform float iris_AlphaTestValue;\n");
		if (isLeash) {
			shader.append("flat ");
		}
		shader.append("in vec4 iris_vertexColor;\n");

		main.append("float iris_vertexColorAlpha = iris_vertexColor.a;");

		if (inputs.hasTex()) {
			shader.append("uniform sampler2D gtexture;\n");
			shader.append("in vec2 texCoord;\n");

			main.append("    vec4 color = texture(gtexture, texCoord)");

			if (intensityTex) {
				main.append(".rrrr");
			}

			if (alphaTest == AlphaTests.VERTEX_ALPHA) {
				main.append(" * vec4(iris_vertexColor.rgb, 1);\n");
			} else {
				main.append(" * iris_vertexColor;\n");
			}
		} else {
			if (alphaTest == AlphaTests.VERTEX_ALPHA) {
				main.append("vec4 color = vec4(iris_vertexColor.rgb, 1);\n");
			} else {
				main.append("vec4 color = iris_vertexColor;\n");
			}
		}

		if (inputs.hasOverlay()) {
			shader.append("in vec4 overlayColor;\n");

			main.append("    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);\n");
		}

		if (inputs.hasLight()) {
			shader.append("uniform sampler2D lightmap;\n");
			shader.append("in vec2 lightCoord;\n");

			main.append("    color *= texture(lightmap, lightCoord);\n");
		}

		if (fogMode == FogMode.PER_VERTEX || fogMode == FogMode.PER_FRAGMENT) {
			shader.append("uniform vec4 iris_FogColor;\n");
			shader.append("uniform float iris_FogStart;\n");
			shader.append("uniform float iris_FogEnd;\n");

			if (fogMode == FogMode.PER_VERTEX) {
				// Use vertex distances, close enough
				shader.append("in float vertexDistance;\n");
				main.append("float fragmentDistance = vertexDistance;\n");
			} else /*if (fogMode == FogMode.PER_FRAGMENT)*/ {
				// Use fragment distances since beam vertices are very far apart
				shader.append("uniform mat4 iris_ProjMat;\n");
				main.append("float fragmentDistance = -iris_ProjMat[3].z / ((gl_FragCoord.z) * -2.0 + 1.0 - iris_ProjMat[2].z);\n");
			}

			// These are custom Iris uniforms, set in ShaderCreator.createFallback.
			shader.append("uniform float iris_FogDensity = 1.0;\n");
			shader.append("uniform int iris_FogIsExp2 = 1;\n");

			main.append("    float fogFactor;\n");
			main.append("    if (iris_FogIsExp2 == 1) {\n");
			main.append("        float x = fragmentDistance * iris_FogDensity;\n");
			main.append("        fogFactor = exp(-x * x);\n");
			main.append("    } else {\n");
			main.append("        fogFactor = (iris_FogEnd - fragmentDistance) / (iris_FogEnd - iris_FogStart);\n");
			main.append("    }\n");

			main.append("    fogFactor = clamp(fogFactor, 0.0, 1.0);\n");

			main.append("    color.rgb = mix(iris_FogColor.rgb, color.rgb, fogFactor * iris_FogColor.a);\n");
		}

		main.append("    fragColor = color;\n");

		// void main
		shader.append("void main() {\n");
		shader.append(main);
		shader.append(alphaTest.toExpression("fragColor.a", "iris_AlphaTestValue", "    "));
		shader.append("}\n");

		return shader.toString();
	}
}
