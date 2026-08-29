package org.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Owns the render lists of one pass (terrain, or the shadow pass) and the search that produces them. The lattice
 * and search thread are shared between passes through {@link SectionGraph}.
 */
public class RenderListManager {
    @Getter
    @NotNull
    private SortedRenderLists renderLists;
    @Getter
    @NotNull
    private ChunkRebuildLists rebuildLists;

    private final SectionGraph graph;
    private final SectionLattice lattice;
    // Whether this manager produces the shadow pass's lists. The shadow search is orthographic and receiver-driven;
    // the terrain search is the camera-rooted one with the angular frustum clamp.
    private final boolean shadow;
    // Whether this pass's searches run on the shared search thread rather than inline.
    private final boolean async;

    // Non-null for the duration of an in-progress graph search (already completed when the search ran inline).
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

    /**
     * @param graph  the lattice and search thread shared with the other pass's manager
     * @param shadow whether this manager serves the shadow pass
     * @param mode   which passes search asynchronously: {@code EVERYTHING} for both, {@code ONLY_SHADOW} for the
     *               shadow pass alone, {@code NONE} for neither
     */
    public RenderListManager(SectionGraph graph, boolean shadow, AsyncOcclusionMode mode, @Nullable SectionTicker sectionTicker) {
        this.graph = graph;
        this.lattice = graph.getLattice();
        this.shadow = shadow;
        this.async = mode == AsyncOcclusionMode.EVERYTHING || (shadow && mode == AsyncOcclusionMode.ONLY_SHADOW);
        this.sectionTicker = sectionTicker;
        this.renderLists = SortedRenderLists.empty();
        this.rebuildLists = ChunkRebuildLists.empty();
    }

    /**
     * Start the terrain search from the camera section. Only valid on the terrain manager.
     */
    public void startGraphUpdate(Viewport viewport, int frame, int regionIdsLength, float searchDistance, boolean useOcclusionCulling, int targetQueueSize) {
        if (this.shadow) {
            throw new IllegalStateException("startGraphUpdate is for the terrain pass; use startShadowGraphUpdate");
        }

        this.lattice.ensureWindowCovers(viewport.getChunkCoord(), searchDistance);

        this.submitSearch(frame, regionIdsLength, targetQueueSize, visitor ->
                this.lattice.findVisible(visitor, viewport, searchDistance, regionIdsLength, useOcclusionCulling, true, frame));
    }

    /**
     * Start the shadow search. Only valid on the shadow manager, and only after the terrain manager has submitted
     * its search for this frame.
     *
     * @param lightVector unit vector toward the shadow light, or {@code null} to run the frustum-only scan instead
     */
    public void startShadowGraphUpdate(Viewport shadowViewport, int frame, int regionIdsLength, float searchDistance, @Nullable Vector3fc lightVector, int targetQueueSize) {
        if (!this.shadow) {
            throw new IllegalStateException("startShadowGraphUpdate is for the shadow pass; use startGraphUpdate");
        }

        this.lattice.ensureWindowCovers(shadowViewport.getChunkCoord(), searchDistance);

        this.submitSearch(frame, regionIdsLength, targetQueueSize, visitor ->
                this.lattice.findShadowVisible(visitor, shadowViewport, searchDistance, regionIdsLength, lightVector, frame));
    }

    private void submitSearch(int frame, int regionIdsLength, int targetQueueSize,
                              Function<VisibleChunkCollector, SectionLattice.VisibilitySnapshot> search) {
        if (this.currentOcclusionFuture != null) {
            throw new IllegalStateException("Occlusion work in progress while trying to submit next task");
        }

        var visitor = new VisibleChunkCollector(this.lattice, frame, regionIdsLength, targetQueueSize);

        Supplier<VisibleChunkCollector> occlusionTask = () -> {
            this.pendingVisibilitySnapshot = search.apply(visitor);

            // WARNING: when async, this runs on the search thread.
            // SectionTicker.onRenderListUpdated() must be safe to call off the render thread.
            if (this.sectionTicker != null) {
                this.sectionTicker.onRenderListUpdated(visitor.getSortedRenderLists());
            }

            return visitor;
        };

        this.pendingLastUpdatedFrame = frame;
        this.currentOcclusionFuture = this.graph.submit(occlusionTask, this.async);

        if (!this.async) {
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

            this.graph.onSearchJoined();
        }

        // Run tasks deferred while searches were in flight. The join() above establishes happens-before,
        // so the search thread's writes to the lattice arrays are visible here. A no-op while the other
        // pass's search is still running.
        this.graph.runDeferredTasks();
    }

    public void destroy() {
        if (currentOcclusionFuture != null) {
            currentOcclusionFuture.join();
            currentOcclusionFuture = null;
            this.graph.onSearchJoined();
        }
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
