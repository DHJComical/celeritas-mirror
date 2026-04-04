package org.embeddedt.embeddium.impl.model.quad;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * A light data cache which uses a flat-array to store the light data for the blocks in a given chunk and its direct
 * neighbors. This is considerably faster than using a hash table to lookup values for a given block position and
 * can be re-used by {@link WorldSlice} to avoid allocations.
 */
public class ArrayLightDataCache extends LightDataAccess {
    private final BlockAndTintGetter world;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public ArrayLightDataCache(BlockAndTintGetter world) {
        this.world = world;
    }

    protected int compute(int x, int y, int z) {
        BlockPos pos = this.pos.set(x, y, z);
        BlockAndTintGetter world = this.world;

        BlockState state = world.getBlockState(pos);

        boolean em = state.emissiveRendering(/*? if >=1.16 {*/world, pos/*?}*/);
        boolean op = state.isViewBlocking(world, pos) && (
                //? if <26.1 {
                state.getLightBlock(world, pos) != 0
                //?} else
                /*state.getLightDampening() != 0*/
        );
        boolean fo = state.isSolidRender(/*? if <1.21.11 {*/world, pos/*?}*/);
        boolean fc = state.isCollisionShapeFullBlock(world, pos);

        int lu = WorldUtil.getLightEmission(state, world, pos);

        // OPTIMIZE: Do not calculate light data if the block is full and opaque and does not emit light.
        int bl;
        int sl;
        if (fo && lu == 0) {
            bl = 0;
            sl = 0;
        } else {
            // calculate light data using custom approach for emissive blocks, and vanilla otherwise
            if (em) {
                bl = world.getBrightness(LightLayer.BLOCK, pos);
                sl = world.getBrightness(LightLayer.SKY, pos);
            } else {
                // call the vanilla method so mods using custom lightmap logic work correctly
                //? if <26.1 {
                int packedCoords = LevelRenderer.getLightColor(/*? if >=1.21.11 {*/ /*LevelRenderer.BrightnessGetter.DEFAULT, *//*?}*/ world, state, pos);
                bl = net.minecraft.client.renderer.LightTexture.block(packedCoords);
                sl = net.minecraft.client.renderer.LightTexture.sky(packedCoords);
                //?} else {
                /*int packedCoords = LevelRenderer.getLightCoords(LevelRenderer.BrightnessGetter.DEFAULT, world, state, pos);
                bl = net.minecraft.util.LightCoordsUtil.block(packedCoords);
                sl = net.minecraft.util.LightCoordsUtil.sky(packedCoords);
                *///?}
            }
        }

        // FIX: Do not apply AO from blocks that emit light
        float ao;
        if (lu == 0) {
            ao = state.getShadeBrightness(world, pos);
        } else {
            ao = 1.0f;
        }

        return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packAO(ao) | packLU(lu) | packSL(sl) | packBL(bl);
    }
}