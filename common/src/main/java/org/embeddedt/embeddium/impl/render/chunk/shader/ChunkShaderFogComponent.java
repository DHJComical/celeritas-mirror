package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;

/**
 * These shader implementations try to remain compatible with the deprecated fixed function pipeline by manually
 * copying the state into each shader's uniforms. The shader code itself is a straight-forward implementation of the
 * fog functions themselves from the fixed-function pipeline, except that they use the distance from the camera
 * rather than the z-buffer to produce better looking fog that doesn't move with the player's view angle.
 *
 * Minecraft itself will actually try to enable distance-based fog by using the proprietary NV_fog_distance extension,
 * but as the name implies, this only works on graphics cards produced by NVIDIA. The shader implementation however does
 * not depend on any vendor-specific extensions and is written using very simple GLSL code.
 */
public abstract class ChunkShaderFogComponent implements ChunkShaderComponent {
    protected final FogService fogService;

    protected ChunkShaderFogComponent(FogService fogService) {
        this.fogService = fogService;
    }

    public static class None extends ChunkShaderFogComponent {
        public None(ShaderBindingContext context, ChunkShaderEnvironment environment) {
            super(environment.fogService());
        }

        @Override
        public void setup() {

        }
    }

    public static class Exp extends ChunkShaderFogComponent {
        private final GlUniformFloat4v uFogColor;
        private final GlUniformFloat uFogDensity;

        public Exp(ShaderBindingContext context, ChunkShaderEnvironment environment) {
            super(environment.fogService());

            this.uFogColor = context.bindUniform("u_FogColor", GlUniformFloat4v::new);
            this.uFogDensity = context.bindUniform("u_FogDensity", GlUniformFloat::new);
        }

        @Override
        public void setup() {
            this.uFogColor.set(this.fogService.getFogColor());
            this.uFogDensity.set(this.fogService.getFogDensity());
        }
    }

    public static class Exp2 extends ChunkShaderFogComponent {
        private final GlUniformFloat4v uFogColor;
        private final GlUniformFloat uFogDensity;

        public Exp2(ShaderBindingContext context, ChunkShaderEnvironment environment) {
            super(environment.fogService());

            this.uFogColor = context.bindUniform("u_FogColor", GlUniformFloat4v::new);
            this.uFogDensity = context.bindUniform("u_FogDensity", GlUniformFloat::new);
        }

        @Override
        public void setup() {
            this.uFogColor.set(this.fogService.getFogColor());
            this.uFogDensity.set(this.fogService.getFogDensity());
        }
    }

    public static class Smooth extends ChunkShaderFogComponent {
        private final GlUniformFloat4v uFogColor;

        private final GlUniformInt uFogShape;
        private final GlUniformFloat uFogStart;
        private final GlUniformFloat uFogEnd;

        public Smooth(ShaderBindingContext context, ChunkShaderEnvironment environment) {
            super(environment.fogService());

            this.uFogColor = context.bindUniform("u_FogColor", GlUniformFloat4v::new);
            this.uFogShape = context.bindUniform("u_FogShape", GlUniformInt::new);
            this.uFogStart = context.bindUniform("u_FogStart", GlUniformFloat::new);
            this.uFogEnd = context.bindUniform("u_FogEnd", GlUniformFloat::new);
        }

        @Override
        public void setup() {
            this.uFogColor.set(this.fogService.getFogColor());
            this.uFogShape.set(this.fogService.getFogShapeIndex());

            this.uFogStart.set(this.fogService.getFogStart());
            this.uFogEnd.set(this.fogService.getFogEnd());
        }
    }

}
