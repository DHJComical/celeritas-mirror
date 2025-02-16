package org.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class RenderListManager {
    private static final boolean ENABLE_ASYNC_GRAPH_SEARCH = false;
    private static final ExecutorService ASYNC_GRAPH_SEARCH_EXECUTOR = Executors.newSingleThreadExecutor();

    @Getter
    @NotNull
    private SortedRenderLists renderLists;
    @Getter
    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    private final OcclusionCuller occlusionCuller;

    private CompletableFuture<VisibleChunkCollector> currentOcclusionFuture;

    @Getter
    @Setter
    private boolean needsUpdate = true;

    @Getter
    private int lastUpdatedFrame;

    private int pendingLastUpdatedFrame;

    public RenderListManager(Long2ReferenceMap<RenderSection> sections, int minSectionY, int maxSectionY) {
        this.occlusionCuller = new OcclusionCuller(sections, minSectionY, maxSectionY);
        this.renderLists = SortedRenderLists.empty();
        this.rebuildLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            this.rebuildLists.put(type, new ArrayDeque<>());
        }
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

        if (ENABLE_ASYNC_GRAPH_SEARCH) {
            this.currentOcclusionFuture = CompletableFuture.supplyAsync(occlusionTask, ASYNC_GRAPH_SEARCH_EXECUTOR);
        } else {
            this.currentOcclusionFuture = CompletableFuture.completedFuture(occlusionTask.get());
            this.finishPreviousGraphUpdate();
        }

        this.needsUpdate = false;
    }

    public void finishPreviousGraphUpdate() {
        if (currentOcclusionFuture == null) {
            return;
        }

        VisibleChunkCollector visitor = currentOcclusionFuture.join();

        this.renderLists = visitor.createRenderLists();
        this.rebuildLists = visitor.getRebuildLists();

        this.currentOcclusionFuture = null;
        this.lastUpdatedFrame = this.pendingLastUpdatedFrame;
    }

    public void destroy() {
        if (currentOcclusionFuture != null) {
            currentOcclusionFuture.join();
            currentOcclusionFuture = null;
        }
    }
}
