package org.embeddedt.embeddium.impl.loader.fabric;

//? if fabric {

/*import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;

public class FabricLoaderServices implements LoaderServices {
    @Override
    public int getFluidTintColor(BlockAndTintGetter world, FluidState state, BlockPos pos) {
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(state.getType());
        return handler != null ? handler.getFluidColor(world, pos, state) : -1;
    }
}

*///?}