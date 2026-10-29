package net.irisshaders.iris.compat.flywheel.engine;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.backend.Samplers;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ClrwlMaterialRenderState
{
    public static void setup(Material material, @Nullable ClrwlBlendModeOverride blendOverride, List<BufferBlendInformation> bufferBlendOverrides)
    {
        setupTexture(material);
        setupBackfaceCulling(material.backfaceCulling());
        setupPolygonOffset(material.polygonOffset());
        setupDepthTest(material.depthTest());
        setupTransparency(material.transparency(), blendOverride, bufferBlendOverrides);
        setupWriteMask(material.writeMask());
    }

    public static void setupOit(Material material)
    {
        setupTexture(material);
        setupBackfaceCulling(material.backfaceCulling());
        setupPolygonOffset(material.polygonOffset());
        setupDepthTest(material.depthTest());
        WriteMask mask = material.writeMask();
        boolean writeColor = mask.color();
        RenderSystem.colorMask(writeColor, writeColor, writeColor, writeColor);
    }

    private static void setupTexture(Material material)
    {
        Samplers.DIFFUSE.makeActive();
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(material.texture());
        texture.setFilter(material.blur(), material.mipmap());
        int textureId = texture.getId();
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.bindTexture(textureId);
    }

    private static void setupBackfaceCulling(boolean backfaceCulling)
    {
        if (backfaceCulling) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

    }

    private static void setupPolygonOffset(boolean polygonOffset)
    {
        if (polygonOffset) {
            RenderSystem.polygonOffset(-1.0F, -10.0F);
            RenderSystem.enablePolygonOffset();
        } else {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        }

    }

    private static void setupDepthTest(DepthTest depthTest)
    {
        switch (depthTest) {
            case OFF:
                RenderSystem.disableDepthTest();
                break;
            case NEVER:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(512);
                break;
            case LESS:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(513);
                break;
            case EQUAL:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(514);
                break;
            case LEQUAL:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                break;
            case GREATER:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(516);
                break;
            case NOTEQUAL:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(517);
                break;
            case GEQUAL:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(518);
                break;
            case ALWAYS:
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(519);
        }

    }

    private static void setupTransparency(Transparency transparency, @Nullable ClrwlBlendModeOverride blendOverride, List<BufferBlendInformation> bufferBlendOverrides)
    {
        if (blendOverride == null)
        {
            switch (transparency) {
                case OPAQUE:
                    RenderSystem.disableBlend();
                    break;
                case ADDITIVE:
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    break;
                case LIGHTNING:
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    break;
                case GLINT:
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                    break;
                case CRUMBLING:
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    break;
                case TRANSLUCENT, ORDER_INDEPENDENT:
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    break;
            }
        }
        else
        {
            if (blendOverride.blendMode() == null)
            {
                RenderSystem.disableBlend();
            }
            else
            {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(blendOverride.blendMode().srcRgb(),
                                               blendOverride.blendMode().dstRgb(),
                                               blendOverride.blendMode().srcAlpha(),
                                               blendOverride.blendMode().dstAlpha());
            }
        }

        for (var entry : bufferBlendOverrides)
        {
            if (entry.blendMode() == null)
            {
                IrisRenderSystem.disableBufferBlend(entry.index());
            }
            else
            {
                IrisRenderSystem.enableBufferBlend(entry.index());
                IrisRenderSystem.blendFuncSeparatei(entry.index(),
                        entry.blendMode().srcRgb(),
                        entry.blendMode().dstRgb(),
                        entry.blendMode().srcAlpha(),
                        entry.blendMode().dstAlpha());
            }
        }
    }

    private static void setupWriteMask(WriteMask mask)
    {
        RenderSystem.depthMask(mask.depth());
        boolean writeColor = mask.color();
        RenderSystem.colorMask(writeColor, writeColor, writeColor, writeColor);
    }

    public static void reset()
    {
        resetTexture();
        resetBackfaceCulling();
        resetPolygonOffset();
        resetDepthTest();
        resetTransparency();
        resetWriteMask();
    }

    private static void resetTexture()
    {
        Samplers.DIFFUSE.makeActive();
        RenderSystem.setShaderTexture(0, 0);
    }

    private static void resetBackfaceCulling()
    {
        RenderSystem.enableCull();
    }

    private static void resetPolygonOffset()
    {
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
    }

    private static void resetDepthTest()
    {
        RenderSystem.disableDepthTest();
        RenderSystem.depthFunc(515);
    }

    private static void resetTransparency()
    {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void resetWriteMask()
    {
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
    }
}
