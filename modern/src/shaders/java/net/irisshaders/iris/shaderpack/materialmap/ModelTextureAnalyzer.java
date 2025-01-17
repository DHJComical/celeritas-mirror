package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModelTextureAnalyzer {
    /**
     * Whether or not to use multithreading for texture analysis. Disabled by default as this appears to dramatically
     * slow down analysis with dynamic model loading.
     */
    private static final boolean USE_MULTITHREADING = false;
    private final List<AnalyzerThread> threads;

    private static List<ImmutableList<BlockState>> getBlockStateGroups(Object2IntMap<BlockState> blockStateIds) {
        Object2ObjectMap<Block, List<BlockState>> statesByBlock = new Object2ObjectOpenHashMap<>();

        // Group states by block. This means that all models for the same block will be retrieved at once, which may
        // help dynamic model loading implementations.
        for (var state : blockStateIds.keySet()) {
            statesByBlock.computeIfAbsent(state.getBlock(), b -> new ArrayList<>(2)).add(state);
        }

        return statesByBlock.values().stream().map(l -> {
            var allStates = l.get(0).getBlock().getStateDefinition().getPossibleStates();
            if (l.size() == allStates.size()) {
                return allStates;
            } else {
                return ImmutableList.copyOf(l);
            }
        }).toList();
    }

    ModelTextureAnalyzer(Object2IntMap<BlockState> blockStateIds) {
        int numThreads = USE_MULTITHREADING ? Runtime.getRuntime().availableProcessors() : 1;

        this.threads = new ArrayList<>(numThreads);

        var blockStateGroups = getBlockStateGroups(blockStateIds);

        int numGroupsPerThread = (blockStateGroups.size() + numThreads - 1) / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int startIndex = i * numGroupsPerThread;
            int endIndex = Math.min(blockStateGroups.size(), (i+1) * numGroupsPerThread);
            var thread = new AnalyzerThread(blockStateIds, blockStateGroups.subList(startIndex, endIndex));
            thread.setName("Celeritas Texture Material Analyzer #" + (i + 1));
            this.threads.add(thread);
        }
    }

    private static int computePropertiesForState(BlockState state) {
        int props = 0;
        if (state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            props |= 1;
        }
        return props;
    }

    private static final Int2ObjectMap<Object2IntFunction<BlockState>> FIXED_ID_CACHE = new Int2ObjectOpenHashMap<>();

    private static Object2IntFunction<BlockState> fixedId(int id) {
        return FIXED_ID_CACHE.computeIfAbsent(id, i -> state -> i);
    }

    private static Object2IntFunction<BlockState> forPropertyMap(Int2IntMap byPropertyMap) {
        return state -> {
            if (!(state instanceof BlockState)) {
                return -1;
            }

            int props = computePropertiesForState((BlockState)state);

            int blockId = byPropertyMap.getOrDefault(props, -1);

            if (blockId != -1) {
                return blockId;
            }

            // Compute the closest bitwise match and use that

            int bestBlockId = -1, bestDifferenceCount = Integer.MAX_VALUE;

            for (var entry : Int2IntMaps.fastIterable(byPropertyMap)) {
                int diff = Integer.bitCount(entry.getIntKey() ^ props);
                if (diff < bestDifferenceCount) {
                    bestBlockId = entry.getIntKey();
                    bestDifferenceCount = diff;
                }
            }

            return bestBlockId;
        };
    }
    private static final Object2IntFunction<BlockState> NONE = fixedId(-1);

    public static int fetchBlockIdForTexturedState(Object2ObjectMap<TextureAtlasSprite, Object2IntFunction<BlockState>> fallbackMaterialMap, BlockState state, TextureAtlasSprite sprite) {
        var byPropertyMap = fallbackMaterialMap.get(sprite);

        if (byPropertyMap == null) {
            return -1;
        }

        return byPropertyMap.applyAsInt(state);
    }

    private static void mergeIntoMainMap(Object2ObjectMap<TextureAtlasSprite, Int2ObjectMap<Int2IntMap>> dest, Object2ObjectMap<TextureAtlasSprite, Int2ObjectMap<Int2IntMap>> src) {
        src.forEach((sprite, votesByProperty) -> {
            dest.merge(sprite, votesByProperty, (oldVotesByProperty, newVotesByProperty) -> {
                Int2ObjectMaps.fastForEach(newVotesByProperty, propertyEntry -> {
                    var properties = propertyEntry.getIntKey();
                    var votes = propertyEntry.getValue();
                    oldVotesByProperty.merge(properties, votes, (oldVotes, newVotes) -> {
                        for (var entry : Int2IntMaps.fastIterable(newVotes)) {
                            oldVotes.merge(entry.getIntKey(), entry.getIntValue(), Integer::sum);
                        }
                        return oldVotes;
                    });
                });

                return oldVotesByProperty;
            });
        });
    }

    public static @Nullable Object2ObjectMap<TextureAtlasSprite, Object2IntFunction<BlockState>> runAnalysisSync(@Nullable Object2IntMap<BlockState> blockStateIds) {
        Stopwatch watch = Stopwatch.createStarted();
        var result = runAnalysis(blockStateIds).join();
        watch.stop();
        Iris.logger.info("Analyzed texture materials in {}", watch);
        return result;
    }

    public static CompletableFuture<@Nullable Object2ObjectMap<TextureAtlasSprite, Object2IntFunction<BlockState>>> runAnalysis(@Nullable Object2IntMap<BlockState> blockStateIds) {
        if (blockStateIds == null) {
            return CompletableFuture.completedFuture(null);
        }

        var analyzer = new ModelTextureAnalyzer(blockStateIds);

        // Start the analysis running in the background
        analyzer.threads.forEach(Thread::start);

        var threadedStageCompletion = CompletableFuture.allOf(analyzer.threads.stream().map(AnalyzerThread::getCompletionFuture).toArray(CompletableFuture[]::new));
        // Wait for all the threads to terminate
        return threadedStageCompletion.thenApply(v -> {
            Object2ObjectOpenHashMap<TextureAtlasSprite, Int2ObjectMap<Int2IntMap>> mergedMap = new Object2ObjectOpenHashMap<>();
            for (AnalyzerThread t : analyzer.threads) {
                mergeIntoMainMap(mergedMap, t.votingMap);
            }

            Object2ObjectMap<TextureAtlasSprite, Object2IntFunction<BlockState>> finalMap = new Object2ObjectOpenHashMap<>();

            var entryComparator = Comparator.comparingInt(Int2IntMap.Entry::getIntValue);

            for (var entry : Object2ObjectMaps.fastIterable(mergedMap)) {
                Int2IntMap materialByProperty = new Int2IntArrayMap();

                for (var entryByProperty : Int2ObjectMaps.fastIterable(entry.getValue())) {
                    Int2IntMap.Entry highestEntry;

                    if (entry.getValue().size() == 1) {
                        highestEntry = entryByProperty.getValue().int2IntEntrySet().iterator().next();
                    } else {
                        highestEntry = entryByProperty.getValue().int2IntEntrySet().stream().max(entryComparator).orElseThrow();
                    }

                    materialByProperty.put(entryByProperty.getIntKey(), highestEntry.getIntKey());
                }

                if (materialByProperty.size() == 0) {
                    finalMap.put(entry.getKey(), NONE);
                } else if (materialByProperty.size() == 1 || materialByProperty.values().intStream().distinct().count() == 1) {
                    finalMap.put(entry.getKey(), fixedId(materialByProperty.values().iterator().nextInt()));
                } else {
                    finalMap.put(entry.getKey(), forPropertyMap(materialByProperty));
                }
            }

            return finalMap;
        });
    }

    static class AnalyzerThread extends Thread {
        private final SingleThreadedRandomSource random = new SingleThreadedRandomSource(42L);
        private final Object2ObjectOpenHashMap<TextureAtlasSprite, Int2ObjectMap<Int2IntMap>> votingMap = new Object2ObjectOpenHashMap<>();
        private final BlockModelShaper blockModelShaper = Minecraft.getInstance().getModelManager().getBlockModelShaper();
        private final Object2IntMap<BlockState> blockStateIds;
        private final List<ImmutableList<BlockState>> tasks;
        private final CompletableFuture<Void> completableFuture;
        private final ReferenceOpenHashSet<BakedQuad> seenQuads = new ReferenceOpenHashSet<>();

        AnalyzerThread(Object2IntMap<BlockState> blockStateIds, List<ImmutableList<BlockState>> tasks) {
            this.blockStateIds = blockStateIds;
            this.tasks = tasks;
            this.completableFuture = new CompletableFuture<>();
        }

        @Override
        public void run() {
            try {
                this.tasks.forEach(this::voteOnStates);
            } catch(Throwable e) {
                Iris.logger.error("Exception encountered during texture analysis", e);
            } finally {
                this.completableFuture.complete(null);
            }
        }

        public CompletableFuture<Void> getCompletionFuture() {
            return this.completableFuture;
        }

        private void voteOnStates(List<BlockState> states) {
            int sz = states.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < sz; i++) {
                var state = states.get(i);

                var model = blockModelShaper.getBlockModel(state);

                var materialId = blockStateIds.getInt(state);

                var stateProps = computePropertiesForState(state);

                try {
                    for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
                        conductVoting(model, state, stateProps, direction, materialId);
                    }

                    conductVoting(model, state, stateProps, null, materialId);
                } catch (Exception ignored) {
                    // No problem, we'll just skip this block.
                    break;
                }
            }

            seenQuads.clear();
        }

        private void conductVoting(BakedModel model, BlockState state, int stateProps, @Nullable Direction direction, int vote) {
            random.setSeed(42L);
            //? if forge
            List<BakedQuad> quadList = model.getQuads(state, direction, random, net.minecraftforge.client.model.data.ModelData.EMPTY, null);
            //? if neoforge
            /*List<BakedQuad> quadList = model.getQuads(state, direction, random, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);*/
            //? if fabric
            /*List<BakedQuad> quadList = model.getQuads(state, direction, random);*/

            var votingMap = this.votingMap;

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < quadList.size(); i++) {
                var quad = quadList.get(i);

                // Do not allow the same quad to vote multiple times for different states. This helps prevent
                // blocks with high quantities of blockstates from skewing the vote. We need to do this, rather than
                // merging all the votes for a given block, as we need to allow actual visual variations to be counted.
                if (!seenQuads.add(quad)) {
                    continue;
                }

                var sprite = quad.getSprite();

                if (sprite != null) {
                    var votes = votingMap.get(sprite);
                    if (votes == null) {
                        votes = new Int2ObjectArrayMap<>();
                        votingMap.put(sprite, votes);
                    }

                    var votesByProperty = votes.get(stateProps);
                    if (votesByProperty == null) {
                        votesByProperty = new Int2IntArrayMap();
                        votes.put(stateProps, votesByProperty);
                    }

                    votesByProperty.mergeInt(vote, 1, Integer::sum);
                }
            }
        }
    }
}
