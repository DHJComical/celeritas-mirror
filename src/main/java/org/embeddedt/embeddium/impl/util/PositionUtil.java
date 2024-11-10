package org.embeddedt.embeddium.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class PositionUtil {
    public static BlockPos.MutableBlockPos setWithOffset(BlockPos.MutableBlockPos dest, BlockPos pos, Direction offset) {
        dest.set(pos.getX() + offset.getStepX(), pos.getY() + offset.getStepY(), pos.getZ() + offset.getStepZ());
        return dest;
    }
}
