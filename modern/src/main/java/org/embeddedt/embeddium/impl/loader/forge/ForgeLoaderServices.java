package org.embeddedt.embeddium.impl.loader.forge;

//? if forge {
import net.minecraftforge.common.ForgeConfig;
//? if >=1.19
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
//?} else if neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeConfig;
*///?}

//? if forgelike {
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
//? if >=1.19
import org.embeddedt.embeddium.impl.modern.render.chunk.light.ForgeLightPipeline;

public final class ForgeLoaderServices implements LoaderServices {
    @Override
    public boolean hasCustomLightPipeline() {
        //? if forge && >=1.19 {
        return ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
        //?} else if neoforge {
        /*return NeoForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
        *///?} else
        /*return false;*/
    }

    //? if >=1.19 {
    @Override
    public LightPipeline createCustomLightPipeline(LightMode mode, LightDataAccess cache) {
        return mode == LightMode.SMOOTH ? ForgeLightPipeline.smooth(cache) : ForgeLightPipeline.flat(cache);
    }
    //?}

    @Override
    public int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos) {
        //? if >=1.19 {
        return IClientFluidTypeExtensions.of(state).getTintColor(state, world, pos);
        //?} else
        /*return state.getType().getAttributes().getColor(world, pos);*/
    }

    @Override
    public boolean isCullableAABB(AABB box) {
        //? if forge && <1.18
        /*return !box.equals(net.minecraftforge.common.extensions.IForgeTileEntity.INFINITE_EXTENT_AABB);*/
        //? if forge && >=1.18
        return !box.equals(net.minecraftforge.common.extensions.IForgeBlockEntity.INFINITE_EXTENT_AABB);
        //? if neoforge && >=1.20.6
        /*return !box.equals(AABB.INFINITE);*/
        //? if neoforge && <1.20.6
        /*return !box.equals(net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension.INFINITE_EXTENT_AABB);*/
    }
}
//?}