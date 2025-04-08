package net.irisshaders.iris.pipeline.foss_transform;

import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.taumc.glsl.Transformer;

import static net.irisshaders.iris.pipeline.foss_transform.ShaderTransformer.*;

public class CompositeTransformer {
    private static void patchCompositeDepth(Transformer translationUnit, Parameters parameters) {
        int type = translationUnit.findType("centerDepthSmooth");
        if (type != 0) {
            translationUnit.removeVariable("centerDepthSmooth");

            translationUnit.injectFunction("uniform sampler2D iris_centerDepthSmooth;");

            translationUnit.replaceExpression("centerDepthSmooth", "texture(iris_centerDepthSmooth, vec2(0.5)).r");
        }
    }

    public static void patchCompositeCore(Transformer translationUnit, Parameters parameters) {
        patchCompositeDepth(translationUnit, parameters);

        if (parameters.type == PatchShaderType.VERTEX) {
            translationUnit.rename("vaPosition", "Position");
            translationUnit.rename("vaUV0", "UV0");
            translationUnit.replaceExpression("modelViewMatrix", "mat4(1.0)");
            // This is used to scale the quad projection matrix from (0, 1) to (-1, 1).
            translationUnit.replaceExpression("projectionMatrix",
                    "mat4(vec4(2.0, 0.0, 0.0, 0.0), vec4(0.0, 2.0, 0.0, 0.0), vec4(0.0), vec4(-1.0, -1.0, 0.0, 1.0))");
            translationUnit.replaceExpression("modelViewMatrixInverse", "mat4(1.0)");
            translationUnit.replaceExpression("projectionMatrixInverse",
                    "inverse(mat4(vec4(2.0, 0.0, 0.0, 0.0), vec4(0.0, 2.0, 0.0, 0.0), vec4(0.0), vec4(-1.0, -1.0, 0.0, 1.0)))");
            translationUnit.replaceExpression("textureMatrix", "mat4(1.0)");
        }
    }

    public static void patchComposite(Transformer translationUnit, Parameters parameters) {
        commonPatch(translationUnit, parameters, false);
        patchCompositeDepth(translationUnit, parameters);

        for (int i = 0; i < 8; i++) {
            translationUnit.replaceExpression("gl_TextureMatrix[" + i + "]", "mat4(1.0f)");
        }

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            translationUnit.replaceExpression("gl_MultiTexCoord0","vec4(UV0, 0.0, 1.0)");
            translationUnit.injectVariable("in vec2 UV0;");

            replaceGlMultiTexCoordBounded(translationUnit, 1, 7);
        }

        // No color attributes, the color is always solid white.
        translationUnit.replaceExpression("gl_Color", "vec4(1.0, 1.0, 1.0, 1.0)");

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            // https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glNormal.xml
            // The initial value of the current normal is the unit vector, (0, 0, 1).
            translationUnit.replaceExpression("gl_Normal", "vec3(0.0, 0.0, 1.0)");
        }

        translationUnit.replaceExpression("gl_NormalMatrix", "mat3(1.0)");

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            translationUnit.injectVariable("in vec3 Position;");
            if (translationUnit.containsCall("ftransform")) {
                translationUnit.injectFunction(
                        "vec4 ftransform() { return gl_ModelViewProjectionMatrix * gl_Vertex; }");
            }
            translationUnit.replaceExpression("gl_Vertex", "vec4(Position, 1.0)");
        }

        // TODO: All of the transformed variants of the input matrices, preferably
        // computed on the CPU side...
        translationUnit.replaceExpression("gl_ModelViewProjectionMatrix",
                "(gl_ProjectionMatrix * gl_ModelViewMatrix)");
        translationUnit.replaceExpression("gl_ModelViewMatrix", "mat4(1.0)");

        // This is used to scale the quad projection matrix from (0, 1) to (-1, 1).
        translationUnit.replaceExpression("gl_ProjectionMatrix",
                "mat4(vec4(2.0, 0.0, 0.0, 0.0), vec4(0.0, 2.0, 0.0, 0.0), vec4(0.0), vec4(-1.0, -1.0, 0.0, 1.0))");

        applyIntelHd4000Workaround(translationUnit);
    }

}
