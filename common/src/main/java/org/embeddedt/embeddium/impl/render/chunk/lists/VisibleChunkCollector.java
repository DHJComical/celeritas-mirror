package org.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Queue;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionNode;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

public class VisibleChunkCollector implements OcclusionCuller.Visitor {
    @Getter(AccessLevel.PACKAGE)
    private final ObjectArrayList<ChunkRenderList> sortedRenderLists;
    private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> sortedRebuildLists;
    private final int[] rebuildQueueOverflowCounts;
    private final ChunkRenderList[] renderListsByRegion;

    private final int frame;

    private final boolean ignoreQueueSizeLimit;

    private boolean hasAdditionalUpdates;

    public VisibleChunkCollector(int frame, int regionIdsLength, boolean ignoreQueueSizeLimit) {
        this.frame = frame;

        this.sortedRenderLists = new ObjectArrayList<>();
        this.sortedRebuildLists = new EnumMap<>(ChunkUpdateType.class);
        this.rebuildQueueOverflowCounts = new int[ChunkUpdateType.values().length];
        this.ignoreQueueSizeLimit = ignoreQueueSizeLimit;
        this.renderListsByRegion = new ChunkRenderList[regionIdsLength];

        for (var type : ChunkUpdateType.values()) {
            this.sortedRebuildLists.put(type, new ArrayDeque<>());
        }
    }

    private ChunkRenderList createRenderList(RenderRegion region) {
        ChunkRenderList renderList = new ChunkRenderList(region);
        this.sortedRenderLists.add(renderList);
        this.renderListsByRegion[region.getId()] = renderList;
        return renderList;
    }

    @Override
    public void visit(OcclusionNode node, boolean visible) {
        var section = node.getRenderSection();

        // Note: even if a section does not have render objects, we must ensure the render list is initialized and put
        // into the sorted queue of lists, so that we maintain the correct order of draw calls.
        int regionId = node.getRenderRegionId();
        ChunkRenderList renderList = this.renderListsByRegion[regionId];

        if (renderList == null) {
            renderList = this.createRenderList(section.getRegion());
        }

        if (visible) {
            if (section.hasAnythingToRender()) {
                renderList.add(section);
            }

            this.addToRebuildLists(section);
        }
    }

    private void addToRebuildLists(RenderSection section) {
        ChunkUpdateType type = section.getPendingUpdate();

        // Skip sections with an in-flight build to avoid redundant work. This is an advisory
        // check only: submitRebuildTasks() will validate getPendingUpdate() independently before
        // scheduling, so a stale null read of the token here cannot cause a double submission.
        if (type != null && section.getBuildCancellationToken() == null) {
            Queue<RenderSection> queue = this.sortedRebuildLists.get(type);

            if (this.ignoreQueueSizeLimit || queue.size() < type.getMaximumQueueSize()) {
                queue.add(section);
            } else {
                this.rebuildQueueOverflowCounts[type.ordinal()]++;
                this.hasAdditionalUpdates = true;
            }
        }
    }

    public SortedRenderLists createRenderLists() {
        return new SortedRenderLists(this.sortedRenderLists);
    }

    public ChunkRebuildLists getRebuildLists() {
        EnumMap<ChunkUpdateType, Integer> overflowCounts = new EnumMap<>(ChunkUpdateType.class);
        if (this.hasAdditionalUpdates) {
            var values = ChunkUpdateType.values();
            for (int i = 0; i < values.length; i++) {
                if (this.rebuildQueueOverflowCounts[i] != 0) {
                    overflowCounts.put(values[i], this.rebuildQueueOverflowCounts[i]);
                }
            }
        }
        return new ChunkRebuildLists(this.sortedRebuildLists, this.hasAdditionalUpdates, overflowCounts);
    }
}
