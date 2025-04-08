package net.irisshaders.iris.pipeline.foss_transform;

import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import org.taumc.glsl.Transformer;

public class EntityPatcherNew {
    public static void patchOverlayColor(Transformer translationUnit, VanillaParameters parameters) {
        // delete original declaration
        if (translationUnit.hasVariable("entityColor")) {
            translationUnit.removeVariable("entityColor");
        }

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            // add our own declarations
            // TODO: We're exposing entityColor to this stage even if it isn't declared in
            // this stage. But this is needed for the pass-through behavior.
            translationUnit.injectVariable("uniform sampler2D iris_overlay;");
            translationUnit.injectVariable("out vec4 entityColor;");
            translationUnit.injectVariable("out vec4 iris_vertexColor;");
            translationUnit.injectVariable("in ivec2 iris_UV1;");

            // Create our own main function to wrap the existing main function, so that we
            // can pass through the overlay color at the end to the geometry or fragment
            // stage.
            // TAU: We can only prepend one statement at a time, so we append in reverse order to the original
            // Iris transformer.


            // Workaround for a shader pack bug:
            // https://github.com/IrisShaders/Iris/issues/1549
            // Some shader packs incorrectly ignore the alpha value, and assume that rgb
            // will be zero if there is no hit flash, we try to emulate that here
            translationUnit.prependMain("entityColor.rgb *= float(entityColor.a != 0.0);");
            translationUnit.prependMain("iris_vertexColor = iris_Color;");
            translationUnit.prependMain("entityColor = vec4(overlayColor.rgb, 1.0 - overlayColor.a);");
            translationUnit.prependMain("vec4 overlayColor = texelFetch(iris_overlay, iris_UV1, 0);");
        } else if (parameters.type.glShaderType == ShaderType.TESSELATION_CONTROL) {
            // replace read references to grab the color from the first vertex.
            translationUnit.replaceExpression("entityColor", "entityColor[gl_InvocationID]");

            // TODO: this is passthrough behavior
            translationUnit.injectVariable(
                    "patch out vec4 entityColorTCS;");
            translationUnit.injectVariable("in vec4 entityColor[];");
            translationUnit.injectVariable("out vec4 iris_vertexColorTCS[];");
            translationUnit.injectVariable("in vec4 iris_vertexColor[];");
            translationUnit.prependMain(
                    "entityColorTCS = entityColor[gl_InvocationID];\n" +
                    "iris_vertexColorTCS[gl_InvocationID] = iris_vertexColor[gl_InvocationID];");
        } else if (parameters.type.glShaderType == ShaderType.TESSELATION_EVAL) {
            // replace read references to grab the color from the first vertex.
            translationUnit.replaceExpression("entityColor", "entityColorTCS");

            // TODO: this is passthrough behavior
            translationUnit.injectVariable("out vec4 entityColorTES;");
            translationUnit.injectVariable("patch in vec4 entityColorTCS;");
            translationUnit.injectVariable("out vec4 iris_vertexColorTES;");
            translationUnit.injectFunction("in vec4 iris_vertexColorTCS[];");
           translationUnit.prependMain(
                    "entityColorTES = entityColorTCS;\n" +
                    "iris_vertexColorTES = iris_vertexColorTCS[0];");
        } else if (parameters.type.glShaderType == ShaderType.GEOMETRY) {
            // replace read references to grab the color from the first vertex.
            translationUnit.replaceExpression("entityColor", "entityColor[0]");

            // TODO: this is passthrough behavior
            translationUnit.injectVariable("out vec4 entityColorGS;");
            translationUnit.injectVariable("in vec4 entityColor[];");
            translationUnit.injectVariable("out vec4 iris_vertexColorGS;");
            translationUnit.injectVariable("in vec4 iris_vertexColor[];");
            translationUnit.prependMain(
                    "entityColorGS = entityColor[0];\n" +
                    "iris_vertexColorGS = iris_vertexColor[0];");

            if (parameters.hasTesselation) {
                translationUnit.rename("iris_vertexColor", "iris_vertexColorTES");
                translationUnit.rename("entityColor", "entityColorTES");
            }
        } else if (parameters.type.glShaderType == ShaderType.FRAGMENT) {
            translationUnit.injectVariable(
                    "in vec4 entityColor;");
            translationUnit.injectVariable("in vec4 iris_vertexColor;");

            translationUnit.prependMain("float iris_vertexColorAlpha = iris_vertexColor.a;");

            // Different output name to avoid a name collision in the geometry shader.
            if (parameters.hasGeometry) {
                translationUnit.rename("entityColor", "entityColorGS");
                translationUnit.rename("iris_vertexColor", "iris_vertexColorGS");
            } else if (parameters.hasTesselation) {
                translationUnit.rename("entityColor", "entityColorTES");
                translationUnit.rename("iris_vertexColor", "iris_vertexColorTES");
            }
        }
    }

    public static void patchEntityId(
            Transformer translationUnit,
            VanillaParameters parameters) {
        // delete original declaration
        if (translationUnit.hasVariable("entityId")) {
            translationUnit.removeVariable("entityId");
        }
        if (translationUnit.hasVariable("blockEntityId")) {
            translationUnit.removeVariable("blockEntityId");
        }
        if (translationUnit.hasVariable("currentRenderedItemId")) {
            translationUnit.removeVariable("currentRenderedItemId");
        }

        if (parameters.type.glShaderType == ShaderType.GEOMETRY) {
            translationUnit.replaceExpression("entityId",
                    "iris_entityInfo[0].x");

            translationUnit.replaceExpression("blockEntityId",
                    "iris_entityInfo[0].y");

            translationUnit.replaceExpression("currentRenderedItemId",
                    "iris_entityInfo[0].z");
        } else {
            translationUnit.replaceExpression("entityId",
                    "iris_entityInfo.x");

            translationUnit.replaceExpression("blockEntityId",
                    "iris_entityInfo.y");

            translationUnit.replaceExpression("currentRenderedItemId",
                    "iris_entityInfo.z");
        }

        if (parameters.type.glShaderType == ShaderType.VERTEX) {
            // add our own declarations
            // TODO: We're exposing entityColor to this stage even if it isn't declared in
            // this stage. But this is needed for the pass-through behavior.
            translationUnit.injectVariable(
                    "flat out ivec3 iris_entityInfo;");
            translationUnit.injectVariable("in ivec3 iris_Entity;");

            // Create our own main function to wrap the existing main function, so that we
            // can pass through the overlay color at the end to the geometry or fragment
            // stage.
            translationUnit.prependMain(
                    "iris_entityInfo = iris_Entity;");
        } else if (parameters.type.glShaderType == ShaderType.TESSELATION_CONTROL) {
            // TODO: this is passthrough behavior
            translationUnit.injectVariable(
                    "flat out ivec3 iris_entityInfoTCS[];");
            translationUnit.injectVariable("flat in ivec3 iris_entityInfo[];");
            translationUnit.replaceExpression("iris_entityInfo", "iris_EntityInfo[gl_InvocationID]");

            translationUnit.prependMain(
                    "iris_entityInfoTCS[gl_InvocationID] = iris_entityInfo[gl_InvocationID];");
        } else if (parameters.type.glShaderType == ShaderType.TESSELATION_EVAL) {
            // TODO: this is passthrough behavior
            translationUnit.injectVariable("flat out ivec3 iris_entityInfoTES;");
            translationUnit.injectVariable("flat in ivec3 iris_entityInfoTCS[];");
            translationUnit.prependMain(
                    "iris_entityInfoTES = iris_entityInfoTCS[0];");

            translationUnit.replaceExpression("iris_entityInfo", "iris_EntityInfoTCS[0]");

        } else if (parameters.type.glShaderType == ShaderType.GEOMETRY) {
            // TODO: this is passthrough behavior
            translationUnit.injectVariable(
                    "flat out ivec3 iris_entityInfoGS;");
            translationUnit.injectVariable("flat in ivec3 iris_entityInfo" + (parameters.hasTesselation ? "TES" : "") + "[];");
            translationUnit.prependMain(
                    "iris_entityInfoGS = iris_entityInfo" + (parameters.hasTesselation ? "TES" : "") + "[0];");
        } else if (parameters.type.glShaderType == ShaderType.FRAGMENT) {
            translationUnit.injectVariable(
                    "flat in ivec3 iris_entityInfo;");

            // Different output name to avoid a name collision in the geometry shader.
            if (parameters.hasGeometry) {
                translationUnit.rename("iris_entityInfo", "iris_EntityInfoGS");
            } else if (parameters.hasTesselation) {
                translationUnit.rename("iris_entityInfo", "iris_entityInfoTES");
            }
        }
    }
}
