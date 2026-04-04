package org.embeddedt.embeddium.impl.mixin.core.model.colors;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(BlockColors.class)
public interface BlockColorsAccessor {
    //? if <26.1 {
    @Accessor("blockColors")
    Map<net.minecraft.core.Holder.Reference<Block>, net.minecraft.client.color.block.BlockColor>
    //?} else {
    /*@Accessor("sources")
    Map<Block, List<net.minecraft.client.color.block.BlockTintSource>>
    *///?}
    celeritas$getProviders();
}
