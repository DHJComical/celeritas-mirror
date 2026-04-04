package org.embeddedt.embeddium.impl.gl.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;

import java.util.Collection;
import java.util.List;

public class FogHelper implements FogService {
    private static final FogHelper INSTANCE = new FogHelper();

    private static FogData fogData() {
        return Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.fogData;
    }

    @Override
    public float getFogEnd() {
        return fogData().renderDistanceEnd;
    }

    @Override
    public float getFogStart() {
        return fogData().renderDistanceStart;
    }

    @Override
    public float getFogDensity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFogShapeIndex() {
        return 1; // TODO
    }

    @Override
    public float getFogCutoff() {
        return getFogEnd();
    }

    @Override
    public float[] getFogColor() {
        var colorVec = fogData().color;
        return new float[] { colorVec.x, colorVec.y, colorVec.z, colorVec.w };
    }

    @Override
    public ChunkShaderComponent.Factory<?> getFogMode() {
        return PostmodernFogComponent.FACTORY;
    }

    private static class PostmodernFogComponent implements ChunkShaderComponent {
        private static final ChunkShaderComponent.Factory<PostmodernFogComponent> FACTORY = new ChunkShaderComponent.Factory<>() {
            @Override
            public PostmodernFogComponent create(ShaderBindingContext context) {
                return new PostmodernFogComponent(context);
            }

            @Override
            public Collection<String> getDefines() {
                return List.of("USE_FOG", "USE_FOG_POSTMODERN");
            }
        };

        private final GlUniformFloat4v uFogColor;
        private final GlUniformFloat uRenderDistFogStart, uRenderDistFogEnd, uEnvFogStart, uEnvFogEnd;

        public PostmodernFogComponent(ShaderBindingContext context) {
            this.uFogColor = context.bindUniform("u_FogColor", GlUniformFloat4v::new);
            this.uRenderDistFogStart = context.bindUniform("u_RenderDistFogStart", GlUniformFloat::new);
            this.uRenderDistFogEnd = context.bindUniform("u_RenderDistFogEnd", GlUniformFloat::new);
            this.uEnvFogStart = context.bindUniform("u_EnvFogStart", GlUniformFloat::new);
            this.uEnvFogEnd = context.bindUniform("u_EnvFogEnd", GlUniformFloat::new);
        }

        @Override
        public void setup() {
            this.uFogColor.set(FogHelper.INSTANCE.getFogColor());
            var data = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.fogData;
            this.uRenderDistFogStart.set(data.renderDistanceStart);
            this.uRenderDistFogEnd.set(data.renderDistanceEnd);
            this.uEnvFogStart.set(data.environmentalStart);
            this.uEnvFogEnd.set(data.environmentalEnd);
        }
    }
}