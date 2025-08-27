package org.embeddedt.embeddium.impl.render.chunk;

import lombok.Getter;
import lombok.Setter;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
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
    private boolean hasAnythingToRender;
    @Getter
    private int visualsServiceFlags;

    /**
     * A mapping from translucent render passes to the sort state for that particular pass (which contains data needed
     * to perform a resort of the geometry as the camera moves). Will be empty for sections without any translucent
     * render passes.
     */
    @Getter
    @NotNull
    private Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> translucencySortStates = Collections.emptyMap();

    @Getter
    private TranslucentQuadAnalyzer.Level highestSortingLevel = TranslucentQuadAnalyzer.Level.NONE;

    @Getter
    @Setter
    private boolean needsDynamicTranslucencySorting;

    // Pending Update State
    @Nullable
    private CancellationToken buildCancellationToken = null;

    @Nullable
    private ChunkUpdateType pendingUpdateType;

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
            this.buildCancellationToken = null;
        }

        this.setInfo(null);
        this.disposed = true;
    }

    public boolean setInfo(@Nullable BuiltRenderSectionData info) {
        boolean changed = !Objects.equals(info, this.contextData);
        if (changed) {
            if (this.contextData == null) {
                this.getRegion().updateSectionLoadTime(this);
            }
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
        this.visualsServiceFlags = this.contextData != null ? this.contextData.getVisualBitmaskForSection() : 0;
        this.hasAnythingToRender = this.visualsServiceFlags != 0;
    }

    public boolean hasAnythingToRender() {
        return this.hasAnythingToRender;
    }

    public void setTranslucencySortStates(@NotNull Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> sortStates) {
        this.translucencySortStates = Map.copyOf(sortStates);

        TranslucentQuadAnalyzer.Level level = TranslucentQuadAnalyzer.Level.NONE;
        boolean needsDynamicSorting = false;

        if (!sortStates.isEmpty()) {
            // Find highest level among all sort states
            for (TranslucentQuadAnalyzer.SortState state : sortStates.values()) {
                level = state.level().ordinal() > level.ordinal() ? state.level() : level;
                needsDynamicSorting |= state.requiresDynamicSorting();
            }
        }

        this.highestSortingLevel = level;
        this.needsDynamicTranslucencySorting = needsDynamicSorting;
    }

    public @Nullable CancellationToken getBuildCancellationToken() {
        return this.buildCancellationToken;
    }

    public void setBuildCancellationToken(@Nullable CancellationToken token) {
        this.buildCancellationToken = token;
    }

    public @Nullable ChunkUpdateType getPendingUpdate() {
        return this.pendingUpdateType;
    }

    public void setPendingUpdate(@Nullable ChunkUpdateType type) {
        this.pendingUpdateType = type;
    }

    /**
     * Request a type of chunk update for this render section. This may "upgrade" an existing pending update for the
     * section.
     * @param type the chunk update
     * @return true if the section's chunk update type has changed
     */
    public boolean requestUpdate(ChunkUpdateType type) {
        type = ChunkUpdateType.getPromotionUpdateType(this.pendingUpdateType, type);

        if (type != null) {
            this.pendingUpdateType = type;
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
