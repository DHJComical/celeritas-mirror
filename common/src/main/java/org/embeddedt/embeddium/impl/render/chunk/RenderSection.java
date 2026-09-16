package org.embeddedt.embeddium.impl.render.chunk;

import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.embeddedt.embeddium.impl.render.chunk.sorting.PartitionTree;
import org.embeddedt.embeddium.impl.render.chunk.sorting.SortState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection extends AbstractSection {
    // Render Region State
    private final RenderRegion region;

    // We must use EVERYTHING here for the default visibility encoding, so that ContextBundle.empty() would correspond
    // to the data generated for an empty section.
    public static final BuiltRenderSectionData EMPTY_DATA = new BuiltRenderSectionData();

    static {
        EMPTY_DATA.hasBlockGeometry = false;
        EMPTY_DATA.visibilityData = VisibilityEncoding.EVERYTHING;
    }

    // Rendering State
    private BuiltRenderSectionData contextData;

    // Packed encoding of the visibility encoding, visuals flags, pending update type, and build-in-flight bit;
    // see PackedSectionMetadata for the bit layout. The graph search reads a mirror of this in SectionLattice, so
    // mutate it only through writeMetadata(), which notifies the mirror.
    private long packedMetadata;

    // Notified on every packedMetadata change to keep the lattice mirror in sync. Null until the section is
    // attached to its manager (the initial constructor write predates attachment).
    @Nullable
    private MetadataSink metadataSink;

    public interface MetadataSink {
        void onMetadataChanged(RenderSection section);
    }

    /** Marks a section with no translucent render passes at all, which the debug overlay does not count */
    public static final int NO_TRANSLUCENT_GEOMETRY = -1;

    /**
     * A mapping from translucent render passes to the sort state for that particular pass, holding the data needed to
     * resort that pass' geometry as the camera moves. Only passes that actually need resorting appear here, so this is
     * empty for sections whose translucent geometry (if any) was sorted once at mesh time.
     */
    @Getter
    @NotNull
    private Map<TerrainRenderPass, SortState.Resortable> translucencySortStates = Collections.emptyMap();

    /**
     * The {@link SortState#debugIndex()} of the most expensive sort among this section's translucent passes, or
     * {@link #NO_TRANSLUCENT_GEOMETRY} if it has none. Only consumed by the debug overlay.
     */
    @Getter
    private int highestSortingIndex = NO_TRANSLUCENT_GEOMETRY;

    public enum SortMode {
        NONE,
        TREE,
        DYNAMIC
    }

    @Getter
    @NotNull
    private SortMode sortMode = SortMode.NONE;

    // Pending Update State

    // The in-flight build job for this section, if one exists. Serves two purposes:
    //   1. Cancellation: allows delete() to abort a queued or executing build early.
    //   2. Deduplication hint: VisibleChunkCollector skips re-queuing a section whose build
    //      is already in flight. submitRebuildTasks() performs the authoritative type check.
    @Nullable
    private CancellationToken buildCancellationToken = null;

    private int lastBuiltFrame = -1;
    private int lastSubmittedFrame = -1;

    // Lifetime state
    private boolean disposed;

    @Getter
    @Setter
    private long lastBuildDurationNanos;

    // Used by the translucency sorter, to determine when a section needs sorting again
    public double lastCameraX, lastCameraY, lastCameraZ;

    public RenderSection(RenderRegion region, int chunkX, int chunkY, int chunkZ) {
        super(chunkX, chunkY, chunkZ);

        this.region = region;

        this.contextData = null;
        this.updateCachedContextDataFlags();
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        if (this.buildCancellationToken != null) {
            this.buildCancellationToken.setCancelled();
            this.setBuildCancellationToken(null);
        }

        this.setInfo(null);
        this.disposed = true;
    }

    public boolean setInfo(@Nullable BuiltRenderSectionData info) {
        boolean changed = !Objects.equals(info, this.contextData);
        if (changed) {
            var region = this.getRegion();
            if (this.contextData == null) {
                region.updateSectionLoadTime(this);
            }
            region.onSectionDataChanged();
            this.contextData = info;
            this.updateCachedContextDataFlags();
        }
        return changed;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public boolean isBuilt() {
        return this.contextData != null;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public @Nullable BuiltRenderSectionData getBuiltContext() {
        return this.contextData;
    }

    public void updateCachedContextDataFlags() {
        int flags = 0;
        if (this.contextData != null) {
            flags = this.contextData.getVisualBitmaskForSection();
            if (this.sortMode == SortMode.DYNAMIC) {
                flags |= 1 << RenderVisualsService.NEEDS_DYNAMIC_SORT;
            }
        }
        long visibilityData = this.contextData != null ? this.contextData.visibilityData : VisibilityEncoding.NULL;
        boolean hasOccluderData = this.contextData != null && this.contextData.occluderBoxes != null;

        this.writeMetadata(PackedSectionMetadata.withHasOccluderData(PackedSectionMetadata.withVisibilityData(
                PackedSectionMetadata.withVisualsFlags(this.packedMetadata, flags), visibilityData), hasOccluderData));
    }

    // The sole writer of packedMetadata: stores the new word and notifies the mirror.
    private void writeMetadata(long packed) {
        this.packedMetadata = packed;
        if (this.metadataSink != null) {
            this.metadataSink.onMetadataChanged(this);
        }
    }

    public long getPackedMetadata() {
        return this.packedMetadata;
    }

    public void setMetadataSink(@Nullable MetadataSink sink) {
        this.metadataSink = sink;
    }

    public int getVisualsServiceFlags() {
        return PackedSectionMetadata.getVisualsFlags(this.packedMetadata);
    }

    public long getVisibilityData() {
        return PackedSectionMetadata.getVisibilityData(this.packedMetadata);
    }

    public boolean hasAnythingToRender() {
        return this.getVisualsServiceFlags() != 0;
    }

    /**
     * @param sortStates the passes needing a resort as the camera moves; every entry implies dynamic sorting
     * @param highestSortingIndex the highest variant seen before compaction, or {@link #NO_TRANSLUCENT_GEOMETRY}
     */
    public void setTranslucencySortStates(@NotNull Map<TerrainRenderPass, SortState.Resortable> sortStates, int highestSortingIndex) {
        this.translucencySortStates = Map.copyOf(sortStates);
        this.highestSortingIndex = highestSortingIndex;
        if (sortStates.isEmpty()) {
            this.sortMode = SortMode.NONE;
        } else if (sortStates.values().stream().allMatch(state -> state instanceof PartitionTree)) {
            this.sortMode = SortMode.TREE;
        } else {
            this.sortMode = SortMode.DYNAMIC;
        }
        this.updateCachedContextDataFlags();
    }

    public void clearTranslucencySortStates() {
        this.translucencySortStates = Collections.emptyMap();
        this.sortMode = SortMode.NONE;
        this.updateCachedContextDataFlags();
    }

    public boolean isTreeSorted() {
        return this.sortMode == SortMode.TREE;
    }

    public @Nullable CancellationToken getBuildCancellationToken() {
        return this.buildCancellationToken;
    }

    public void setBuildCancellationToken(@Nullable CancellationToken token) {
        this.buildCancellationToken = token;
        this.writeMetadata(PackedSectionMetadata.withBuildInFlight(this.packedMetadata, token != null));
    }

    public @Nullable ChunkUpdateType getPendingUpdate() {
        return PackedSectionMetadata.getPendingUpdate(this.packedMetadata);
    }

    public void setPendingUpdate(@Nullable ChunkUpdateType type) {
        this.writeMetadata(PackedSectionMetadata.withPendingUpdate(this.packedMetadata, type));
    }

    /**
     * Request a type of chunk update for this render section. This may "upgrade" an existing pending update for the
     * section.
     * @param type the chunk update
     * @return true if the section's chunk update type has changed
     */
    public boolean requestUpdate(ChunkUpdateType type) {
        type = ChunkUpdateType.getPromotionUpdateType(this.getPendingUpdate(), type);

        if (type != null) {
            this.setPendingUpdate(type);
            return true;
        } else {
            return false;
        }
    }

    public int getLastBuiltFrame() {
        return this.lastBuiltFrame;
    }

    public void setLastBuiltFrame(int lastBuiltFrame) {
        this.lastBuiltFrame = lastBuiltFrame;
    }

    public int getLastSubmittedFrame() {
        return this.lastSubmittedFrame;
    }

    public void setLastSubmittedFrame(int lastSubmittedFrame) {
        this.lastSubmittedFrame = lastSubmittedFrame;
    }
}
