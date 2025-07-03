package net.irisshaders.iris.pipeline.foss_transform;

import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import org.taumc.glsl.Transformer;

import static net.irisshaders.iris.pipeline.foss_transform.ShaderTransformer.addIfNotExistsType;
import static net.irisshaders.iris.pipeline.foss_transform.ShaderTransformer.upgradeStorageQualifiers;

public class VanillaTransformer {
    public static void patchVanilla(Transformer translationUnit, VanillaParameters parameters) {
        // this happens before common to make sure the renaming of attributes is done on
        // attribute inserted by this
        if (parameters.inputs.hasOverlay()) {
            EntityPatcherNew.patchOverlayColor(translationUnit, parameters);
            EntityPatcherNew.patchEntityId(translationUnit, parameters);
        } else if (parameters.inputs.isText()) {
            EntityPatcherNew.patchEntityId(translationUnit, parameters);
        }

        ShaderTransformer.commonPatch(translationUnit, parameters, false);

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            // Alias of gl_MultiTexCoord1 on 1.15+ for OptiFine
            // See https://github.com/IrisShaders/Iris/issues/1149
            translationUnit.rename("gl_MultiTexCoord2", "gl_MultiTexCoord1");

            if (parameters.inputs.hasTex()) {
                translationUnit.replaceExpression("gl_MultiTexCoord0",
                        "vec4(iris_UV0, 0.0, 1.0)");
                translationUnit.injectVariable(
                        "in vec2 iris_UV0;");
            } else {
                translationUnit.replaceExpression("gl_MultiTexCoord0",
                        "vec4(0.5, 0.5, 0.0, 1.0)");
            }

            if (parameters.inputs.hasLight()) {
                translationUnit.replaceExpression("gl_MultiTexCoord1",
                        "vec4(iris_UV2, 0.0, 1.0)");
                translationUnit.injectVariable(
                        "in ivec2 iris_UV2;");
            } else {
                translationUnit.replaceExpression("gl_MultiTexCoord1",
                        "vec4(240.0, 240.0, 0.0, 1.0)");
            }

            ShaderTransformer.patchMultiTexCoord3(translationUnit, parameters);

            // gl_MultiTexCoord0 and gl_MultiTexCoord1 are the only valid inputs (with
            // gl_MultiTexCoord2 and gl_MultiTexCoord3 as aliases), other texture
            // coordinates are not valid inputs.
            ShaderTransformer.replaceGlMultiTexCoordBounded(translationUnit, 4, 7);
        }

        translationUnit.injectVariable(
                "uniform vec4 iris_ColorModulator;");

        if (parameters.inputs.hasColor() && parameters.type == PatchShaderType.VERTEX) {
            // TODO: Handle the fragment / geometry shader here
            if (parameters.alpha == AlphaTests.VERTEX_ALPHA) {
                translationUnit.replaceExpression("gl_Color",
                        "vec4((iris_Color * iris_ColorModulator).rgb, iris_ColorModulator.a)");
            } else {
                translationUnit.replaceExpression("gl_Color",
                        "(iris_Color * iris_ColorModulator)");
            }

            if (parameters.type.glShaderType == ShaderType.VERTEX) {
                translationUnit.injectVariable(
                        "in vec4 iris_Color;");
            }
        } else if (parameters.inputs.isGlint()) {
            translationUnit.injectVariable(
                    "uniform float iris_GlintAlpha;");
            // iris_ColorModulator should be applied regardless of the alpha test state.
            translationUnit.replaceExpression("gl_Color", "vec4(iris_ColorModulator.rgb, iris_ColorModulator.a * iris_GlintAlpha)");
        } else {
            // iris_ColorModulator should be applied regardless of the alpha test state.
            translationUnit.rename("gl_Color", "iris_ColorModulator");
        }

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            if (parameters.inputs.hasNormal()) {
                if (!parameters.inputs.isNewLines()) {
                    translationUnit.rename("gl_Normal", "iris_Normal");
                } else {
                    translationUnit.replaceExpression("gl_Normal",
                            "vec3(0.0, 0.0, 1.0)");
                }

                translationUnit.injectVariable(
                        "in vec3 iris_Normal;");
            } else {
                translationUnit.replaceExpression("gl_Normal",
                        "vec3(0.0, 0.0, 1.0)");
            }
        }

        translationUnit.injectVariable("uniform mat4 iris_LightmapTextureMatrix;");
        translationUnit.injectVariable("uniform mat4 iris_TextureMat;");
        translationUnit.injectVariable("uniform mat4 iris_ModelViewMat;");

        // TODO: More solid way to handle texture matrices
        translationUnit.replaceExpression("gl_TextureMatrix[0]", "iris_TextureMat");
        translationUnit.replaceExpression("gl_TextureMatrix[1]", "iris_LightmapTextureMatrix");

        // TODO: Should probably add the normal matrix as a proper uniform that's
        // computed on the CPU-side of things
        translationUnit.replaceExpression("gl_NormalMatrix",
                "iris_NormalMat");

        translationUnit.replaceExpression("gl_ModelViewMatrixInverse",
                "iris_ModelViewMatInverse");

        translationUnit.replaceExpression("gl_ProjectionMatrixInverse",
                "iris_ProjMatInverse");
        translationUnit.injectVariable("uniform mat3 iris_NormalMat;");
        translationUnit.injectVariable("uniform mat4 iris_ProjMatInverse;");
        translationUnit.injectVariable("uniform mat4 iris_ModelViewMatInverse;");

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            translationUnit.injectVariable(
                    "in vec3 iris_Position;");
            if (translationUnit.containsCall("ftransform")) {
                translationUnit.injectFunction(
                        "vec4 ftransform() { return gl_ModelViewProjectionMatrix * gl_Vertex; }");
            }

            if (parameters.inputs.isNewLines()) {
                translationUnit.replaceExpression("gl_Vertex",
                        "vec4(iris_Position + iris_vertex_offset, 1.0)");

                // Create our own main function to wrap the existing main function, so that we
                // can do our line shenanigans.
                // TRANSFORM: this is fine since the AttributeTransformer has a different name
                // in the vertex shader
                translationUnit.rename("main", "irisMain");
                translationUnit.injectVariable(
                        "vec3 iris_vertex_offset = vec3(0.0);");

                translationUnit.injectVariable("uniform vec2 iris_ScreenSize;");
                translationUnit.injectVariable( "uniform float iris_LineWidth;");

                translationUnit.injectFunction("void iris_widen_lines(vec4 linePosStart, vec4 linePosEnd) {" +
                        "vec3 ndc1 = linePosStart.xyz / linePosStart.w;" +
                        "vec3 ndc2 = linePosEnd.xyz / linePosEnd.w;" +
                        "vec2 lineScreenDirection = normalize((ndc2.xy - ndc1.xy) * iris_ScreenSize);" +
                        "vec2 lineOffset = vec2(-lineScreenDirection.y, lineScreenDirection.x) * iris_LineWidth / iris_ScreenSize;"
                        +
                        "if (lineOffset.x < 0.0) {" +
                        "    lineOffset *= -1.0;" +
                        "}" +
                        "if (gl_VertexID % 2 == 0) {" +
                        "    gl_Position = vec4((ndc1 + vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);" +
                        "} else {" +
                        "    gl_Position = vec4((ndc1 - vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);" +
                        "}}");

                translationUnit.injectAtEnd("void main() {" +
                        "iris_vertex_offset = iris_Normal;" +
                        "irisMain();" +
                        "vec4 linePosEnd = gl_Position;" +
                        "gl_Position = vec4(0.0);" +
                        "iris_vertex_offset = vec3(0.0);" +
                        "irisMain();" +
                        "vec4 linePosStart = gl_Position;" +
                        "iris_widen_lines(linePosStart, linePosEnd);}");
            } else {
                translationUnit.replaceExpression("gl_Vertex", "vec4(iris_Position, 1.0)");
            }
        }

        // TODO: All of the transformed variants of the input matrices, preferably
        // computed on the CPU side...
        translationUnit.replaceExpression("gl_ModelViewProjectionMatrix",
                "(gl_ProjectionMatrix * gl_ModelViewMatrix)");

        if (parameters.hasChunkOffset) {
            boolean doInjection = translationUnit.containsCall("gl_ModelViewMatrix");
            if (doInjection) {
                translationUnit.replaceExpression("gl_ModelViewMatrix",
                        "(iris_ModelViewMat * _iris_internal_translate(iris_ChunkOffset))");
                translationUnit.injectFunction("uniform vec3 iris_ChunkOffset;");
                translationUnit.injectFunction(
                        "mat4 _iris_internal_translate(vec3 offset) {" +
                                "return mat4(1.0, 0.0, 0.0, 0.0," +
                                "0.0, 1.0, 0.0, 0.0," +
                                "0.0, 0.0, 1.0, 0.0," +
                                "offset.x, offset.y, offset.z, 1.0); }");
            }
        } else if (parameters.inputs.isNewLines()) {
            translationUnit.injectVariable(
                    "const float iris_VIEW_SHRINK = 1.0 - (1.0 / 256.0);");
            translationUnit.injectVariable(
                    "const mat4 iris_VIEW_SCALE = mat4(" +
                            "iris_VIEW_SHRINK, 0.0, 0.0, 0.0," +
                            "0.0, iris_VIEW_SHRINK, 0.0, 0.0," +
                            "0.0, 0.0, iris_VIEW_SHRINK, 0.0," +
                            "0.0, 0.0, 0.0, 1.0);");
            translationUnit.replaceExpression("gl_ModelViewMatrix",
                    "(iris_VIEW_SCALE * iris_ModelViewMat)");
        } else {
            translationUnit.rename("gl_ModelViewMatrix", "iris_ModelViewMat");
        }

        translationUnit.rename("gl_ProjectionMatrix", "iris_ProjMat");
        translationUnit.injectVariable(
                "uniform mat4 iris_ProjMat;");
    }

    public static void patchVanillaCore(Transformer translationUnit, VanillaParameters parameters) {
        if (parameters.inputs.hasOverlay()) {
            if (!parameters.inputs.isText()) {
                EntityPatcherNew.patchOverlayColor(translationUnit, parameters);
            }
            EntityPatcherNew.patchEntityId(translationUnit, parameters);
        }

        ShaderTransformer.commonPatch(translationUnit, parameters, true);
        translationUnit.rename("alphaTestRef", "iris_currentAlphaTest");
        translationUnit.rename("modelViewMatrix", "iris_ModelViewMat");
        translationUnit.rename("gl_ModelViewMatrix", "iris_ModelViewMat");
        translationUnit.rename("modelViewMatrixInverse", "iris_ModelViewMatInverse");
        translationUnit.rename("gl_ModelViewMatrixInverse", "iris_ModelViewMatInverse");
        translationUnit.rename("projectionMatrix", "iris_ProjMat");
        translationUnit.rename("gl_ProjectionMatrix", "iris_ProjMat");
        translationUnit.rename("projectionMatrixInverse", "iris_ProjMatInverse");
        translationUnit.rename("gl_ProjectionMatrixInverse", "iris_ProjMatInverse");
        translationUnit.rename("textureMatrix", "iris_TextureMat");

        translationUnit.replaceExpression("gl_TextureMatrix[0]", "iris_TextureMat");
        translationUnit.replaceExpression("gl_TextureMatrix[1]",
                "mat4(vec4(0.00390625, 0.0, 0.0, 0.0), vec4(0.0, 0.00390625, 0.0, 0.0), vec4(0.0, 0.0, 0.00390625, 0.0), vec4(0.03125, 0.03125, 0.03125, 1.0))");
        translationUnit.replaceExpression("gl_TextureMatrix[2]",
                "mat4(vec4(0.00390625, 0.0, 0.0, 0.0), vec4(0.0, 0.00390625, 0.0, 0.0), vec4(0.0, 0.0, 0.00390625, 0.0), vec4(0.03125, 0.03125, 0.03125, 1.0))");
        addIfNotExistsType(translationUnit, "iris_TextureMat", "uniform mat4");
        addIfNotExistsType(translationUnit, "iris_ProjMat", "uniform mat4");
        addIfNotExistsType(translationUnit, "iris_ProjMatInverse", "uniform mat4");
        addIfNotExistsType(translationUnit, "iris_ModelViewMat", "uniform mat4");
        addIfNotExistsType(translationUnit, "iris_ModelViewMatInverse", "uniform mat4");
        translationUnit.rename("normalMatrix", "iris_NormalMat");
        translationUnit.rename("gl_NormalMatrix", "iris_NormalMat");
        addIfNotExistsType(translationUnit, "iris_NormalMat", "uniform mat3");
        translationUnit.rename("chunkOffset", "iris_ChunkOffset");
        addIfNotExistsType(translationUnit, "iris_ChunkOffset", "uniform vec3");

        upgradeStorageQualifiers(translationUnit, parameters);

        if (parameters.type == PatchShaderType.VERTEX) {
            translationUnit.replaceExpression("gl_Vertex", "vec4(iris_Position, 1.0)");
            translationUnit.rename("vaPosition", "iris_Position");
            if (parameters.inputs.hasColor()) {
                translationUnit.replaceExpression("vaColor", "iris_Color * iris_ColorModulator");
                translationUnit.replaceExpression("gl_Color", "iris_Color * iris_ColorModulator");
            } else {
                translationUnit.replaceExpression("vaColor", "iris_ColorModulator");
                translationUnit.replaceExpression("gl_Color", "iris_ColorModulator");
            }
            translationUnit.rename("vaNormal", "iris_Normal");
            translationUnit.rename("gl_Normal", "iris_Normal");
            translationUnit.rename("vaUV0", "iris_UV0");
            translationUnit.replaceExpression("gl_MultiTexCoord0", "vec4(iris_UV0, 0.0, 1.0)");
            if (parameters.inputs.hasLight()) {
                translationUnit.replaceExpression("gl_MultiTexCoord1", "vec4(iris_UV2, 0.0, 1.0)");
                translationUnit.replaceExpression("gl_MultiTexCoord2", "vec4(iris_UV2, 0.0, 1.0)");
                translationUnit.rename("vaUV2", "iris_UV2");
            } else {
                translationUnit.replaceExpression("gl_MultiTexCoord1", "vec4(240.0, 240.0, 0.0, 1.0)");
                translationUnit.replaceExpression("gl_MultiTexCoord2", "vec4(240.0, 240.0, 0.0, 1.0)");
                translationUnit.rename("vaUV2", "iris_UV2");
            }
            translationUnit.rename("vaUV1", "iris_UV1");

            addIfNotExistsType(translationUnit, "iris_Color", "in vec4");
            addIfNotExistsType(translationUnit, "iris_ColorModulator", "uniform vec4");
            addIfNotExistsType(translationUnit, "iris_Position", "in vec3");
            addIfNotExistsType(translationUnit, "iris_Normal", "in vec3");
            addIfNotExistsType(translationUnit, "iris_UV0", "in vec2");
            addIfNotExistsType(translationUnit, "iris_UV1", "in vec2");
            addIfNotExistsType(translationUnit, "iris_UV2", "in vec2");
        }
    }
}
