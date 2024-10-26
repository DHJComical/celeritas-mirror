package org.embeddedt.embeddium.impl.model.color;

import org.embeddedt.embeddium.api.world.EmbeddiumBlockAndTintGetter;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.blender.BlendedColorProvider;
import org.embeddedt.embeddium.api.util.ColorARGB;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;

public class DefaultColorProviders {
    public static ColorProvider<BlockState> adapt(BlockColor provider) {
        return new VanillaAdapter(provider);
    }

    public static ColorProvider<FluidState> getFluidProvider() {
        return new ForgeFluidAdapter();
    }

    /**
     * Adapter for {@link BlendedColorProvider} that accepts a BiomeColors method reference (or any biome color
     * getter using a similar interface) and uses it to perform per-vertex biome blending.
     */
    public static class VertexBlendedBiomeColorAdapter<T> extends BlendedColorProvider<T> {
        private final VanillaBiomeColor vanillaGetter;

        /**
         * Interface whose signature matches those of the getAverageXYZColor functions in BiomeColors, allowing
         * for method references to be passed to {@link VertexBlendedBiomeColorAdapter}'s constructor.
         */
        @FunctionalInterface
        public interface VanillaBiomeColor {
            int getAverageColor(BlockAndTintGetter getter, BlockPos pos);
        }

        public VertexBlendedBiomeColorAdapter(VanillaBiomeColor vanillaGetter) {
            this.vanillaGetter = vanillaGetter;
        }

        @Override
        protected int getColor(EmbeddiumBlockAndTintGetter world, BlockPos pos) {
            return vanillaGetter.getAverageColor(world, pos);
        }
    }

    private static class VanillaAdapter implements ColorProvider<BlockState> {
        private final BlockColor provider;

        private VanillaAdapter(BlockColor provider) {
            this.provider = provider;
        }

        @Override
        public void getColors(EmbeddiumBlockAndTintGetter view, BlockPos pos, BlockState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output, ColorARGB.toABGR(this.provider.getColor(state, view, pos, quad.getColorIndex())));
        }
    }

    private static class ForgeFluidAdapter implements ColorProvider<FluidState> {
        @Override
        public void getColors(EmbeddiumBlockAndTintGetter view, BlockPos pos, FluidState state, ModelQuadView quad, int[] output) {
            if (view == null || state == null) {
                Arrays.fill(output, -1);
                return;
            }

            Arrays.fill(output, ColorARGB.toABGR(LoaderServices.INSTANCE.getFluidTintColor(view, state, pos)));
        }
    }
}
