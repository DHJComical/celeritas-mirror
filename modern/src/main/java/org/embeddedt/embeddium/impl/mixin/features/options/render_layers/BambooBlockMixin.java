package org.embeddedt.embeddium.impl.mixin.features.options.render_layers;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.world.level.block. /*? if >=1.20 {*/ BambooStalkBlock /*?} else {*/ /*BambooBlock*//*?}*/.class)
public class BambooBlockMixin extends Block {
    public BambooBlockMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author embeddedt
     * @reason fix bamboo rendering extra inner faces
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        if (adjacentState.is(this) && direction.getAxis().isVertical()) {
            return true;
        } else {
            return super.skipRendering(state, adjacentState, direction);
        }
    }
}
