package org.embeddedt.embeddium.impl.render.chunk.lists;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionNode;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RenderListManager {
    @Getter
    @NotNull
    private SortedRenderLists renderLists;
    @Getter
    @NotNull
    private ChunkRebuildLists rebuildLists;

    private final OcclusionCuller occlusionCuller;

    private final Long2ReferenceMap<OcclusionNode> occlusionNodes = new Long2ReferenceOpenHashMap<>();

    private CompletableFuture<VisibleChunkCollector> currentOcclusionFuture;

    @Getter
    @Setter
    private boolean needsUpdate = true;

    @Getter
    private int lastUpdatedFrame;

    private int pendingLastUpdatedFrame;

    private final ArrayDeque<Runnable> updateTasks = new ArrayDeque<>();

    private final ExecutorService asyncGraphExecutor;

    @Nullable
    private final SectionTicker sectionTicker;

    private List<String> renderListDebugStrings;

    public RenderListManager(int minSectionY, int maxSectionY, boolean useAsyncGraphSearch, @Nullable SectionTicker sectionTicker) {
        this.sectionTicker = sectionTicker;

        if (useAsyncGraphSearch) {
            this.asyncGraphExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("Celeritas chunk graph search thread");
                thread.setDaemon(true);
                return thread;
            });
        } else {
            this.asyncGraphExecutor = null;
        }
        this.occlusionCuller = new OcclusionCuller(this.occlusionNodes, minSectionY, maxSectionY);
        this.renderLists = SortedRenderLists.empty();
        this.rebuildLists = ChunkRebuildLists.EMPTY;
    }

    public void startGraphUpdate(Viewport viewport, int frame, float searchDistance, boolean useOcclusionCulling, boolean allowInfiniteUpdateTasks) {
        if (this.currentOcclusionFuture != null) {
            throw new IllegalStateException("Occlusion work in progress while trying to submit next task");
        }

        var visitor = new VisibleChunkCollector(frame, allowInfiniteUpdateTasks);

        Supplier<VisibleChunkCollector> occlusionTask = () -> {
            this.occlusionCuller.findVisible(visitor, viewport, searchDistance, useOcclusionCulling, frame);
            return visitor;
        };

        this.pendingLastUpdatedFrame = frame;

        if (this.asyncGraphExecutor != null) {
            this.currentOcclusionFuture = CompletableFuture.supplyAsync(occlusionTask, this.asyncGraphExecutor);
        } else {
            this.currentOcclusionFuture = CompletableFuture.completedFuture(occlusionTask.get());
            this.finishPreviousGraphUpdate();
        }

        this.needsUpdate = false;
    }

    public void finishPreviousGraphUpdate() {
        if (currentOcclusionFuture != null) {
            VisibleChunkCollector visitor = currentOcclusionFuture.join();

            this.renderLists = visitor.createRenderLists();
            this.rebuildLists = visitor.getRebuildLists();

            if (this.sectionTicker != null) {
                this.sectionTicker.onRenderListUpdated(this.renderLists);
            }

            this.currentOcclusionFuture = null;
            this.lastUpdatedFrame = this.pendingLastUpdatedFrame;

            this.renderListDebugStrings = null;
        }

        Runnable task;

        while ((task = updateTasks.poll()) != null) {
            task.run();
        }
    }

    public void destroy() {
        if (currentOcclusionFuture != null) {
            currentOcclusionFuture.join();
            currentOcclusionFuture = null;
        }

        if (asyncGraphExecutor != null) {
            asyncGraphExecutor.shutdown();

            try {
                if (!asyncGraphExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new InterruptedException();
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("Async graph executor has somehow not shut down");
            }
        }
    }

    private OcclusionNode getOcclusionNode(int x, int y, int z) {
        return this.occlusionNodes.get(PositionUtil.packSection(x, y, z));
    }

    private void connectNeighborNodes(OcclusionNode render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            OcclusionNode adj = this.getOcclusionNode(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(OcclusionNode render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            OcclusionNode adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private void assertOcclusionNotRunning() {
        if (this.currentOcclusionFuture != null) {
            throw new IllegalStateException("Attempted to update occlusion graph during occlusion!");
        }
    }

    public void attachRenderSection(RenderSection section) {
        this.assertOcclusionNotRunning();

        var key = section.positionAsLong();

        OcclusionNode occlusionNode = this.occlusionNodes.get(key);

        if (occlusionNode != null) {
            throw new IllegalStateException("Occlusion node already exists for section " + section);
        }

        var node = new OcclusionNode(section);
        this.occlusionNodes.put(key, node);
        this.connectNeighborNodes(node);
        this.needsUpdate = true;
    }

    public void detachRenderSection(RenderSection section) {
        this.assertOcclusionNotRunning();

        var key = section.positionAsLong();

        OcclusionNode occlusionNode = this.occlusionNodes.remove(key);

        if (occlusionNode == null) {
            throw new IllegalStateException("Occlusion node does not exist for section " + section);
        }

        this.disconnectNeighborNodes(occlusionNode);
        this.needsUpdate = true;
    }

    private void submitUpdateTask(Runnable runnable) {
        if (this.currentOcclusionFuture == null) {
            runnable.run();
        } else {
            this.updateTasks.add(runnable);
        }
    }

    public void updateVisibilityData(int x, int y, int z, long visibilityData) {
        this.submitUpdateTask(() -> {
            var node = this.getOcclusionNode(x, y, z);
            if (node != null) {
                node.setVisibilityData(visibilityData);
                this.needsUpdate = true;
            }
        });
    }

    public boolean isSectionVisible(int x, int y, int z) {
        OcclusionNode render = this.getOcclusionNode(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() >= this.lastUpdatedFrame;
    }

    public void tickVisibleRenders() {
        if (this.sectionTicker != null) {
            this.sectionTicker.tickVisibleRenders();
        }
    }

    public List<String> getRenderListDebugStrings() {
        if (this.renderListDebugStrings == null) {
            this.renderListDebugStrings = computeRenderListDebugStrings();
        }
        return this.renderListDebugStrings;
    }

    private List<String> computeRenderListDebugStrings() {
        Object2IntOpenHashMap<TerrainRenderPass> renderPassCounts = new Object2IntOpenHashMap<>();


        var iterator = renderLists.iterator();

        int[] sectionCounts = new int[TranslucentQuadAnalyzer.Level.VALUES.length];

        boolean isSorting = renderLists.getPasses().stream().anyMatch(TerrainRenderPass::isSorted);

        while (iterator.hasNext()) {
            var renderList = iterator.next();

            if (renderList.getSectionsWithGeometryCount() == 0) {
                continue;
            }

            var region = renderList.getRegion();

            for (TerrainRenderPass pass : region.getPasses()) {
                int numToAdd = 0;
                var storage = region.getStorage(pass);
                var iter = Objects.requireNonNull(renderList.sectionsWithGeometryIterator(false));

                while (iter.hasNext()) {
                    int sectionIndex = iter.nextByteAsInt();
                    var pMeshData = storage.getDataPointer(sectionIndex);

                    if (SectionRenderDataUnsafe.getSliceMask(pMeshData) != 0) {
                        numToAdd++;
                    }
                }

                if (numToAdd > 0) {
                    renderPassCounts.addTo(pass, numToAdd);
                }
            }

            if (isSorting) {
                var iter = Objects.requireNonNull(renderList.sectionsWithGeometryIterator(false));

                while (iter.hasNext()) {
                    int sectionIndex = iter.nextByteAsInt();
                    var section = region.getSection(sectionIndex);

                    // Do not count sections without translucent data
                    if(section == null || section.getTranslucencySortStates().isEmpty()) {
                        continue;
                    }

                    sectionCounts[section.getHighestSortingLevel().ordinal()]++;
                }
            }
        }

        ImmutableList.Builder<String> builder = ImmutableList.builder();

        StringBuilder sb = new StringBuilder("Passes: ");
        var iter = renderPassCounts.object2IntEntrySet().stream().sorted(Comparator.comparingInt(e -> -e.getIntValue())).iterator();

        while (iter.hasNext()) {
            var entry = iter.next();

            sb.append(Character.toUpperCase(entry.getKey().name().charAt(0)));
            sb.append('=');
            sb.append(entry.getIntValue());

            if (iter.hasNext()) {
                sb.append(", ");
            }
        }

        builder.add(sb.toString());

        if (isSorting) {
            sb = new StringBuilder();

            sb.append("Sorting: ");
            TranslucentQuadAnalyzer.Level[] values = TranslucentQuadAnalyzer.Level.VALUES;
            for (int i = 0; i < values.length; i++) {
                TranslucentQuadAnalyzer.Level level = values[i];
                sb.append(level.name());
                sb.append('=');
                sb.append(sectionCounts[level.ordinal()]);
                if((i + 1) < values.length) {
                    sb.append(", ");
                }
            }

            builder.add(sb.toString());
        }

        return builder.build();
    }
}
