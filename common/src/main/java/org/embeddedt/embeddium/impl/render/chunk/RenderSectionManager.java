package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.embeddedt.embeddium.impl.common.util.TimeUtil;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.profiling.TimerQueryManager;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobMetricsTracker;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobCollector;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderSortTask;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderListManager;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionGraph;
import org.embeddedt.embeddium.impl.render.chunk.lists.SectionTicker;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.metrics.RenderSectionMetricsTracker;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.AsyncOcclusionMode;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegionManager;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.ShadowSearchFrustum;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.embeddedt.embeddium.impl.util.iterator.ByteIterator;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkSortOutput;
import org.embeddedt.embeddium.impl.render.chunk.sorting.CutPlaneIndex;
import org.embeddedt.embeddium.impl.render.chunk.sorting.PartitionTree;
import org.embeddedt.embeddium.impl.render.chunk.sorting.SortState;
import org.embeddedt.embeddium.impl.util.suppliers.ExpiringSupplier;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class RenderSectionManager {
    /**
     * When true, the section manager will continuously mark all sections as needing to be remeshed whenever the
     * update queue empties.
     */
    protected static final boolean CONTINUOUSLY_REMESH_WORLD = false;

    /**
     * How many frames' worth of dispatch the BFS may collect into the initial-build list. The list only needs to
     * outlast the interval between graph updates, and an overflow re-marks the graph dirty as soon as results are
     * uploaded, so a small multiple of the in-flight target is sufficient.
     */
    private static final int REBUILD_LIST_FRAMES = 2;

    private final ChunkBuilder builder;

    private final Thread renderThread = Thread.currentThread();

    private final RenderRegionManager regions;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final ConcurrentLinkedDeque<ChunkJobResult<? extends ChunkTaskOutput>> buildResults = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Runnable> asyncSubmittedTasks = new ConcurrentLinkedDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final int renderDistance;

    /** {@link #cameraPosition}, rounded down to block coordinates */
    protected @Nullable Vector3ic cameraBlockPosition;

    protected Vector3d cameraPosition = new Vector3d();

    /** last frame's exact camera position */
    protected @Nullable Vector3d previousCameraPosition;

    private final CutPlaneIndex<RenderSection> cutPlaneIndex = new CutPlaneIndex<>();

    private final ChunkJobMetricsTracker.MetricsData treeSortMetrics = new ChunkJobMetricsTracker.MetricsData();
    private long treeStatsWindowStart = System.nanoTime();
    private int treeTriggersThisSecond, treeTriggersLastSecond;

    @Getter
    private final RenderPassConfiguration<?> renderPassConfiguration;

    private final Set<TerrainRenderPass> disabledRenderPasses;

    private final int minSection, maxSection;

    // Lattice and search thread shared by the terrain and shadow passes.
    private final SectionGraph sectionGraph;

    protected final RenderListManager renderListManager;

    @Nullable
    protected final RenderListManager shadowRenderListManager;

    // Set by the shadow pass, which precedes the terrain pass in a frame and submits both searches. The terrain
    // pass of the same frame then skips re-running the search.
    private boolean shadowPassRanThisFrame;

    // Shared by every section (one allocation, not one per section); installed on each RenderSection so its
    // packedMetadata changes fan out to the list manager mirror(s).
    private final RenderSection.MetadataSink metadataSink = this::pushSectionMetadata;

    protected final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    private final Object2ObjectOpenHashMap<TerrainRenderPass, TimerQueryManager> renderPassDrawTimers = new Object2ObjectOpenHashMap<>();

    protected final ReferenceSet<RenderSection> sectionsRequestingUpdate = new ReferenceOpenHashSet<>();

    @Getter
    protected final ChunkJobMetricsTracker jobMetricsTracker = new ChunkJobMetricsTracker();

    @Getter
    protected final RenderSectionMetricsTracker sectionMetricsTracker = new RenderSectionMetricsTracker();

    @Deprecated
    public RenderSectionManager(RenderPassConfiguration<?> configuration, Supplier<ChunkBuildContext> contextSupplier,
                                BiFunction<RenderDevice, RenderPassConfiguration<?>, ChunkRenderer> chunkRenderer,
                                int renderDistance, CommandList commandList, int minSection, int maxSection,
                                int requestedThreads) {
        this(configuration, contextSupplier, chunkRenderer, renderDistance, commandList, minSection, maxSection, requestedThreads, false);
    }

    public RenderSectionManager(RenderPassConfiguration<?> configuration, Supplier<ChunkBuildContext> contextSupplier,
                                BiFunction<RenderDevice, RenderPassConfiguration<?>, ChunkRenderer> chunkRenderer,
                                int renderDistance, CommandList commandList, int minSection, int maxSection,
                                int requestedThreads, boolean hasShadowPass) {
        this.chunkRenderer = chunkRenderer.apply(RenderDevice.INSTANCE, configuration);

        this.renderPassConfiguration = configuration;

        this.builder = new ChunkBuilder(this::managedBlock, contextSupplier, requestedThreads);

        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList, this.renderPassConfiguration);

        this.minSection = minSection;
        this.maxSection = maxSection;
        AsyncOcclusionMode asyncMode = this.getAsyncOcclusionMode();
        this.sectionGraph = new SectionGraph(this.minSection, this.maxSection, asyncMode, hasShadowPass, this.useRasterOcclusionCulling());
        this.renderListManager = new RenderListManager(this.sectionGraph, false, asyncMode, this.createSectionTicker());
        if (hasShadowPass) {
            this.shadowRenderListManager = new RenderListManager(this.sectionGraph, true, asyncMode, this.createSectionTicker());
        } else {
            this.shadowRenderListManager = null;
        }

        this.disabledRenderPasses = new ReferenceArraySet<>();
    }

    protected abstract AsyncOcclusionMode getAsyncOcclusionMode();

    protected @Nullable SectionTicker createSectionTicker() {
        return null;
    }

    public void managedBlock(BooleanSupplier isDone) {
        while (!isDone.getAsBoolean()) {
            Runnable task = this.asyncSubmittedTasks.poll();
            if (task != null) {
                task.run();
            } else {
                LockSupport.parkNanos("Wait", 100000L);
            }
        }
    }

    public void runAsyncTasks() {
        Runnable task;

        while ((task = this.asyncSubmittedTasks.poll()) != null) {
            task.run();
        }

        this.renderPassDrawTimers.values().forEach(TimerQueryManager::updateTime);
    }

    /**
     * Whether terrain is being rendered for shadows.
     */
    public boolean isInShadowPass() {
        return false;
    }

    protected boolean isDebugInfoShown() {
        return false;
    }

    /**
     * Terrain-pass update: run the terrain search (unless the shadow pass already ran it this frame) and the
     * per-frame camera bookkeeping.
     */
    public void update(Viewport positionedViewport, int frame, boolean spectator) {
        if (!this.shadowPassRanThisFrame) {
            this.updateCameraPosition(positionedViewport);
        }

        if (this.shadowPassRanThisFrame) {
            // The shadow pass searched for this frame if the graph was dirty then. Any needsUpdate raised by
            // build results between the two passes carries over to the next frame.
            this.shadowPassRanThisFrame = false;
        } else {
            this.createTerrainRenderList(positionedViewport, null, frame, spectator);
        }

        this.checkTranslucencyChange();
    }

    /**
     * Shadow-pass update. The shadow pass runs before the terrain pass in a frame, so this first runs the terrain
     * search for the player viewport when one is due, then the shadow search.
     */
    public void updateForShadowPass(Viewport playerViewport, Viewport shadowViewport, int frame, boolean spectator) {
        if (this.shadowRenderListManager == null) {
            throw new IllegalStateException("No shadow pass configured");
        }

        this.updateCameraPosition(playerViewport);
        this.shadowPassRanThisFrame = true;

        if (this.renderListManager.isNeedsUpdate()) {
            this.createTerrainRenderList(playerViewport, null, frame, spectator);
        }

        Vector3fc lightVector = null;

        if (shadowViewport.getFrustum() instanceof ShadowSearchFrustum searchFrustum && searchFrustum.supportsOcclusionSearch()) {
            lightVector = new Vector3f(searchFrustum.shadowLightX(), searchFrustum.shadowLightY(), searchFrustum.shadowLightZ());
        }

        this.shadowRenderListManager.startShadowGraphUpdate(shadowViewport, frame, this.regions.getRegionIdsLength(),
                this.getSearchDistance(null), lightVector, this.getTargetQueueSize());
    }

    private void updateCameraPosition(Viewport positionedViewport) {
        if (this.cameraBlockPosition != null) {
            this.previousCameraPosition = this.cameraPosition;
        }

        this.cameraBlockPosition = positionedViewport.getBlockCoord();
        var transform = positionedViewport.getTransform();
        this.cameraPosition = new Vector3d(transform.x, transform.y, transform.z);
    }

    public boolean hasShadowPass() {
        return this.shadowRenderListManager != null;
    }

    /**
     * Whether the shadow pass has already run this frame, in which case the terrain pass must not join the
     * searches it submitted.
     */
    public boolean didShadowPassRunThisFrame() {
        return this.shadowPassRanThisFrame;
    }

    private void checkTranslucencyChange() {
        long now = System.nanoTime();
        if (now - this.treeStatsWindowStart >= ChunkJobMetricsTracker.OBSERVATION_COUNT_TIME) {
            this.treeTriggersLastSecond = this.treeTriggersThisSecond;
            this.treeTriggersThisSecond = 0;
            this.treeSortMetrics.flipInterval();
            this.treeStatsWindowStart = now;
        }

        if(cameraBlockPosition == null)
            return;

        int camSectionX = PositionUtil.posToSectionCoord(cameraPosition.x);
        int camSectionY = PositionUtil.posToSectionCoord(cameraPosition.y);
        int camSectionZ = PositionUtil.posToSectionCoord(cameraPosition.z);

        this.scheduleTranslucencyUpdates(camSectionX, camSectionY, camSectionZ);
        this.scheduleTreeSortedUpdates();
    }

    private void scheduleTranslucencyUpdates(int camSectionX, int camSectionY, int camSectionZ) {
        var renderListManager = this.getCurrentRenderListManager();
        var rebuildLists = renderListManager.getRebuildLists().byUpdateType();
        var allowImportant = allowImportantRebuilds();
        var translucentPass = this.renderPassConfiguration.defaultTranslucentMaterial().pass;
        if (!this.hasTranslucencySortedSections()) {
            return;
        }
        for (Iterator<ChunkRenderList> it = renderListManager.getRenderLists().iterator(); it.hasNext(); ) {
            ChunkRenderList entry = it.next();
            var region = entry.getRegion();
            if (!region.hasSectionsInPass(translucentPass)) {
                continue;
            }
            ByteIterator sectionIterator = entry.sectionsNeedingDynamicSortIterator();
            if (sectionIterator == null) {
                continue;
            }
            while (sectionIterator.hasNext()) {
                var section = region.getSection(sectionIterator.nextByteAsInt());

                // tree-sorted sections are retriggered by scheduleTreeSortedUpdates() instead
                if (section == null || section.getSortMode() != RenderSection.SortMode.DYNAMIC) {
                    continue;
                }

                ChunkUpdateType update = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(), (allowImportant && this.shouldPrioritizeRebuild(section)) ? ChunkUpdateType.IMPORTANT_SORT : ChunkUpdateType.SORT);

                if (update == null) {
                    // We wouldn't be able to resort this section anyway
                    continue;
                }

                double dx = cameraPosition.x - section.lastCameraX;
                double dy = cameraPosition.y - section.lastCameraY;
                double dz = cameraPosition.z - section.lastCameraZ;
                double camDelta = (dx * dx) + (dy * dy) + (dz * dz);

                if (camDelta < 1) {
                    // Didn't move enough, ignore
                    continue;
                }

                boolean cameraChangedSection = camSectionX != PositionUtil.posToSectionCoord(section.lastCameraX) ||
                        camSectionY != PositionUtil.posToSectionCoord(section.lastCameraY) ||
                        camSectionZ != PositionUtil.posToSectionCoord(section.lastCameraZ);

                if (!cameraChangedSection && !section.isAlignedWithSectionOnGrid(camSectionX, camSectionY, camSectionZ)) {
                    continue;
                }

                section.setPendingUpdate(update);
                // Inject it into the appropriate list
                rebuildLists.get(update).add(section);

                section.lastCameraX = cameraPosition.x;
                section.lastCameraY = cameraPosition.y;
                section.lastCameraZ = cameraPosition.z;
            }
        }
    }

    /** Resorts tree-sorted sections whose cut planes this frame's movement crossed, visible or not */
    private void scheduleTreeSortedUpdates() {
        if (this.previousCameraPosition == null || this.cutPlaneIndex.size() == 0) {
            return;
        }

        this.cutPlaneIndex.query(
                this.previousCameraPosition.x, this.previousCameraPosition.y, this.previousCameraPosition.z,
                this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z,
                section -> {
                    this.treeTriggersThisSecond++;
                    this.scheduleTreeSort(section);
                }
        );
    }

    private void scheduleTreeSort(RenderSection section) {
        ChunkUpdateType update = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(),
                (allowImportantRebuilds() && this.shouldPrioritizeRebuild(section)) ? ChunkUpdateType.IMPORTANT_SORT : ChunkUpdateType.SORT);

        if (update == null) {
            // We wouldn't be able to resort this section anyway
            return;
        }

        section.setPendingUpdate(update);
        this.getCurrentRenderListManager().getRebuildLists().byUpdateType().get(update).add(section);
    }

    /**
     * {@return true if the renderer should respect per-frame queue limits and not try to update as many chunks as
     * possible per frame}
     */
    protected boolean shouldRespectUpdateTaskQueueSizeLimit() {
        return true;
    }

    private void createTerrainRenderList(Viewport viewport, Matrix4fc projectionMatrix, int frame, boolean spectator) {
        final var searchDistance = this.getSearchDistance(projectionMatrix);
        final var useOcclusionCulling = this.shouldUseOcclusionCulling(viewport, spectator);

        this.renderListManager.startGraphUpdate(viewport, frame, this.regions.getRegionIdsLength(),
                searchDistance, useOcclusionCulling, this.getTargetQueueSize());
    }

    private int getTargetQueueSize() {
        if (this.shouldRespectUpdateTaskQueueSizeLimit()) {
            return (int) Math.min(Integer.MAX_VALUE, (long) this.builder.getTargetQueueSize() * REBUILD_LIST_FRAMES);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    protected abstract boolean useFogOcclusion();

    protected abstract boolean useRasterOcclusionCulling();

    private float getSearchDistance(@Nullable Matrix4fc projectionMatrix) {
        float distance;

        if (this.useFogOcclusion()) {
            distance = this.getEffectiveRenderDistance(projectionMatrix);
        } else {
            distance = this.getRenderDistance();
        }

        return distance;
    }

    protected abstract boolean shouldUseOcclusionCulling(Viewport viewport, boolean spectator);

    private boolean hasTranslucencySortedSections() {
        return this.getCurrentRenderListManager().getRenderLists().getPasses().stream().anyMatch(TerrainRenderPass::isSorted);
    }

    protected abstract boolean isSectionVisuallyEmpty(int x, int y, int z);

    public void onSectionAdded(int x, int y, int z) {
        long key = PositionUtil.packSection(x, y, z);

        if (this.sectionByPosition.containsKey(key)) {
            return;
        }

        RenderRegion region = this.regions.createForChunk(x, y, z);

        RenderSection renderSection = new RenderSection(region, x, y, z);
        region.addSection(renderSection);
        renderSection.setMetadataSink(this.metadataSink);

        this.sectionByPosition.put(key, renderSection);

        this.sectionGraph.attachRenderSection(renderSection);
        this.markGraphDirty();

        this.invalidateCachedSectionData(renderSection);

        if (this.isSectionVisuallyEmpty(x, y, z)) {
            this.updateSectionInfo(renderSection, RenderSection.EMPTY_DATA);
        } else {
            renderSection.setPendingUpdate(ChunkUpdateType.INITIAL_BUILD);
        }


        this.markGraphDirty();
    }

    public void onSectionRemoved(int x, int y, int z) {
        RenderSection section = this.sectionByPosition.remove(PositionUtil.packSection(x, y, z));

        if (section == null) {
            return;
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.invalidateCachedSectionData(section);

        this.updateSectionInfo(section, null);

        this.sectionGraph.detachRenderSection(section);
        this.markGraphDirty();

        this.sectionMetricsTracker.removeSection(section);

        this.cutPlaneIndex.remove(section);

        section.delete();

        this.markGraphDirty();
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, CameraTransform occlusionCamera, CameraTransform camera) {
        if (disabledRenderPasses.contains(pass)) {
            return;
        }

        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        boolean shouldProfile = isDebugInfoShown();

        TimerQueryManager timer = null;

        if (shouldProfile) {
            timer = renderPassDrawTimers.computeIfAbsent(pass, $ -> new TimerQueryManager());
            timer.startProfiling();
        }

        this.chunkRenderer.render(matrices, commandList, this.getCurrentRenderListManager().getRenderLists(), pass, occlusionCamera, camera);

        if (shouldProfile) {
            timer.finishProfiling();
        }

        commandList.flush();
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.getCurrentRenderListManager().isSectionVisible(x, y, z);
    }

    private boolean rebuildListHasUpdates() {
        for (var queue : this.getCurrentRenderListManager().getRebuildLists().byUpdateType().values()) {
            if (!queue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inject sections that requested a rebuild between graph updates into the appropriate rebuild lists.
     */
    private void promoteInterimRebuildList() {
        if (this.sectionsRequestingUpdate.isEmpty()) {
            return;
        }

        var rebuildLists = this.getCurrentRenderListManager().getRebuildLists().byUpdateType();
        boolean graphUpdatePending = this.getCurrentRenderListManager().isNeedsUpdate();

        for (var section : this.sectionsRequestingUpdate) {
            var updateType = section.getPendingUpdate();
            if (updateType == null) {
                // should never happen, but be defensive
                continue;
            }
            if (!graphUpdatePending || updateType.isImportant()) {
                rebuildLists.get(updateType).add(section);
            }
        }

        this.sectionsRequestingUpdate.clear();
    }

    public void updateChunks(boolean updateImmediately) {
        this.regions.update();
        this.jobMetricsTracker.tick();

        // Advance the scheduling controller once per frame, before any dispatch reads the budget. This runs only
        // on the main terrain pass so that an additional shadow pass in the same frame does not double-tick the
        // controller (which would halve its measured frame time); both passes share the same worker queue and
        // in-flight target.
        if (!this.isInShadowPass()) {
            this.builder.tickSchedulingBudget(this.jobMetricsTracker);
        }

        this.promoteInterimRebuildList();

        if (!rebuildListHasUpdates()) {
            if (CONTINUOUSLY_REMESH_WORLD && !this.getCurrentRenderListManager().getRebuildLists().hasAdditionalUpdates()) {
                this.scheduleRebuildAll();
            }
            return;
        }

        var blockingRebuilds = new ChunkJobCollector(Integer.MAX_VALUE, this.buildResults::add);
        var deferredRebuilds = new ChunkJobCollector(this.builder.getSchedulingBudget(), this.buildResults::add);

        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_REBUILD);
        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_SORT);

        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.REBUILD);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.INITIAL_BUILD);

        // Sorts fill whatever worker time the mesh dispatch left over, scaled by their measured relative cost
        var deferredSorts = new ChunkJobCollector(this.builder.getSortSchedulingBudget(), this.buildResults::add);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredSorts, ChunkUpdateType.SORT);

        blockingRebuilds.awaitCompletion(this.builder);

        // Tick singlethreaded rebuilds
        this.builder.tick();
    }

    public void uploadChunks() {
        var results = this.collectChunkBuildResults();

        if (results.isEmpty()) {
            return;
        }

        // Ensure occlusion threads are stopped at this point, as we're about to mutate render section data.
        this.finishAllGraphUpdates();

        this.processChunkBuildResults(results);

        for (var result : results) {
            result.output().delete();
        }

        // Forcefully mark the graph as needing updates if the previous render list detected an overflow of the
        // update queue. This is necessary to queue those additional chunks.
        if (this.getCurrentRenderListManager().getRebuildLists().hasAdditionalUpdates()) {
            this.markGraphDirty();
        }
    }

    public final void tickVisibleRenders() {
        this.getCurrentRenderListManager().tickVisibleRenders();
    }

    private void processChunkBuildResults(ArrayList<ChunkJobResult.Success<? extends ChunkTaskOutput>> results) {
        var filtered = filterChunkBuildResults(results);

        this.regions.uploadMeshes(RenderDevice.INSTANCE.createCommandList(), filtered, this::markGraphDirty);

        for (var holder : filtered) {
            var result = holder.output();

            // whether this result belongs to the most recently submitted build
            boolean latest = result.buildTime >= result.render.getLastSubmittedFrame();

            if (result instanceof ChunkBuildOutput buildResult) {
                boolean changed = this.updateSectionInfo(result.render, buildResult.info);

                if (changed) {
                    // The chunk graph must be rebuilt if the render section reports the info has changed. This
                    // could indicate an occlusion data update, block entity addition/removal, animated texture
                    // change, etc.
                    this.markGraphDirty();
                }

                // We only change the translucency info on full rebuilds, as sorts can keep using the same data
                this.updateTranslucencyInfo(result.render, buildResult.meshes, latest);
            }

            var job = result.render.getBuildCancellationToken();

            if (job != null && latest) {
                result.render.setBuildCancellationToken(null);
            }

            result.render.setLastBuiltFrame(result.buildTime);
            this.sectionMetricsTracker.updateSectionBuildDuration(result.render, holder.executionTimeNanos());
        }
    }

    /**
     * @param latestBuild whether the meshes come from the most recent submission; a stale build keeps its planes
     *                    indexed but leaves the camera bookkeeping to the build still in flight
     */
    private void updateTranslucencyInfo(RenderSection render, Map<TerrainRenderPass, BuiltSectionMeshParts> meshes, boolean latestBuild) {
        Map<TerrainRenderPass, SortState.Resortable> sortStates = new Reference2ObjectArrayMap<>();
        int highestIndex = RenderSection.NO_TRANSLUCENT_GEOMETRY;

        for(var entry : meshes.entrySet()) {
            if(!entry.getKey().isSorted()) {
                continue;
            }

            var state = Objects.requireNonNull(entry.getValue().sortState());

            highestIndex = Math.max(highestIndex, state.debugIndex());

            // Only resortable states survive compaction; everything else is already in its final order.
            if(state.compactForStorage() instanceof SortState.Resortable resortable) {
                sortStates.put(entry.getKey(), resortable);
            }
        }

        render.setTranslucencySortStates(sortStates.isEmpty() ? Collections.emptyMap() : sortStates, highestIndex);

        if (render.isTreeSorted()) {
            List<PartitionTree> trees = new ArrayList<>(sortStates.size());
            for (var state : sortStates.values()) {
                trees.add((PartitionTree) state);
            }

            this.cutPlaneIndex.put(render, PartitionTree.mergeCutPlanes(trees), render.getOriginX(), render.getOriginY(), render.getOriginZ());

            // The build sorted for the camera at submission (see submitRebuildTasks). A crossing since then happened
            // before the planes were indexed, so it has to be caught here or the order stays stale until the next one.
            int ox = render.getOriginX(), oy = render.getOriginY(), oz = render.getOriginZ();
            for (PartitionTree tree : trees) {
                if (latestBuild && tree.crossesCutPlane(render.lastCameraX - ox, render.lastCameraY - oy, render.lastCameraZ - oz,
                        this.cameraPosition.x - ox, this.cameraPosition.y - oy, this.cameraPosition.z - oz)) {
                    this.treeTriggersThisSecond++;
                    this.scheduleTreeSort(render);
                    break;
                }
            }

            // any crossing up to now was just handled. Dynamic sections keep the submission camera so that
            // scheduleTranslucencyUpdates still sees movement made during the build.
            if (latestBuild) {
                render.lastCameraX = this.cameraPosition.x;
                render.lastCameraY = this.cameraPosition.y;
                render.lastCameraZ = this.cameraPosition.z;
            }
        } else {
            // a rebuild that is no longer tree-sorted must drop its stale planes
            this.cutPlaneIndex.remove(render);
        }
    }

    // Section MetadataSink: mirrors a section's packed metadata into the graph-search lattice on every
    // packedMetadata mutation (visibility/visuals via setInfo, pending update, build-in-flight).
    private void pushSectionMetadata(RenderSection section) {
        this.sectionGraph.updateSectionMetadata(section.getChunkX(), section.getChunkY(), section.getChunkZ(),
                section.getPackedMetadata(), this::markGraphDirty);
    }

    @MustBeInvokedByOverriders
    protected boolean updateSectionInfo(RenderSection render, @Nullable BuiltRenderSectionData info) {
        boolean changed = render.setInfo(info);

        if (changed) {
            if (!(info instanceof MinecraftBuiltRenderSectionData<?, ?> data)) {
                this.sectionsWithGlobalEntities.remove(render);
            } else if (!data.globalBlockEntities.isEmpty()) {
                this.sectionsWithGlobalEntities.add(render);
            }
        }

        return changed;
    }

    private static List<ChunkJobResult.Success<? extends ChunkTaskOutput>> filterChunkBuildResults(ArrayList<ChunkJobResult.Success<? extends ChunkTaskOutput>> outputs) {
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, ChunkJobResult.Success<? extends ChunkTaskOutput>>();

        for (var holder : outputs) {
            var output = holder.output();
            if (output.render.isDisposed() || output.render.getLastBuiltFrame() > output.buildTime) {
                continue;
            }

            var render = output.render;
            var previousHolder = map.get(render);

            if (previousHolder == null || previousHolder.output().buildTime < output.buildTime) {
                map.put(render, holder);
            }
        }

        return new ArrayList<>(map.values());
    }

    private ArrayList<ChunkJobResult.Success<? extends ChunkTaskOutput>> collectChunkBuildResults() {
        ArrayList<ChunkJobResult.Success<? extends ChunkTaskOutput>> results = new ArrayList<>();
        ChunkJobResult<? extends ChunkTaskOutput> result;

        while ((result = this.buildResults.poll()) != null) {
            if (result instanceof ChunkJobResult.Success<? extends ChunkTaskOutput> successfulResult) {
                this.jobMetricsTracker.collectMetrics(successfulResult);

                if (successfulResult.output() instanceof ChunkSortOutput sort && sort.render.isTreeSorted() && successfulResult.executionTimeNanos() >= 0) {
                    this.treeSortMetrics.collect(successfulResult.executionTimeNanos());
                }
                results.add(successfulResult);
            } else if (result instanceof ChunkJobResult.Failure<? extends ChunkTaskOutput> failure) {
                failure.abort();
            } else {
                throw new AssertionError();
            }
        }

        return results;
    }

    private void submitRebuildTasks(ChunkJobCollector collector, ChunkUpdateType type) {
        var queue = this.getCurrentRenderListManager().getRebuildLists().byUpdateType().get(type);

        int frame = this.getCurrentRenderListManager().getLastUpdatedFrame();

        int cameraX = (int) Math.floor(this.cameraPosition.x);
        int cameraY = (int) Math.floor(this.cameraPosition.y);
        int cameraZ = (int) Math.floor(this.cameraPosition.z);

        while (!queue.isEmpty() && collector.canOffer()) {
            RenderSection section = queue.remove();

            if (section.isDisposed()) {
                continue;
            }

            // The pending update type may have changed since this entry was queued. Cases:
            //   - A SORT was promoted to REBUILD (e.g. a block changed while a sort was pending):
            //     the section remains in the SORT queue but pendingUpdate is now REBUILD, so the
            //     SORT pass skips it and the REBUILD pass picks it up correctly.
            //   - The type was cleared by a prior pass in the same frame.
            //   - The type was set to null after the async BFS generated the list (authoritative
            //     guard against double submissions from a stale buildCancellationToken read).
            if (section.getPendingUpdate() != type) {
                continue;
            }

            ChunkBuilderTask<? extends ChunkTaskOutput> task = type.isSort() ? this.createSortTask(section, frame) : this.createRebuildTask(section, frame);

            if (task == null && type.isSort()) {
                // Ignore sorts that became invalid
                section.setPendingUpdate(null);
                continue;
            }

            if (task != null) {
                // Prioritize by distance so sections that only became reachable (and thus schedulable) after their
                // neighbors were built still run ahead of farther sections that were queued in earlier frames.
                long priority = (long) section.getSquaredDistanceFromBlockCenter(cameraX, cameraY, cameraZ);
                var job = this.builder.scheduleTask(task, type.isImportant(), priority, collector::onJobFinished);
                collector.addSubmittedJob(job);

                section.setBuildCancellationToken(job);

                if (!type.isSort()) {
                    // Prevent further sorts from being performed on this section
                    section.clearTranslucencySortStates();

                    // the meshing task sorts its translucent geometry for this camera
                    section.lastCameraX = this.cameraPosition.x;
                    section.lastCameraY = this.cameraPosition.y;
                    section.lastCameraZ = this.cameraPosition.z;
                }
            } else {
                var result = new ChunkJobResult.Success<>(new ChunkBuildOutput(section, RenderSection.EMPTY_DATA, Reference2ReferenceMaps.emptyMap(), frame), -1);
                this.buildResults.add(result);

                section.setBuildCancellationToken(null);
            }

            section.setLastSubmittedFrame(frame);
            section.setPendingUpdate(null);
        }
    }

    protected abstract @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame);

    public ChunkBuilderSortTask createSortTask(RenderSection render, int frame) {
        if(render.getTranslucencySortStates().isEmpty())
            return null;
        return new ChunkBuilderSortTask(render, cameraPosition.x, cameraPosition.y, cameraPosition.z, frame, render.getTranslucencySortStates(), this.renderPassConfiguration);
    }

    public void markGraphDirty() {
        if (this.shadowRenderListManager != null) {
            this.shadowRenderListManager.setNeedsUpdate(true);
        }
        this.renderListManager.setNeedsUpdate(true);
    }

    public void finishAllGraphUpdates() {
        this.renderListManager.finishPreviousGraphUpdate();
        if (this.shadowRenderListManager != null) {
            this.shadowRenderListManager.finishPreviousGraphUpdate();
        }
    }

    /**
     * Whether {@link #update} must run in the current pass. In the terrain pass this is also true when the shadow
     * pass already ran the terrain search this frame, so that {@link #update} can consume that state.
     */
    public boolean needsUpdate() {
        if (this.isInShadowPass()) {
            return this.shadowRenderListManager.isNeedsUpdate();
        }
        return this.renderListManager.isNeedsUpdate() || this.shadowPassRanThisFrame;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.finishAllGraphUpdates();

        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        for (var result : this.collectChunkBuildResults()) {
            result.output().delete(); // delete resources for any pending tasks (including those that were cancelled)
        }

        this.renderListManager.destroy();
        if (this.shadowRenderListManager != null) {
            this.shadowRenderListManager.destroy();
        }
        this.sectionGraph.destroy();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }

        this.renderPassDrawTimers.values().forEach(TimerQueryManager::close);
        this.renderPassDrawTimers.clear();

        this.sectionsWithGlobalEntities.clear();
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.getCurrentRenderListManager().getRenderLists().iterator();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    public final void scheduleAsyncTask(Runnable runnable) {
        if (Thread.currentThread() == this.renderThread) {
            // Run immediately, otherwise the thread may deadlock waiting for itself
            runnable.run();
        } else {
            asyncSubmittedTasks.add(runnable);
        }
    }

    private void scheduleRebuildOffThread(int x, int y, int z, boolean important) {
        scheduleAsyncTask(() -> this.scheduleSectionForRebuild(x, y, z, important));
    }

    public final void scheduleRebuild(int x, int y, int z, boolean important) {
        if (Thread.currentThread() != renderThread) {
            this.scheduleRebuildOffThread(x, y, z, important);
            return;
        }

        this.scheduleSectionForRebuild(x, y, z, important);
    }

    protected void invalidateCachedSectionData(RenderSection section) {

    }

    protected void scheduleSectionForRebuild(int x, int y, int z, boolean important) {
        RenderSection section = this.sectionByPosition.get(PositionUtil.packSection(x, y, z));

        if (section != null) {
            this.invalidateCachedSectionData(section);

            ChunkUpdateType pendingUpdate;

            if (allowImportantRebuilds() && (important || this.shouldPrioritizeRebuild(section))) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_REBUILD;
            } else {
                pendingUpdate = ChunkUpdateType.REBUILD;
            }

            if (section.requestUpdate(pendingUpdate)) {
                // Check importance using the section's new update type, as it may not be exactly what we requested
                important = section.getPendingUpdate().isImportant();

                if (important ||
                        (!this.getCurrentRenderListManager().isNeedsUpdate() &&
                            this.sectionsRequestingUpdate.size() < this.builder.getSchedulingBudget())) {
                    this.sectionsRequestingUpdate.add(section);
                } else {
                    this.markGraphDirty();
                }
            }
        }
    }

    public void scheduleRebuildAll() {
        for (var section : this.sectionByPosition.values()) {
            if (!this.isSectionVisuallyEmpty(section.getChunkX(), section.getChunkY(), section.getChunkZ())) {
                this.invalidateCachedSectionData(section);
                section.requestUpdate(ChunkUpdateType.REBUILD);
            }
        }
        this.markGraphDirty();
    }

    private static final float NEARBY_REBUILD_DISTANCE = MathUtil.square(16.0f);

    private boolean shouldPrioritizeRebuild(RenderSection section) {
        return this.cameraBlockPosition != null && section.getSquaredDistanceFromBlockCenter(this.cameraBlockPosition.x(), this.cameraBlockPosition.y(), this.cameraBlockPosition.z()) < NEARBY_REBUILD_DISTANCE;
    }

    /**
     * {@return true if rebuilds of chunks near the player should block the main thread, reduces flickering but will
     * potentially cause lag spikes}
     */
    protected boolean allowImportantRebuilds() {
        return false;
    }

    private float getEffectiveRenderDistance(@Nullable Matrix4fc projectionMatrix) {
        var color = ChunkShaderFogComponent.FOG_SERVICE.getFogColor();
        var alpha = color[3];
        var distance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();
        var shape = ChunkShaderFogComponent.FOG_SERVICE.getFogShapeIndex();

        var renderDistance = this.getRenderDistance();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (Math.abs(alpha - 1.0f) >= 1.0E-5F) {
            return renderDistance;
        }

        if (shape == FogService.FOG_SHAPE_PLANAR) {
            // The cullers measure the cylindrical distance max(|xz|, |y|) from the camera, which for spherical and
            // cylindrical fog is never larger than the distance the shader fogs by, so the cutoff can be used
            // as-is. Planar fog instead measures depth along the view axis, which is *smaller* than that distance,
            // so the cutoff has to be scaled up to the worst case before the cullers can use it.
            var secant = getMaximumFrustumSecant(projectionMatrix);

            if (secant == 0.0f) {
                // Not a projection we can bound the view cone of; assume fog can hide nothing.
                return renderDistance;
            }

            distance *= secant;
        }

        return Math.min(renderDistance, distance + 0.5f);
    }

    /**
     * Computes the largest factor by which a point inside the view frustum can be farther from the camera than its
     * depth along the view axis.
     *
     * <p>Computing from the projection rather than from the game's setting keeps this correct
     * under dynamic FOV, spyglasses, aspect ratio changes, temporal jitter, and any mod which alters the
     * projection.
     */
    private static float getMaximumFrustumSecant(@Nullable Matrix4fc projectionMatrix) {
        if (projectionMatrix == null) {
            return 0.0f;
        }

        // A perspective projection divides by -z (m23 = -1 before any scaling). An orthographic one (used by the
        // shadow pass) leaves w untouched, has no apex, and therefore no bounded view cone.
        float w = Math.abs(projectionMatrix.m23());

        if (w == 0.0f) {
            return 0.0f;
        }

        float tanX = (w + Math.abs(projectionMatrix.m20())) / Math.abs(projectionMatrix.m00());
        float tanY = (w + Math.abs(projectionMatrix.m21())) / Math.abs(projectionMatrix.m11());

        float secant = (float)Math.sqrt(1.0 + (tanX * tanX) + (tanY * tanY));

        // Rejects a degenerate projection (a zero or NaN term anywhere above lands here) rather than letting it
        // poison the search distance.
        if (!(secant >= 1.0f) || !Float.isFinite(secant)) {
            return 0.0f;
        }

        return secant;
    }

    private float getRenderDistance() {
        return this.renderDistance * 16.0f;
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(PositionUtil.packSection(x, y, z));
    }

    public Collection<RenderSection> getAllRenderSections() {
        return Collections.unmodifiableCollection(this.sectionByPosition.values());
    }

    private Object2LongMap<TerrainRenderPass> computeRenderPassTimingsMap() {
        Object2LongOpenHashMap<TerrainRenderPass> map = new Object2LongOpenHashMap<>();
        for (var entry : renderPassDrawTimers.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getLastTime());
        }
        return map;
    }

    protected final Supplier<Object2LongMap<TerrainRenderPass>> renderPassTimingsDebounced = new ExpiringSupplier<>(this::computeRenderPassTimingsMap, 1, TimeUnit.SECONDS);

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        int count = 0, indexCount = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        long indexUsed = 0, indexAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            var buffer = resources.getGeometryArena();

            deviceUsed += buffer.getDeviceUsedMemoryL();
            deviceAllocated += buffer.getDeviceAllocatedMemoryL();

            var indexBuffer = resources.getIndexArena();

            if (indexBuffer != null) {
                indexUsed += indexBuffer.getDeviceUsedMemoryL();
                indexAllocated += indexBuffer.getDeviceAllocatedMemoryL();
                indexCount++;
            }

            count++;
        }

        list.add(String.format("G: %d/%d, I: %d/%d MiB (%d buffers)", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated), MathUtil.toMib(indexUsed), MathUtil.toMib(indexAllocated), count));
        list.add(String.format("Transfer Queue: %s", this.regions.getStagingBuffer().toString()));

        var rebuildLists = this.getCurrentRenderListManager().getRebuildLists();

        list.add(String.format("Chunk Queues: U=%02d (P0=%03d | P1=%03d | P2=%03d) S=%03d/%03d",
                this.buildResults.size(),
                rebuildLists.getUpdateCount(ChunkUpdateType.IMPORTANT_REBUILD),
                rebuildLists.getUpdateCount(ChunkUpdateType.REBUILD),
                rebuildLists.getUpdateCount(ChunkUpdateType.INITIAL_BUILD),
                rebuildLists.getUpdateCount(ChunkUpdateType.IMPORTANT_SORT),
                rebuildLists.getUpdateCount(ChunkUpdateType.SORT)
        ));

        var debugStats = renderListManager.getDebugStatistics();

        var counts = debugStats.renderPassCounts().object2IntEntrySet().stream().sorted(Comparator.comparingInt(e -> -e.getIntValue())).iterator();

        var timingMap = renderPassTimingsDebounced.get();

        while (counts.hasNext()) {
            var entry = counts.next();
            var duration = timingMap.getLong(entry.getKey());
            String time;
            if (duration == 0) {
                time = "?? ms";
            } else {
                time = TimeUtil.stringifyTime(duration, TimeUnit.NANOSECONDS);
            }

            list.add(entry.getKey().name() + " - " + entry.getIntValue() + " sections, " + time);
        }

        if (renderListManager.getRenderLists().getPasses().stream().anyMatch(TerrainRenderPass::isSorted)) {
            list.add(debugStats.getSortingString());
            list.add(String.format("Tree Sort: %d planes, %d trig/s, %d sorts/s, %s avg",
                    this.cutPlaneIndex.planeCount(), this.treeTriggersLastSecond,
                    this.treeSortMetrics.getObservationsInLastTimeInterval(),
                    TimeUtil.stringifyTime((long) this.treeSortMetrics.getAverageNanos(0), TimeUnit.NANOSECONDS)));
        }

        return list;
    }

    private RenderListManager getCurrentRenderListManager() {
        return isInShadowPass() ? this.shadowRenderListManager : this.renderListManager;
    }

    public SortedRenderLists getRenderLists() {
        return this.getCurrentRenderListManager().getRenderLists();
    }

    public boolean isSectionBuilt(int x, int y, int z) {
        var section = this.getRenderSection(x, y, z);
        return section != null && section.isBuilt();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.minSection; y < this.maxSection; y++) {
            this.onSectionAdded(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.minSection; y < this.maxSection; y++) {
            this.onSectionRemoved(x, y, z);
        }
    }

    public void toggleRenderingForTerrainPass(TerrainRenderPass pass) {
        if(this.disabledRenderPasses.contains(pass)) {
            this.disabledRenderPasses.remove(pass);
        } else {
            this.disabledRenderPasses.add(pass);
        }
    }

    public final Collection<RenderSection> getSectionsWithGlobalEntities() {
        return ReferenceSets.unmodifiable(this.sectionsWithGlobalEntities);
    }

    public String getTickerDebugString() {
        return this.getCurrentRenderListManager().getTickerDebugString();
    }
}
