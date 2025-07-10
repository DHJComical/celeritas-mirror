package net.irisshaders.iris.pipeline.foss_transform;

import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.taumc.glsl.Transformer;

import static net.irisshaders.iris.pipeline.foss_transform.ShaderTransformer.*;

public class SodiumTransformer {
    public static final int MID_TEXTURE_MAX_VALUE = 32768;
    public static final float MID_TEX_SCALE = 1.0f / MID_TEXTURE_MAX_VALUE;

    public static void patchSodiumCore(Transformer translationUnit, SodiumParameters parameters) {
        translationUnit.rename("alphaTestRef", "iris_currentAlphaTest");
        translationUnit.rename("modelViewMatrix", "iris_ModelViewMatrix");
        translationUnit.rename("modelViewMatrixInverse", "iris_ModelViewMatrixInverse");
        translationUnit.rename("projectionMatrix", "iris_ProjectionMatrix");
        translationUnit.rename("projectionMatrixInverse", "iris_ProjectionMatrixInverse");
        translationUnit.rename("normalMatrix", "iris_NormalMatrix");
        translationUnit.rename("chunkOffset", "u_RegionOffset");
        translationUnit.injectVariable("uniform mat4 iris_LightmapTextureMatrix;");

        if (parameters.type == ShaderType.VERTEX) {
            // _draw_translation replaced with Chunks[_draw_id].offset.xyz
            translationUnit.replaceExpression("vaPosition", "_vert_position + _get_draw_translation(_draw_id)");
            translationUnit.replaceExpression("vaColor", "_vert_color");
            translationUnit.rename("vaNormal", "iris_Normal");
            translationUnit.replaceExpression("vaUV0", "_vert_tex_diffuse_coord");
            translationUnit.replaceExpression("vaUV1", "ivec2(0, 10)");
            translationUnit.rename("vaUV2", "_vert_tex_light_coord");

            translationUnit.replaceExpression("textureMatrix", "mat4(1.0f)");
            replaceMidTexCoord(translationUnit, MID_TEX_SCALE);

            injectVertInit(translationUnit, parameters);
        }
    }

    public static void patchSodium(Transformer translationUnit, SodiumParameters parameters) {
        commonPatch(translationUnit, parameters, false);

        replaceMidTexCoord(translationUnit, MID_TEX_SCALE);

        translationUnit.replaceExpression("gl_TextureMatrix[0]", "mat4(1.0f)");
        translationUnit.replaceExpression("gl_TextureMatrix[1]", "iris_LightmapTextureMatrix");
        translationUnit.injectFunction("uniform mat4 iris_LightmapTextureMatrix;");
        translationUnit.rename("gl_ProjectionMatrix", "iris_ProjectionMatrix");

        if (parameters.type == ShaderType.VERTEX) {

            translationUnit.rename("gl_MultiTexCoord2", "gl_MultiTexCoord1");
            translationUnit.replaceExpression("gl_MultiTexCoord0", "vec4(_vert_tex_diffuse_coord, 0.0f, 1.0f)");
            translationUnit.replaceExpression("gl_MultiTexCoord1", "vec4(_vert_tex_light_coord, 0.0f, 1.0f)");

            patchMultiTexCoord3(translationUnit, parameters);

            // gl_MultiTexCoord0 and gl_MultiTexCoord1 are the only valid inputs (with
            // gl_MultiTexCoord2 and gl_MultiTexCoord3 as aliases), other texture
            // coordinates are not valid inputs.
            replaceGlMultiTexCoordBounded(translationUnit, 4, 7);
        }

        translationUnit.rename("gl_Color", "_vert_color");

        if (parameters.type == ShaderType.VERTEX) {
            translationUnit.rename("gl_Normal", "iris_Normal");
            translationUnit.injectVariable("in vec3 iris_Normal;");
        }

        // TODO: Should probably add the normal matrix as a proper uniform that's
        // computed on the CPU-side of things
        translationUnit.replaceExpression("gl_NormalMatrix",
                "iris_NormalMatrix");
        translationUnit.injectVariable("uniform mat3 iris_NormalMatrix;");

        translationUnit.injectVariable("uniform mat4 iris_ModelViewMatrixInverse;");

        translationUnit.injectVariable("uniform mat4 iris_ProjectionMatrixInverse;");

        // TODO: All of the transformed variants of the input matrices, preferably
        // computed on the CPU side...
        translationUnit.rename("gl_ModelViewMatrix", "iris_ModelViewMatrix");
        translationUnit.rename("gl_ModelViewMatrixInverse", "iris_ModelViewMatrixInverse");
        translationUnit.rename("gl_ProjectionMatrixInverse", "iris_ProjectionMatrixInverse");

        if (parameters.type == ShaderType.VERTEX) {
            // TODO: Vaporwave-Shaderpack expects that vertex positions will be aligned to
            // chunks.
            if (translationUnit.containsCall("ftransform")) {
                translationUnit.injectFunction("vec4 ftransform() { return gl_ModelViewProjectionMatrix * gl_Vertex; }");
            }
            translationUnit.injectFunction(
                    "uniform mat4 iris_ProjectionMatrix;");
            translationUnit.injectFunction(
                    "uniform mat4 iris_ModelViewMatrix;");
            translationUnit.injectFunction(
                    "uniform vec3 u_RegionOffset;");
            translationUnit.injectFunction(
                    // _draw_translation replaced with Chunks[_draw_id].offset.xyz
                    "vec4 getVertexPosition() { return vec4(_vert_position + u_RegionOffset + _get_draw_translation(_draw_id), 1.0f); }");
            translationUnit.replaceExpression("gl_Vertex", "getVertexPosition()");

            // inject here so that _vert_position is available to the above. (injections
            // inject in reverse order if performed piece-wise but in correct order if
            // performed as an array of injections)
            injectVertInit(translationUnit, parameters);
        } else {
            translationUnit.injectVariable(
                    "uniform mat4 iris_ModelViewMatrix;");
            translationUnit.injectFunction(
                    "uniform mat4 iris_ProjectionMatrix;");
        }

        translationUnit.replaceExpression("gl_ModelViewProjectionMatrix",
                "(iris_ProjectionMatrix * iris_ModelViewMatrix)");

        applyIntelHd4000Workaround(translationUnit);
    }

    private static void injectVertInit(Transformer translationUnit, SodiumParameters parameters) {
        // Rest of the code is injected in ShaderTransformer#computeCeleritasHeader
        translationUnit.prependMain("_vert_init();");
    }
}
