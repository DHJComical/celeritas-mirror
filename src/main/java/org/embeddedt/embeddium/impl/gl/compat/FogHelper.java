package org.embeddedt.embeddium.impl.gl.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL20;

import com.mojang.blaze3d.platform.GlStateManager;

import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
//? if <1.18
/*import net.minecraft.client.renderer.FogRenderer;*/
import net.minecraft.util.Mth;

public class FogHelper {
    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = Mth.sqrt(FAR_PLANE_THRESHOLD_EXP);

    public static float getFogEnd() {
        //? if <1.18 {
        /*return GlStateManager.FOG.end;
        *///?} else if <1.21.2 {
        return RenderSystem.getShaderFogEnd();
        //?} else
        /*return RenderSystem.getShaderFog().end();*/
    }

    public static float getFogStart() {
        //? if <1.18 {
        /*return GlStateManager.FOG.start;
        *///?} else if <1.21.2 {
        return RenderSystem.getShaderFogStart();
        //?} else
        /*return RenderSystem.getShaderFog().start();*/
    }

    //? if <1.18 {
    /*public static float getFogDensity() {
        return GlStateManager.FOG.density;
    }
    *///?} else {
    public static float getFogDensity() {
        throw new UnsupportedOperationException();
    }
    //?}

    public static int getFogShapeIndex() {
        //? if >=1.21.2 {
        /*return RenderSystem.getShaderFog().shape().getIndex();
        *///?} else if >=1.18 {
        return RenderSystem.getShaderFogShape().getIndex();
        //?} else
        /*return 0;*/ // always zero for 1.16
    }

    public static float getFogCutoff() {
        //? if <1.18 {
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

    public static float[] getFogColor() {
        //? if <1.18 {
        /*return new float[]{FogRenderer.fogRed, FogRenderer.fogGreen, FogRenderer.fogBlue, 1.0F};
        *///?} else if <1.21.2 {
        return RenderSystem.getShaderFogColor();
        //?} else {
        /*var fogParams = RenderSystem.getShaderFog();
        return new float[] { fogParams.red(), fogParams.green(), fogParams.blue(), fogParams.alpha() };
        *///?}
    }

    public static ChunkFogMode getFogMode() {
        //? if <1.18 {
        /*int mode = GlStateManager.FOG.mode;

        if(mode == 0 || !GlStateManager.FOG.enable.enabled)
            return ChunkFogMode.NONE;

        switch (mode) {
            case GL20.GL_EXP2:
            case GL20.GL_EXP:
                return ChunkFogMode.EXP2;
            case GL20.GL_LINEAR:
                return ChunkFogMode.SMOOTH;
            default:
                throw new UnsupportedOperationException("Unknown fog mode: " + mode);
        }
        *///?} else
        return ChunkFogMode.SMOOTH;
    }
}
