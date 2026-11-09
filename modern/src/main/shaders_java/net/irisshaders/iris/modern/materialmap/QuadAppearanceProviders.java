package net.irisshaders.iris.modern.materialmap;

//? if forgelike && <26.1
import net.irisshaders.iris.compat.framedblocks.FramedBlocksAppearance;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The {@link QuadAppearanceProvider}s for blocks that need one, each registered with the blocks it applies to. The
 * first registration whose predicate matches a block wins.
 */
public final class QuadAppearanceProviders {
    private record Registration(Predicate<Block> appliesTo, Supplier<QuadAppearanceProvider> factory) {
    }

    private static final List<Registration> REGISTRATIONS = new CopyOnWriteArrayList<>();

    static {
        //? if forgelike && <26.1
        FramedBlocksAppearance.register();
    }

    private QuadAppearanceProviders() {
    }

    /** {@code factory} is called once per meshing thread, so the providers it makes may keep unsynchronized state. */
    public static void register(Predicate<Block> appliesTo, Supplier<QuadAppearanceProvider> factory) {
        REGISTRATIONS.add(new Registration(appliesTo, factory));
    }

    public static Lookup createLookup() {
        return new Lookup(REGISTRATIONS);
    }

    /** One meshing thread's providers, with the provider each block resolved to cached. Not thread-safe. */
    public static final class Lookup {
        private static final QuadAppearanceProvider NONE = new QuadAppearanceProvider() {
            @Override
            public @Nullable BlockState findAppearance(BlockRenderContext ctx, @Nullable Direction side, BakedQuadView quad) {
                return null;
            }

            @Override
            public BlockState shapeState() {
                throw new UnsupportedOperationException();
            }
        };

        private final Registration[] registrations;
        private final QuadAppearanceProvider[] providers;
        private final Reference2ObjectOpenHashMap<Block, QuadAppearanceProvider> byBlock = new Reference2ObjectOpenHashMap<>();

        private Block lastBlock;
        private QuadAppearanceProvider lastProvider;

        private Lookup(List<Registration> registrations) {
            this.registrations = registrations.toArray(new Registration[0]);
            this.providers = new QuadAppearanceProvider[this.registrations.length];
        }

        public @Nullable QuadAppearanceProvider get(Block block) {
            if (this.registrations.length == 0) {
                return null;
            }
            if (block != this.lastBlock) {
                QuadAppearanceProvider provider = this.byBlock.get(block);
                if (provider == null) {
                    provider = this.find(block);
                    this.byBlock.put(block, provider);
                }
                this.lastBlock = block;
                this.lastProvider = provider;
            }
            return this.lastProvider != NONE ? this.lastProvider : null;
        }

        private QuadAppearanceProvider find(Block block) {
            for (int i = 0; i < this.registrations.length; i++) {
                if (this.registrations[i].appliesTo().test(block)) {
                    if (this.providers[i] == null) {
                        this.providers[i] = this.registrations[i].factory().get();
                    }
                    return this.providers[i];
                }
            }
            return NONE;
        }
    }
}
