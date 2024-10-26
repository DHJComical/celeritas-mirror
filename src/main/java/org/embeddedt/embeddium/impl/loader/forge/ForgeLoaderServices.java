package org.embeddedt.embeddium.impl.loader.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.render.chunk.light.ForgeLightPipeline;

public final class ForgeLoaderServices implements LoaderServices {
    @Override
    public boolean hasCustomLightPipeline() {
        return ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
    }

    @Override
    public LightPipeline createCustomLightPipeline(LightMode mode, LightDataAccess cache) {
        return mode == LightMode.SMOOTH ? ForgeLightPipeline.smooth(cache) : ForgeLightPipeline.flat(cache);
    }

    @Override
    public int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos) {
        return IClientFluidTypeExtensions.of(state).getTintColor(state, world, pos);
    }

    @Override
    public boolean isCullableAABB(AABB box) {
        return !box.equals(IForgeBlockEntity.INFINITE_EXTENT_AABB);
    }
}
