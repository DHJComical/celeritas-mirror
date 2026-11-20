package org.embeddedt.embeddium.impl.gl.compat;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.fog.SkyTextureCopy;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderEnvironment;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class FogHelper implements FogService {
    public static final FogHelper INSTANCE = new FogHelper();

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
        return this.getSkyTexture() != 0 ? PostmodernFogComponent.SKY_FACTORY : PostmodernFogComponent.FACTORY;
    }

    private final SkyTextureCopy skyTexture = new SkyTextureCopy(ModernGlStateService.INSTANCE);
    private int skyReadFramebuffer;

    @Override
    public int getSkyTexture() {
        return this.skyTexture.getTexture();
    }

    public void captureSkyTexture() {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

        if (!SkyTextureCopy.shouldProvide(Celeritas.options().quality.useSkyFog) || !(target.getColorTexture() instanceof GlTexture color)) {
            this.skyTexture.delete();
            return;
        }

        // Blaze3D only binds framebuffers inside its own passes, so read the main target through one of our own
        if (this.skyReadFramebuffer == 0) {
            this.skyReadFramebuffer = GlStateManager.glGenFramebuffers();
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.skyReadFramebuffer);
        GlStateManager._glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color.glId(), 0);
        this.skyTexture.copy(target.width, target.height);
    }

    public void invalidateSkyTexture() {
        this.skyTexture.invalidate();
    }

    public void deleteSkyTexture() {
        this.skyTexture.delete();
    }

    private static class PostmodernFogComponent implements ChunkShaderComponent {
        private static final ChunkShaderComponent.Factory<PostmodernFogComponent> FACTORY = new Factory(false);
        private static final ChunkShaderComponent.Factory<PostmodernFogComponent> SKY_FACTORY = new Factory(true);

        private record Factory(boolean skyFog) implements ChunkShaderComponent.Factory<PostmodernFogComponent> {
            @Override
            public PostmodernFogComponent create(ShaderBindingContext context, ChunkShaderEnvironment environment) {
                return new PostmodernFogComponent(context, this.skyFog ? new ChunkShaderFogComponent.SkyUniforms(context, environment.fogService()) : null);
            }

            @Override
            public Collection<String> getDefines() {
                return this.skyFog ? List.of("USE_FOG", "USE_FOG_POSTMODERN", "USE_SKY_FOG") : List.of("USE_FOG", "USE_FOG_POSTMODERN");
            }
        }

        private final GlUniformFloat4v uFogColor;
        private final GlUniformFloat uRenderDistFogStart, uRenderDistFogEnd, uEnvFogStart, uEnvFogEnd;

        private final ChunkShaderFogComponent.@Nullable SkyUniforms skyFog;

        public PostmodernFogComponent(ShaderBindingContext context, ChunkShaderFogComponent.@Nullable SkyUniforms skyFog) {
            this.skyFog = skyFog;
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

            if (this.skyFog != null) {
                this.skyFog.setup();
            }
        }
    }
}