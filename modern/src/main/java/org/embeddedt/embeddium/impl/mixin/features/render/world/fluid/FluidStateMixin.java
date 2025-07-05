package org.embeddedt.embeddium.impl.mixin.features.render.world.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = FluidState.class, priority = 500)
public abstract class FluidStateMixin {
    @Shadow
    public abstract Fluid getType();

    /**
     * @author embeddedt
     * @reason reuse an incoming mutable block pos when possible to avoid allocations
     */
    @Overwrite
    public boolean shouldRenderBackwardUpFace(BlockGetter level, BlockPos pos) {
        if (pos instanceof BlockPos.MutableBlockPos mutable) {
            // Save old position, use as cursor, restore old position
            int oldX = pos.getX(), oldY = pos.getY(), oldZ = pos.getZ();
            boolean result = shouldRenderBackwardUpFaceMutable(level, oldX, oldY, oldZ, mutable);
            mutable.set(oldX, oldY, oldZ);
            return result;
        } else {
            return shouldRenderBackwardUpFaceMutable(level, pos.getX(), pos.getY(), pos.getZ(), new BlockPos.MutableBlockPos());
        }
    }

    @Unique
    private boolean shouldRenderBackwardUpFaceMutable(BlockGetter level, int x, int y, int z, BlockPos.MutableBlockPos cursor) {
        // [VanillaCopy]
        for(int i = -1; i <= 1; ++i) {
            for(int j = -1; j <= 1; ++j) {
                cursor.set(x + i, y, z + j);
                FluidState fluidstate = level.getFluidState(cursor);
                if (!fluidstate.getType().isSame(this.getType()) && !level.getBlockState(cursor).isSolidRender(/*? if <1.21.2 {*/level, cursor/*?}*/)) {
                    return true;
                }
            }
        }

        return false;
    }
}
