package org.embeddedt.embeddium.impl.loader.fabric;

//? if fabric {

/*//? if ffapi
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;

public class FabricLoaderServices implements LoaderServices {
    @Override
    public int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos) {
        //? if ffapi {
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(state.getType());
        return handler != null ? (handler.getFluidColor(world, pos, state) | 0xFF000000) : -1;
        //?} else {
        /^// basic vanilla fallback
        if (state.getType() instanceof LavaFluid) {
            return -1;
        } else {
            return BiomeColors.getAverageWaterColor(world, pos) | 0xFF000000;
        }
        ^///?}
    }
}

*///?}