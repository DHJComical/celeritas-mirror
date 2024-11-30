package org.embeddedt.embeddium.impl.render.immediate;

import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
import org.embeddedt.embeddium.impl.gl.shader.*;
import org.embeddedt.embeddium.impl.gl.shader.uniform.*;
import org.embeddedt.embeddium.impl.render.chunk.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.joml.Matrix4f;

public class CloudShader implements AutoCloseable {
    private static final int ATTRIBUTE_POSITION = 0;
    private static final int ATTRIBUTE_COLOR = 1;
    private static final int FRAG_COLOR = 0;

    private final GlProgram<CloudShaderInterface> program;

    public CloudShader() {
        this.program = createShader();
    }

    private GlProgram<CloudShaderInterface> createShader() {
        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                ResourceLocationUtil.make("sodium", "clouds/clouds.vsh"), ShaderConstants.EMPTY);

        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                ResourceLocationUtil.make("sodium", "clouds/clouds.fsh"), ShaderConstants.EMPTY);

        try {
            return GlProgram.builder(ResourceLocationUtil.make("celeritas", "cloud_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("Position", ATTRIBUTE_POSITION)
                    .bindAttribute("Color", ATTRIBUTE_COLOR)
                    .bindFragmentData("fragColor", FRAG_COLOR)
                    .link(CloudShaderInterface::new);
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    public void prepareForDraw(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float r, float g, float b, float a) {
        this.program.bind();
        this.program.getInterface().setupState(modelViewMatrix, projectionMatrix, r, g, b, a);
    }

    public void clear() {
        this.program.unbind();
    }

    @Override
    public void close() {
        this.program.delete();
    }

    static class CloudShaderInterface {
        private final GlUniformMatrix4f uniformModelViewMatrix;
        private final GlUniformMatrix4f uniformProjectionMatrix;
        private final GlUniformFloat uniformFogStart, uniformFogEnd;
        private final GlUniformFloat4v uniformFogColor, uniformMainColor;

        public CloudShaderInterface(ShaderBindingContext context) {
            this.uniformModelViewMatrix = context.bindUniform("ModelViewMat", GlUniformMatrix4f::new);
            this.uniformProjectionMatrix = context.bindUniform("ProjMat", GlUniformMatrix4f::new);
            this.uniformFogStart = context.bindUniform("FogStart", GlUniformFloat::new);
            this.uniformFogEnd = context.bindUniform("FogEnd", GlUniformFloat::new);
            this.uniformFogColor = context.bindUniform("FogColor", GlUniformFloat4v::new);
            this.uniformMainColor = context.bindUniform("ColorModulator", GlUniformFloat4v::new);
        }

        public void setupState(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float r, float g, float b, float a) {
            this.uniformModelViewMatrix.set(modelViewMatrix);
            this.uniformProjectionMatrix.set(projectionMatrix);
            this.uniformFogStart.set(FogHelper.getFogStart());
            this.uniformFogEnd.set(FogHelper.getFogEnd());
            this.uniformFogColor.set(FogHelper.getFogColor());
            this.uniformMainColor.set(new float[] { r, g, b, a });
        }
    }
}
