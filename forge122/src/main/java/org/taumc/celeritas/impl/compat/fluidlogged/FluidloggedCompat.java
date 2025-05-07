package org.taumc.celeritas.impl.compat.fluidlogged;

import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.api.util.FluidState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.common.Loader;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.world.cloned.CeleritasBlockAccess;

public class FluidloggedCompat {
    /**
     * The mod ID of Fluidlogged API.
     */
    public static final String MODID = "fluidlogged_api";
    public static final boolean IS_LOADED = Loader.isModLoaded(MODID);

    public static FluidState getEmptyFluidState() {
        return FluidState.EMPTY;
    }

    public static void renderFluidState(CeleritasBlockAccess blockAccess, BlockPos pos, IBlockState state, VintageChunkBuildContext context, BlockRendererDispatcher dispatcher) {
        FluidState fluidState = blockAccess.getFluidState(pos);
        if (fluidState != FluidState.EMPTY && (!(state.getBlock() instanceof IFluidloggable) || ((IFluidloggable)state.getBlock()).shouldFluidRender(blockAccess, pos, state, fluidState))) {
            IBlockState renderState = fluidState.getState().getActualState(blockAccess, pos);
            var block = renderState.getBlock();

            for (BlockRenderLayer layer : VintageChunkBuildContext.LAYERS) {
                if (block.canRenderInLayer(renderState, layer)) {
                    ForgeHooksClient.setRenderLayer(layer);
                    var buffer = context.getBufferBuilderForLayer(layer);
                    dispatcher.renderBlock(renderState, pos, blockAccess, buffer);
                }
            }
        }
    }
}
