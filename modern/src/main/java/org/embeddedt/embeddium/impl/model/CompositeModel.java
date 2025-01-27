package org.embeddedt.embeddium.impl.model;

import net.minecraft.client.resources.model.BakedModel;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface CompositeModel {
    @Nullable
    Iterable<BakedModel> celeritas$getInnerModels(BlockState state, /*$ rng >>*/ RandomSource rand);
}
