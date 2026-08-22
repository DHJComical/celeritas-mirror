package org.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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

    private final SectionLattice lattice;
    // False for orthographic passes (e.g. the Iris shadow pass), where the camera-anchored angular clamp
    // in the occlusion search would wrongly cull geometry outside the player's view cone.
    private final boolean allowFrustumClamping;

    // Non-null for the duration of an in-progress async graph search. Acts as a flag:
    // structural mutations to the lattice (attach/detach/rewire) are forbidden while set,
    // and visibilityData updates are deferred to updateTasks rather than applied immediately.
    private CompletableFuture<VisibleChunkCollector> currentOcclusionFuture;

    @Getter
    @Setter
    private boolean needsUpdate = true;

    @Getter
    private int lastUpdatedFrame;

    private int pendingLastUpdatedFrame;

    @NotNull
    private SectionLattice.VisibilitySnapshot visibilitySnapshot = SectionLattice.VisibilitySnapshot.EMPTY;

    // Snapshot produced by the in-progress search. Written on the async thread; read on the render thread
    // in finishPreviousGraphUpdate() after join() establishes happens-before, then published to visibilitySnapshot.
    @Nullable
    private SectionLattice.VisibilitySnapshot pendingVisibilitySnapshot;

    // Tasks deferred by submitUpdateTask() while an async search is running. Drained on the
    // render thread in finishPreviousGraphUpdate() after join() establishes happens-before.
    private final ArrayDeque<Runnable> updateTasks = new ArrayDeque<>();

    private final ExecutorService asyncGraphExecutor;

    @Nullable
    private final SectionTicker sectionTicker;

    public record RenderListDebugStatistics(Object2IntOpenHashMap<TerrainRenderPass> renderPassCounts, int[] sortingSectionCounts) {
        public String getSortingString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Sorting: ");
            TranslucentQuadAnalyzer.Level[] values = TranslucentQuadAnalyzer.Level.VALUES;
            for (int i = 0; i < values.length; i++) {
                TranslucentQuadAnalyzer.Level level = values[i];
                sb.append(level.name());
                sb.append('=');
                sb.append(sortingSectionCounts[level.ordinal()]);
                if((i + 1) < values.length) {
                    sb.append(", ");
                }
            }

            return sb.toString();
        }
    }

    private RenderListDebugStatistics debugStatistics;

    public RenderListManager(int minSectionY, int maxSectionY, boolean useAsyncGraphSearch, boolean allowFrustumClamping, @Nullable SectionTicker sectionTicker) {
        this.sectionTicker = sectionTicker;
        this.allowFrustumClamping = allowFrustumClamping;

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
        this.lattice = new SectionLattice(minSectionY, maxSectionY);
        this.renderLists = SortedRenderLists.empty();
        this.rebuildLists = ChunkRebuildLists.EMPTY;
    }

    public void startGraphUpdate(Viewport viewport, int frame, int regionIdsLength, float searchDistance, boolean useOcclusionCulling, int targetQueueSize) {
        if (this.currentOcclusionFuture != null) {
            throw new IllegalStateException("Occlusion work in progress while trying to submit next task");
        }

        var visitor = new VisibleChunkCollector(this.lattice, frame, regionIdsLength, targetQueueSize);

        this.lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);

        Supplier<VisibleChunkCollector> occlusionTask = () -> {
            this.pendingVisibilitySnapshot = this.lattice.findVisible(visitor, viewport, searchDistance, regionIdsLength, useOcclusionCulling, this.allowFrustumClamping, frame);

            // WARNING: when asyncGraphExecutor != null, this runs on the async thread.
            // SectionTicker.onRenderListUpdated() must be safe to call off the render thread.
            if (this.sectionTicker != null) {
                this.sectionTicker.onRenderListUpdated(visitor.getSortedRenderLists());
            }

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

            // Publish the finished search's snapshot before advancing lastUpdatedFrame, so any concurrent
            // reader that observes the new frame also observes the snapshot produced for it.
            this.visibilitySnapshot = this.pendingVisibilitySnapshot;
            this.pendingVisibilitySnapshot = null;

            this.currentOcclusionFuture = null;
            this.lastUpdatedFrame = this.pendingLastUpdatedFrame;

            this.debugStatistics = null;
        }

        // Run tasks deferred during the async search. The join() above establishes happens-before,
        // so the async thread's writes to the lattice arrays are visible here.
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

    // Structural mutations to the lattice (adding/removing nodes, rewiring neighbor links)
    // are unsafe while the async search holds a reference to it via SectionLattice.
    private void assertOcclusionNotRunning() {
        if (this.currentOcclusionFuture != null) {
            throw new IllegalStateException("Attempted to update occlusion graph during occlusion!");
        }
    }

    public void attachRenderSection(RenderSection section) {
        this.assertOcclusionNotRunning();

        this.lattice.attach(section);
        this.needsUpdate = true;
    }

    public void detachRenderSection(RenderSection section) {
        this.assertOcclusionNotRunning();

        this.lattice.detach(section);
        this.needsUpdate = true;
    }

    // Runs the task immediately if no async search is active, otherwise defers it to
    // finishPreviousGraphUpdate() to prevent concurrent writes to the lattice arrays.
    private void submitUpdateTask(Runnable runnable) {
        if (this.currentOcclusionFuture == null) {
            runnable.run();
        } else {
            this.updateTasks.add(runnable);
        }
    }

    public void updateSectionMetadata(int x, int y, int z, long packedMetadata) {
        this.submitUpdateTask(() -> {
            if (this.lattice.updateSectionMetadata(x, y, z, packedMetadata)) {
                this.needsUpdate = true;
            }
        });
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.visibilitySnapshot.isSectionVisible(x, y, z, this.lastUpdatedFrame);
    }

    public void tickVisibleRenders() {
        if (this.sectionTicker != null) {
            this.sectionTicker.tickVisibleRenders();
        }
    }


    public RenderListDebugStatistics getDebugStatistics() {
        if (this.debugStatistics == null) {
            this.debugStatistics = computeDebugStatistics();
        }
        return this.debugStatistics;
    }

    public String getTickerDebugString() {
        if (this.sectionTicker == null) {
            return "";
        }
        return this.sectionTicker.getDebugString();
    }

    private RenderListDebugStatistics computeDebugStatistics() {
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
                var iter = Objects.requireNonNull(renderList.sectionsWithGeometryIterator());

                while (iter.hasNext()) {
                    int sectionIndex = iter.nextByteAsInt();

                    if (storage.getSliceMask(sectionIndex) != 0) {
                        numToAdd++;
                    }
                }

                if (numToAdd > 0) {
                    renderPassCounts.addTo(pass, numToAdd);
                }
            }

            if (isSorting) {
                var iter = Objects.requireNonNull(renderList.sectionsWithGeometryIterator());

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

        return new RenderListDebugStatistics(renderPassCounts, sectionCounts);
    }
}
