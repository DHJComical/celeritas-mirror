package org.taumc.celeritas.render.terrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.joml.Vector4f;
import org.taumc.celeritas.mixin.terrain.GameRendererAccessor;

import java.util.Collection;
import java.util.List;

public class PostmodernFogHelper implements FogService {
    private static final PostmodernFogHelper INSTANCE = new PostmodernFogHelper();

    public interface FogDataGetter {
        FogData celeritas$getLastFogData();
        Vector4f celeritas$getLastFogColor();
    }

    private static FogDataGetter fogRenderer() {
        return (FogDataGetter)((GameRendererAccessor)Minecraft.getInstance().gameRenderer).celeritas$getFogRenderer();
    }

    @Override
    public float getFogEnd() {
        return fogRenderer().celeritas$getLastFogData().renderDistanceEnd;
    }

    @Override
    public float getFogStart() {
        return fogRenderer().celeritas$getLastFogData().renderDistanceStart;
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
        var colorVec = fogRenderer().celeritas$getLastFogColor();
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
            this.uFogColor.set(PostmodernFogHelper.INSTANCE.getFogColor());
            var data = PostmodernFogHelper.fogRenderer().celeritas$getLastFogData();
            this.uRenderDistFogStart.set(data.renderDistanceStart);
            this.uRenderDistFogEnd.set(data.renderDistanceEnd);
            this.uEnvFogStart.set(data.environmentalStart);
            this.uEnvFogEnd.set(data.environmentalEnd);
        }
    }
}