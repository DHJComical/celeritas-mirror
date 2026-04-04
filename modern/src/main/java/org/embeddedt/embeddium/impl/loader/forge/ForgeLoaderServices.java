package org.embeddedt.embeddium.impl.loader.forge;

//? if forge {
import net.minecraftforge.common.ForgeConfig;
//? if >=1.19
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
//?} else if neoforge {
/*import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
*///?}

//? if forgelike {
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import org.embeddedt.embeddium.impl.loader.common.LoaderServices;

public final class ForgeLoaderServices implements LoaderServices {
    @Override
    public int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos) {
        //? if >=26.1 {
        /*var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
        var tintSource = model.fluidTintSource();
        return tintSource != null ? tintSource.colorInWorld(state, world.getBlockState(pos), world, pos) : -1;
        *///?} else if >=1.19 {
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