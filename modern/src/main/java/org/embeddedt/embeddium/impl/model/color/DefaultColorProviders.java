package org.embeddedt.embeddium.impl.model.color;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.embeddedt.embeddium.api.world.EmbeddiumBlockAndTintGetter;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.api.util.ColorARGB;
//? if <26.1 {
import net.minecraft.client.color.block.BlockColor;
//?} else {
/*import net.minecraft.client.color.block.BlockTintSource;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DefaultColorProviders {
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

    //? if <26.1 {
    public static boolean isVanillaProvider(BlockColor handler) {
        return handler.getClass().getName().startsWith("net.minecraft.");
    }

    public static ColorProvider<BlockState> adapt(BlockColor provider) {
        return new VanillaAdapter(provider);
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
    //?} else {

    /*public static boolean isVanillaProvider(List<BlockTintSource> providers) {
        return providers.stream().allMatch(p -> p.getClass().getName().startsWith("net.minecraft."));
    }

    public static ColorProvider<BlockState> adapt(List<BlockTintSource> providers) {
        return new VanillaAdapter(providers);
    }

    private static final class VanillaAdapter implements ColorProvider<BlockState> {
        private final List<BlockTintSource> providers;
        private final BlockPos.MutableBlockPos lastPos = new BlockPos.MutableBlockPos();
        private final IntList computedTints = new IntArrayList();
        private EmbeddiumBlockAndTintGetter view;

        private VanillaAdapter(List<BlockTintSource> providers) {
            this.providers = providers;
        }

        @Override
        public void getColors(EmbeddiumBlockAndTintGetter view, BlockPos pos, BlockState state, ModelQuadView quad, int[] output) {
            if (this.view != view || !this.lastPos.equals(pos)) {
                this.computeTints(view, pos, state);
            }
            int tintIndex = quad.getColorIndex();
            var computedTints = this.computedTints;
            int tint;
            if (tintIndex >= computedTints.size()) {
                tint = -1;
            } else {
                tint = computedTints.getInt(tintIndex);
            }
            Arrays.fill(output, tint);
        }

        private void computeTints(EmbeddiumBlockAndTintGetter view, BlockPos pos, BlockState state) {
            var providers = this.providers;
            var computedTints = this.computedTints;
            computedTints.clear();
            int sz = providers.size();
            if (sz == 0) {
                net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(state).collectDynamicTintValues(state, view, pos, computedTints);
                for (int i = 0; i < computedTints.size(); i++) {
                    computedTints.set(i, ColorARGB.toABGR(computedTints.getInt(i)));
                }
            } else {
                for (int i = 0; i < sz; i++) {
                    computedTints.add(ColorARGB.toABGR(providers.get(i).colorInWorld(state, view, pos)));
                }
            }
            this.lastPos.set(pos);
            this.view = view;
        }

        @Override
        public void reset() {
            this.view = null;
            this.computedTints.clear();
        }
    }
    *///?}

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
