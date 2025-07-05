package org.embeddedt.embeddium.impl.render.immediate;

import org.embeddedt.embeddium.impl.gl.shader.*;
import org.embeddedt.embeddium.impl.gl.shader.uniform.*;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.joml.Matrix4f;

public class CloudShader implements AutoCloseable {
    private static final int ATTRIBUTE_POSITION = 0;
    private static final int ATTRIBUTE_COLOR = 1;
    private static final int FRAG_COLOR = 0;

    private final GlProgram<CloudShaderInterface> program;

    private static final ShaderConstants CLOUD_CONSTANTS = ShaderConstants.builder().add("USE_FOG").build();

    public CloudShader() {
        this.program = createShader();
    }

    private GlProgram<CloudShaderInterface> createShader() {
        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, "sodium:clouds/clouds.vsh", CLOUD_CONSTANTS);

        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, "sodium:clouds/clouds.fsh", CLOUD_CONSTANTS);

        try {
            return GlProgram.builder("celeritas:cloud_shader")
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
            this.uniformFogStart.set(ChunkShaderFogComponent.FOG_SERVICE.getFogStart());
            this.uniformFogEnd.set(ChunkShaderFogComponent.FOG_SERVICE.getFogEnd());
            this.uniformFogColor.set(ChunkShaderFogComponent.FOG_SERVICE.getFogColor());
            this.uniformMainColor.set(new float[] { r, g, b, a });
        }
    }
}
