package org.embeddedt.embeddium.impl.gl.compat;

//? if >=1.17
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.fog.SkyTextureCopy;
import org.lwjgl.opengl.GL20;

import com.mojang.blaze3d.platform.GlStateManager;

import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
//? if <1.18
//import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;

public class FogHelper implements FogService {
    public static final FogHelper INSTANCE = new FogHelper();

    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = Mth.sqrt(FAR_PLANE_THRESHOLD_EXP);

    public float getFogEnd() {
        //? if <1.17 {
        //return GlStateManager.FOG.end;
        //?} else {
        return RenderSystem.getShaderFogEnd();
        //?}
    }

    public float getFogStart() {
        //? if <1.17 {
        //return GlStateManager.FOG.start;
        //?} else {
        return RenderSystem.getShaderFogStart();
        //?}
    }

    //? if <1.17 {
    /*public float getFogDensity() {
        return GlStateManager.FOG.density;
    }
    *///?} else {
    public float getFogDensity() {
        throw new UnsupportedOperationException();
    }
    //?}

    public int getFogShapeIndex() {
        // fog shape is always zero for 1.17 and older
        //? if >=1.18 {
        return RenderSystem.getShaderFogShape().getIndex();
        //?} else
        //return 0;
    }

    public float getFogCutoff() {
        //? if <1.17 {
        /*int mode = GlStateManager.FOG.mode;

        switch (mode) {
            case GL20.GL_LINEAR:
                return getFogEnd();
            case GL20.GL_EXP:
                return FAR_PLANE_THRESHOLD_EXP / getFogDensity();
            case GL20.GL_EXP2:
                return FAR_PLANE_THRESHOLD_EXP2 / getFogDensity();
            default:
                return 0.0f;
        }
        *///?} else
        return getFogEnd();
    }

    public float[] getFogColor() {
        //? if <1.17 {
        //return new float[]{FogRenderer.fogRed, FogRenderer.fogGreen, FogRenderer.fogBlue, 1.0F};
        //?} else {
        return RenderSystem.getShaderFogColor();
        //?}
    }

    public ChunkFogMode getFogMode() {
        ChunkFogMode mode = this.getVanillaFogMode();
        return mode == ChunkFogMode.SMOOTH && this.getSkyTexture() != 0 ? ChunkFogMode.SMOOTH_SKY : mode;
    }

    private ChunkFogMode getVanillaFogMode() {
        //? if <1.17 {
        /*int mode = GlStateManager.FOG.mode;

        if(mode == 0 || !GlStateManager.FOG.enable.enabled)
            return ChunkFogMode.NONE;

        switch (mode) {
            case GL20.GL_EXP2:
                return ChunkFogMode.EXP2;
            case GL20.GL_EXP:
                return ChunkFogMode.EXP;
            case GL20.GL_LINEAR:
                return ChunkFogMode.SMOOTH;
            default:
                throw new UnsupportedOperationException("Unknown fog mode: " + mode);
        }
        *///?} else
        return ChunkFogMode.SMOOTH;
    }

    private final SkyTextureCopy skyTexture = new SkyTextureCopy(ModernGlStateService.INSTANCE);

    @Override
    public int getSkyTexture() {
        return this.skyTexture.getTexture();
    }

    public void captureSkyTexture() {
        if (!SkyTextureCopy.shouldProvide(Celeritas.options().quality.useSkyFog)) {
            this.skyTexture.delete();
            return;
        }

        var target = Minecraft.getInstance().getMainRenderTarget();
        this.skyTexture.copy(target.width, target.height);
    }

    public void invalidateSkyTexture() {
        this.skyTexture.invalidate();
    }

    public void deleteSkyTexture() {
        this.skyTexture.delete();
    }

    @Override
    public float getSkyFogEdgeStart() {
        float end = this.getSkyFogEdgeEnd();
        //? if >=1.18 {
        return end - Mth.clamp(end / 10.0F, 4.0F, 64.0F);
        //?} else {
        /*return end * 0.75F;
        *///?}
    }

    @Override
    public float getSkyFogEdgeEnd() {
        float renderDistance = Minecraft.getInstance().gameRenderer.getRenderDistance();
        //? if >=1.18 {
        return Math.max(renderDistance, 32.0F);
        //?} else {
        /*return Math.max(renderDistance - 16.0F, 32.0F);
        *///?}
    }
}
