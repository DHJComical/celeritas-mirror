package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.objects.*;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
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

    private static void mergeIntoMainMap(Object2ObjectMap<TextureAtlasSprite, Int2IntMap> dest, Object2ObjectMap<TextureAtlasSprite, Int2IntMap> src) {
        src.forEach((sprite, votes) -> {
            dest.merge(sprite, votes, (oldVotes, newVotes) -> {
                for (var entry : Int2IntMaps.fastIterable(newVotes)) {
                    oldVotes.merge(entry.getIntKey(), entry.getIntValue(), Integer::sum);
                }
                return oldVotes;
            });
        });
    }

    public static @Nullable Object2IntMap<TextureAtlasSprite> runAnalysisSync(@Nullable Object2IntMap<BlockState> blockStateIds) {
        Stopwatch watch = Stopwatch.createStarted();
        var result = runAnalysis(blockStateIds).join();
        watch.stop();
        Iris.logger.info("Analyzed texture materials in {}", watch);
        return result;
    }

    public static CompletableFuture<@Nullable Object2IntMap<TextureAtlasSprite>> runAnalysis(@Nullable Object2IntMap<BlockState> blockStateIds) {
        if (blockStateIds == null) {
            return CompletableFuture.completedFuture(null);
        }

        var analyzer = new ModelTextureAnalyzer(blockStateIds);

        // Start the analysis running in the background
        analyzer.threads.forEach(Thread::start);

        var threadedStageCompletion = CompletableFuture.allOf(analyzer.threads.stream().map(AnalyzerThread::getCompletionFuture).toArray(CompletableFuture[]::new));
        // Wait for all the threads to terminate
        return threadedStageCompletion.thenApply(v -> {
            Object2ObjectOpenHashMap<TextureAtlasSprite, Int2IntMap> mergedMap = new Object2ObjectOpenHashMap<>();
            for (AnalyzerThread t : analyzer.threads) {
                mergeIntoMainMap(mergedMap, t.votingMap);
            }

            Object2IntMap<TextureAtlasSprite> finalMap = new Object2IntOpenHashMap<>();

            var entryComparator = Comparator.comparingInt(Int2IntMap.Entry::getIntValue);

            for (var entry : Object2ObjectMaps.fastIterable(mergedMap)) {
                Int2IntMap.Entry highestEntry;

                if (entry.getValue().size() == 1) {
                    highestEntry = entry.getValue().int2IntEntrySet().iterator().next();
                } else {
                    highestEntry = entry.getValue().int2IntEntrySet().stream().max(entryComparator).orElseThrow();
                }

                finalMap.put(entry.getKey(), highestEntry.getIntKey());
            }

            return finalMap;
        });
    }

    static class AnalyzerThread extends Thread {
        private final SingleThreadedRandomSource random = new SingleThreadedRandomSource(42L);
        private final Object2ObjectMap<TextureAtlasSprite, Int2IntMap> votingMap = new Object2ObjectOpenHashMap<>();
        private final BlockModelShaper blockModelShaper = Minecraft.getInstance().getModelManager().getBlockModelShaper();
        private final Object2IntMap<BlockState> blockStateIds;
        private final List<ImmutableList<BlockState>> tasks;
        private final CompletableFuture<Void> completableFuture;

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

                try {
                    for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
                        conductVoting(model, state, direction, materialId);
                    }

                    conductVoting(model, state, null, materialId);
                } catch (Exception ignored) {
                    // No problem, we'll just skip this block.
                    break;
                }
            }
        }

        private void conductVoting(BakedModel model, BlockState state, @Nullable Direction direction, int vote) {
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
                var sprite = quad.getSprite();

                if (sprite != null) {
                    var votes = votingMap.get(sprite);
                    if (votes == null) {
                        votes = new Int2IntArrayMap();
                        votingMap.put(sprite, votes);
                    }

                    votes.mergeInt(vote, 1, Integer::sum);
                }
            }
        }
    }
}
