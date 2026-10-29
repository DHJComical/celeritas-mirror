package net.irisshaders.iris.pipeline.foss_transform;

import org.taumc.glsl.Transformer;

/**
 * Generates the vertices of 26.1's clouds in a shader pack's vertex shader. Vanilla draws clouds without vertex data,
 * building each vertex from {@code gl_VertexID}, the cloud cells in the {@code CloudFaces} texel buffer and the
 * {@code CloudInfo} block, as its {@code rendertype_clouds} shader does. This feeds the results to the pack's shader in
 * place of the position, color, normal and texture coordinate attributes.
 */
public final class CloudVertices {
	/**
	 * The name of the texel buffer sampler, which must be bound to vanilla's cloud faces buffer.
	 */
	public static final String CLOUD_FACES_SAMPLER = "iris_CloudFaces";

	private CloudVertices() {
	}

	public static void patch(Transformer translationUnit) {
		String[][] attributes = {
			{"iris_Position", "iris_cloudPosition"},
			{"iris_Color", "iris_cloudColor"},
			{"iris_Normal", "iris_cloudNormal"},
			{"iris_UV0", "iris_cloudUV"},
		};

		for (String[] attribute : attributes) {
			translationUnit.removeVariable(attribute[0]);
			translationUnit.rename(attribute[0], attribute[1]);
		}

		translationUnit.injectVariable(VanillaUniformBlocks.CLOUD_INFO.getDeclaration());
		translationUnit.injectVariable("uniform isamplerBuffer " + CLOUD_FACES_SAMPLER + ";");
		translationUnit.injectVariable("vec3 iris_cloudPosition;");
		translationUnit.injectVariable("vec4 iris_cloudColor;");
		translationUnit.injectVariable("vec3 iris_cloudNormal;");
		translationUnit.injectVariable("vec2 iris_cloudUV = vec2(0.0);");
		translationUnit.injectFunction("""
			void iris_generateCloudVertex() {
			    const vec3[] vertices = vec3[](
			        vec3(1, 0, 0), vec3(1, 0, 1), vec3(0, 0, 1), vec3(0, 0, 0),
			        vec3(0, 1, 0), vec3(0, 1, 1), vec3(1, 1, 1), vec3(1, 1, 0),
			        vec3(0, 0, 0), vec3(0, 1, 0), vec3(1, 1, 0), vec3(1, 0, 0),
			        vec3(1, 0, 1), vec3(1, 1, 1), vec3(0, 1, 1), vec3(0, 0, 1),
			        vec3(0, 0, 1), vec3(0, 1, 1), vec3(0, 1, 0), vec3(0, 0, 0),
			        vec3(1, 0, 0), vec3(1, 1, 0), vec3(1, 1, 1), vec3(1, 0, 1)
			    );
			    const vec3[] normals = vec3[](
			        vec3(0, -1, 0), vec3(0, 1, 0), vec3(0, 0, -1), vec3(0, 0, 1), vec3(-1, 0, 0), vec3(1, 0, 0)
			    );
			    const vec4[] faceColors = vec4[](
			        vec4(0.7, 0.7, 0.7, 1.0), vec4(1.0, 1.0, 1.0, 1.0), vec4(0.8, 0.8, 0.8, 1.0),
			        vec4(0.8, 0.8, 0.8, 1.0), vec4(0.9, 0.9, 0.9, 1.0), vec4(0.9, 0.9, 0.9, 1.0)
			    );

			    int quadVertex = gl_VertexID % 4;
			    int index = (gl_VertexID / 4) * 3;
			    int cellX = texelFetch(iris_CloudFaces, index).r;
			    int cellZ = texelFetch(iris_CloudFaces, index + 1).r;
			    int dirAndFlags = texelFetch(iris_CloudFaces, index + 2).r;
			    int direction = dirAndFlags & 7;
			    bool isInsideFace = (dirAndFlags & (1 << 4)) != 0;
			    bool useTopColor = (dirAndFlags & (1 << 5)) != 0;
			    cellX = (cellX << 1) | ((dirAndFlags & (1 << 7)) >> 7);
			    cellZ = (cellZ << 1) | ((dirAndFlags & (1 << 6)) >> 6);

			    vec3 faceVertex = vertices[(direction * 4) + (isInsideFace ? 3 - quadVertex : quadVertex)];
			    iris_cloudPosition = (faceVertex + vec3(cellX, 0, cellZ)) * iris_CloudCellSize + iris_CloudOffset;
			    iris_cloudNormal = isInsideFace ? -normals[direction] : normals[direction];
			    iris_cloudColor = (useTopColor ? faceColors[1] : faceColors[direction]) * iris_CloudColor;
			}""");
		translationUnit.prependMain("iris_generateCloudVertex();");
	}
}
