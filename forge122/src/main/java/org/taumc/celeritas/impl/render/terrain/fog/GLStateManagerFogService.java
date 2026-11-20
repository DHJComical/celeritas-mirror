package org.taumc.celeritas.impl.render.terrain.fog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.fog.SkyTextureCopy;
import org.taumc.celeritas.impl.render.terrain.GLStateManagerGlStateService;
import org.taumc.celeritas.CeleritasVintage;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;

public class GLStateManagerFogService implements FogService {
    public static final GLStateManagerFogService INSTANCE = new GLStateManagerFogService();

    @Override
    public float getFogEnd() {
        return GlStateManager.fogState.end;
    }

    @Override
    public float getFogStart() {
        return GlStateManager.fogState.start;
    }

    @Override
    public float getFogDensity() {
        return GlStateManager.fogState.density;
    }

    @Override
    public int getFogShapeIndex() {
        return 0;
    }

    @Override
    public float getFogCutoff() {
        return GlStateManager.fogState.end;
    }

    @Override
    public float[] getFogColor() {
        EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;
        return new float[]{entityRenderer.fogColorRed, entityRenderer.fogColorGreen, entityRenderer.fogColorBlue, 1.0F};
    }

    @Override
    public ChunkFogMode getFogMode() {
        ChunkFogMode mode = this.getGlFogMode();
        return mode == ChunkFogMode.SMOOTH && this.getSkyTexture() != 0 ? ChunkFogMode.SMOOTH_SKY : mode;
    }

    private ChunkFogMode getGlFogMode() {
        if (!GlStateManager.fogState.fog.currentState) {
            return ChunkFogMode.NONE;
        }
        return ChunkFogMode.fromGLMode(GlStateManager.fogState.mode);
    }

    private final SkyTextureCopy skyTexture = new SkyTextureCopy(GLStateManagerGlStateService.INSTANCE);

    @Override
    public int getSkyTexture() {
        return this.skyTexture.getTexture();
    }

    public void captureSkyTexture() {
        if (!SkyTextureCopy.shouldProvide(CeleritasVintage.options().quality.useSkyFog)) {
            this.skyTexture.delete();
            return;
        }

        // Sized to the display even when FBOs are disabled
        Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
        this.skyTexture.copy(framebuffer.framebufferWidth, framebuffer.framebufferHeight);
    }

    public void invalidateSkyTexture() {
        this.skyTexture.invalidate();
    }

    public void deleteSkyTexture() {
        this.skyTexture.delete();
    }

    @Override
    public float getSkyFogEdgeStart() {
        return this.getSkyFogEdgeEnd() * 0.75F;
    }

    @Override
    public float getSkyFogEdgeEnd() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0F;
    }
}
