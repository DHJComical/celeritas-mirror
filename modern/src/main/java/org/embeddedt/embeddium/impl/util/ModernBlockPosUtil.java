package org.embeddedt.embeddium.impl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class ModernBlockPosUtil {
    public static BlockPos.MutableBlockPos setWithOffset(BlockPos.MutableBlockPos dest, BlockPos pos, Direction offset) {
        var offsetVector = offset.getNormal();
        dest.set(pos.getX() + offsetVector.getX(), pos.getY() + offsetVector.getY(), pos.getZ() + offsetVector.getZ());
        return dest;
    }
}
