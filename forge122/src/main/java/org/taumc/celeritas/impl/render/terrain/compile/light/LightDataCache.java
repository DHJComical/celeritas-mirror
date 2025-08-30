package org.taumc.celeritas.impl.render.terrain.compile.light;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.taumc.celeritas.impl.util.EmptyBlockAccess;

public class LightDataCache extends LightDataAccess {
    private final IBlockAccess world;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public LightDataCache(IBlockAccess world) {
        this.world = world;
    }

    protected int compute(int x, int y, int z) {
        BlockPos pos = this.pos.setPos(x, y, z);
        IBlockAccess world = this.world;

        var state = world.getBlockState(pos);

        boolean em;

        try {
            em = state.getPackedLightmapCoords(EmptyBlockAccess.INSTANCE, BlockPos.ORIGIN) == 15728880;
        } catch (Exception e) {
            em = false;
        }

        boolean op = !state.isTranslucent() && state.getLightOpacity(world, pos) == 0;
        boolean fo = state.isOpaqueCube();
        boolean fc = state.isBlockNormalCube();

        int lu = state.getLightValue(world, pos);

        // OPTIMIZE: Do not calculate light data if the block is full and opaque and does not emit light.
        int bl;
        int sl;
        if (fo && lu == 0) {
            bl = 0;
            sl = 0;
        } else {
            // call the vanilla method so mods using custom lightmap logic work correctly
            int packedCoords = state.getPackedLightmapCoords(world, pos);
            bl = LightDataAccess.unpackBlock(packedCoords);
            sl = LightDataAccess.unpackSky(packedCoords);
        }

        // FIX: Do not apply AO from blocks that emit light
        float ao;
        if (lu == 0) {
            ao = state.getAmbientOcclusionLightValue();
        } else {
            ao = 1.0f;
        }

        return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packAO(ao) | packLU(lu) | packSL(sl) | packBL(bl);
    }
}
