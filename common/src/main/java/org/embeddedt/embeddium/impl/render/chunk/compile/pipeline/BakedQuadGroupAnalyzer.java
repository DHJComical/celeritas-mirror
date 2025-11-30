package org.embeddedt.embeddium.impl.render.chunk.compile.pipeline;

import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;

import java.util.List;

public class BakedQuadGroupAnalyzer {
    /**
     * Tracks whether the MC-138211 quad reorienting fix should be applied during emission of quad geometry.
     * This fix must be disabled with certain modded models that use superimposed quads, as it can alter the triangulation
     * of some layers but not others, resulting in Z-fighting.
     */
    public static final int USE_REORIENTING = 0x1;
    public static final int USE_RENDER_PASS_OPTIMIZATION = 0x2;
    public static final int USE_ALL_THINGS = 0xFFFFFFFF;

    private int defaultRenderingFlags = 0;
    private int unassignedFaceRenderingFlags = 0;

    private static int computeLightFlagMask(BakedQuadView quad) {
        int flag = 0;

        if (quad.hasAmbientOcclusion()) {
            flag |= 1;
        }

        if (quad.hasShade()) {
            flag |= 2;
        }

        return flag;
    }

    private SpriteTransparencyLevel getQuadTransparencyLevel(BakedQuadView quad) {
        if ((quad.getFlags() & ModelQuadFlags.IS_PASS_OPTIMIZABLE) == 0 || quad.celeritas$getSprite() == null) {
            return SpriteTransparencyLevel.TRANSLUCENT;
        }

        return SpriteTransparencyLevel.Holder.getTransparencyLevel(quad.celeritas$getSprite());
    }

    public void setDefaultRenderingFlags(int flags) {
        this.defaultRenderingFlags = flags;
        this.unassignedFaceRenderingFlags = flags;
    }

    public int getFlagsForRendering(ModelQuadFacing facing, List<? extends BakedQuadView> quads) {
        int quadRenderingFlags = facing == ModelQuadFacing.UNASSIGNED ? this.unassignedFaceRenderingFlags : this.defaultRenderingFlags;

        int quadsSize = quads.size();

        // By definition, singleton or empty lists of quads have a common config. Only check larger lists
        if (quadsSize >= 2) {
            // Disable reorienting if quads use different light configurations, as otherwise layered quads
            // may be triangulated differently from others in the stack, and that will cause z-fighting.
            int flagMask = -1;

            SpriteTransparencyLevel highestSeenLevel = SpriteTransparencyLevel.OPAQUE;

            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < quadsSize; i++) {
                var quad = BakedQuadView.of(quads.get(i));

                int newFlag = computeLightFlagMask(quad);
                if (flagMask == -1) {
                    flagMask = newFlag;
                } else if (newFlag != flagMask) {
                    // Disable reorienting
                    quadRenderingFlags &= ~USE_REORIENTING;
                }

                SpriteTransparencyLevel level = getQuadTransparencyLevel(quad);

                if (level.ordinal() < highestSeenLevel.ordinal()) {
                    // Downgrading will result in the quads being rendered in the wrong order, disable
                    quadRenderingFlags &= ~USE_RENDER_PASS_OPTIMIZATION;
                } else {
                    highestSeenLevel = level;
                }
            }
        }

        // Disable any flags in the null cullface that were disabled in other cullfaces
        this.unassignedFaceRenderingFlags &= quadRenderingFlags;

        return quadRenderingFlags;
    }

    public static Material chooseOptimalMaterial(int analyzerFlags, Material defaultMaterial, RenderPassConfiguration<?> renderPassConfiguration, BakedQuadView quad) {
        if (defaultMaterial == renderPassConfiguration.defaultSolidMaterial() || (analyzerFlags & USE_RENDER_PASS_OPTIMIZATION) == 0 || (quad.getFlags() & ModelQuadFlags.IS_PASS_OPTIMIZABLE) == 0 || quad.celeritas$getSprite() == null) {
            // No improvement possible
            return defaultMaterial;
        }

        SpriteTransparencyLevel level = SpriteTransparencyLevel.Holder.getTransparencyLevel(quad.celeritas$getSprite());

        if (level == SpriteTransparencyLevel.OPAQUE) {
            // Can use solid with no visual difference
            return renderPassConfiguration.defaultSolidMaterial();
        } else if (level == SpriteTransparencyLevel.TRANSPARENT && defaultMaterial == renderPassConfiguration.defaultTranslucentMaterial()) {
            // Can use cutout_mipped with no visual difference
            return renderPassConfiguration.defaultCutoutMippedMaterial();
        } else {
            // Have to use default
            return defaultMaterial;
        }
    }
}
