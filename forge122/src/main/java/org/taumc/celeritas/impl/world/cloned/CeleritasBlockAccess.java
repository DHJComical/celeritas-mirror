package org.taumc.celeritas.impl.world.cloned;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.taumc.celeritas.impl.compat.fluidlogged.FluidloggedBlockAccess;

/**
 * Contains extensions to the vanilla {@link IBlockAccess}.
 */
public interface CeleritasBlockAccess extends IBlockAccess, FluidloggedBlockAccess {
    int getBlockTint(BlockPos pos, BiomeColorHelper.ColorResolver resolver);
}
