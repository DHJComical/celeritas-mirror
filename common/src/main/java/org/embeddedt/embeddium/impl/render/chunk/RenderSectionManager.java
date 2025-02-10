package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobCollector;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderSortTask;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegionManager;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.embeddedt.embeddium.impl.util.iterator.ByteIterator;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class RenderSectionManager {
    private final ChunkBuilder builder;

    private final Thread renderThread = Thread.currentThread();

    private final RenderRegionManager regions;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final ConcurrentLinkedDeque<ChunkJobResult<ChunkBuildOutput>> buildResults = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Runnable> asyncSubmittedTasks = new ConcurrentLinkedDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final OcclusionCuller occlusionCuller;

    private final int renderDistance;

    @NotNull
    private SortedRenderLists renderLists, shadowRenderLists;

    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    private int lastUpdatedFrame;

    private boolean needsUpdate;

    protected @Nullable Vector3i lastCameraPosition;
    protected Vector3d cameraPosition = new Vector3d();

    protected boolean translucencySorting;

    @Getter
    private final RenderPassConfiguration<?> renderPassConfiguration;

    private final Set<TerrainRenderPass> disabledRenderPasses;

    private final int minSection, maxSection;

    public RenderSectionManager(RenderPassConfiguration<?> configuration, Supplier<ChunkBuildContext> contextSupplier, BiFunction<RenderDevice, ChunkVertexType, ChunkRenderer> chunkRenderer, int renderDistance, CommandList commandList, int minSection, int maxSection, int requestedThreads) {
        this.chunkRenderer = chunkRenderer.apply(RenderDevice.INSTANCE, configuration.vertexType());

        this.renderPassConfiguration = configuration;

        this.builder = new ChunkBuilder(this::managedBlock, contextSupplier, requestedThreads);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList, this.renderPassConfiguration);

        this.renderLists = SortedRenderLists.empty();
        this.shadowRenderLists = SortedRenderLists.empty();
        this.minSection = minSection;
        this.maxSection = maxSection;
        this.occlusionCuller = new OcclusionCuller(Long2ReferenceMaps.unmodifiable(this.sectionByPosition), this.minSection, this.maxSection);

        this.rebuildLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            this.rebuildLists.put(type, new ArrayDeque<>());
        }

        this.disabledRenderPasses = new ReferenceArraySet<>();
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
    }

    /**
     * Whether terrain is being rendered for shadows.
     */
    public boolean isInShadowPass() {
        return false;
    }

    public void update(PositionedViewport positionedViewport, int frame, boolean spectator) {
        this.lastCameraPosition = positionedViewport.blockPosition();
        this.cameraPosition = positionedViewport.position();

        this.createTerrainRenderList(positionedViewport, frame, spectator);

        if (isInShadowPass()) {
            return;
        }

        this.needsUpdate = false;
        this.lastUpdatedFrame = frame;
    }

    private void checkTranslucencyChange() {
        if(!this.translucencySorting || lastCameraPosition == null)
            return;

        int camSectionX = PositionUtil.posToSectionCoord(cameraPosition.x);
        int camSectionY = PositionUtil.posToSectionCoord(cameraPosition.y);
        int camSectionZ = PositionUtil.posToSectionCoord(cameraPosition.z);

        this.scheduleTranslucencyUpdates(camSectionX, camSectionY, camSectionZ);
    }

    private void scheduleTranslucencyUpdates(int camSectionX, int camSectionY, int camSectionZ) {
        var sortRebuildList = this.rebuildLists.get(ChunkUpdateType.SORT);
        var importantSortRebuildList = this.rebuildLists.get(ChunkUpdateType.IMPORTANT_SORT);
        var allowImportant = allowImportantRebuilds();
        var translucentPass = this.renderPassConfiguration.defaultTranslucentMaterial().pass;
        if (!this.getRenderLists().hasPass(translucentPass)) {
            return;
        }
        for (Iterator<ChunkRenderList> it = this.getRenderLists().iterator(); it.hasNext(); ) {
            ChunkRenderList entry = it.next();
            var region = entry.getRegion();
            if (!region.hasSectionsInPass(translucentPass)) {
                continue;
            }
            ByteIterator sectionIterator = entry.sectionsWithGeometryIterator(false);
            if (sectionIterator == null) {
                continue;
            }
            while (sectionIterator.hasNext()) {
                var section = region.getSection(sectionIterator.nextByteAsInt());

                if (section == null || !section.isBuilt()) {
                    // Nonexistent/unbuilt sections are not relevant
                    continue;
                }

                boolean hasTranslucentData = section.isNeedsDynamicTranslucencySorting();

                if (!hasTranslucentData) {
                    // Sections without sortable translucent data are not relevant
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

                if (cameraChangedSection || section.isAlignedWithSectionOnGrid(camSectionX, camSectionY, camSectionZ)) {
                    section.setPendingUpdate(update);
                    // Inject it into the rebuild lists
                    (update == ChunkUpdateType.IMPORTANT_SORT ? importantSortRebuildList : sortRebuildList).add(section);

                    section.lastCameraX = cameraPosition.x;
                    section.lastCameraY = cameraPosition.y;
                    section.lastCameraZ = cameraPosition.z;
                }
            }
        }
    }

    protected abstract boolean shouldRespectUpdateTaskQueueSizeLimit();

    private void createTerrainRenderList(PositionedViewport positionedViewport, int frame, boolean spectator) {
        this.resetRenderLists();

        final var searchDistance = this.getSearchDistance();
        final var useOcclusionCulling = this.shouldUseOcclusionCulling(positionedViewport, spectator);

        var visitor = new VisibleChunkCollector(frame, !this.shouldRespectUpdateTaskQueueSizeLimit());

        this.occlusionCuller.findVisible(visitor, positionedViewport.viewport(), searchDistance, useOcclusionCulling, frame);

        this.setRenderLists(visitor.createRenderLists());
        this.rebuildLists = visitor.getRebuildLists();

        this.checkTranslucencyChange();
    }

    protected abstract boolean useFogOcclusion();

    private float getSearchDistance() {
        float distance;

        // TODO: does *every* shaderpack really disable fog?
        if (this.useFogOcclusion() && !ShaderModBridge.areShadersEnabled()) {
            distance = this.getEffectiveRenderDistance();
        } else {
            distance = this.getRenderDistance();
        }

        return distance;
    }

    protected abstract boolean shouldUseOcclusionCulling(PositionedViewport positionedViewport, boolean spectator);

    private void resetRenderLists() {
        this.setRenderLists(SortedRenderLists.empty());

        for (var list : this.rebuildLists.values()) {
            list.clear();
        }
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

        this.sectionByPosition.put(key, renderSection);

        if (this.isSectionVisuallyEmpty(x, y, z)) {
            this.updateSectionInfo(renderSection, ContextBundle.empty());
        } else {
            renderSection.setPendingUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.connectNeighborNodes(renderSection);

        this.needsUpdate = true;
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

        this.disconnectNeighborNodes(section);
        this.updateSectionInfo(section, null);

        section.delete();

        this.needsUpdate = true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        if (disabledRenderPasses.contains(pass)) {
            return;
        }

        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.getRenderLists(), pass, new CameraTransform(x, y, z));

        commandList.flush();
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.getRenderSection(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == this.lastUpdatedFrame;
    }

    private boolean rebuildListHasUpdates() {
        for (var queue : this.rebuildLists.values()) {
            if (!queue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void updateChunks(boolean updateImmediately) {
        this.regions.update();

        if (!rebuildListHasUpdates()) {
            return;
        }

        var blockingRebuilds = new ChunkJobCollector(Integer.MAX_VALUE, this.buildResults::add);
        var deferredRebuilds = new ChunkJobCollector(this.builder.getSchedulingBudget(), this.buildResults::add);

        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_REBUILD);
        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_SORT);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.REBUILD);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.INITIAL_BUILD);

        // Count sort tasks as requiring a quarter of the resources of a mesh task
        var deferredSorts = new ChunkJobCollector(Math.max(4, this.builder.getSchedulingBudget() * 4), this.buildResults::add);
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

        this.processChunkBuildResults(results);

        for (var result : results) {
            result.delete();
        }
    }

    public void tickVisibleRenders() {

    }

    private void processChunkBuildResults(ArrayList<ChunkBuildOutput> results) {
        var filtered = filterChunkBuildResults(results);

        this.regions.uploadMeshes(RenderDevice.INSTANCE.createCommandList(), filtered);

        for (var result : filtered) {
            if(result.info != null) {
                // The chunk graph must be rebuilt whenever a section is remeshed, in order to consider changes in
                // geometry, visibility data, etc.
                this.needsUpdate = true;

                this.updateSectionInfo(result.render, result.info);
                if (this.translucencySorting) {
                    // We only change the translucency info on full rebuilds, as sorts can keep using the same data
                    this.updateTranslucencyInfo(result.render, result.meshes);
                }
            }

            var job = result.render.getBuildCancellationToken();

            if (job != null && result.buildTime >= result.render.getLastSubmittedFrame()) {
                result.render.setBuildCancellationToken(null);
            }

            result.render.setLastBuiltFrame(result.buildTime);
        }
    }

    private void updateTranslucencyInfo(RenderSection render, Map<TerrainRenderPass, BuiltSectionMeshParts> meshes) {
        Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> sortStates = new Reference2ObjectArrayMap<>();
        for(var entry : meshes.entrySet()) {
            if(entry.getKey().isSorted()) {
                sortStates.put(entry.getKey(), entry.getValue().getSortState().compactForStorage());
            }
        }
        render.setTranslucencySortStates(sortStates.isEmpty() ? Collections.emptyMap() : sortStates);
    }

    @MustBeInvokedByOverriders
    protected void updateSectionInfo(RenderSection render, ContextBundle<RenderSection> info) {
        render.setInfo(info);
    }

    private static List<ChunkBuildOutput> filterChunkBuildResults(ArrayList<ChunkBuildOutput> outputs) {
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, ChunkBuildOutput>();

        for (var output : outputs) {
            if (output.render.isDisposed() || output.render.getLastBuiltFrame() > output.buildTime) {
                continue;
            }

            var render = output.render;
            var previous = map.get(render);

            if (previous == null || previous.buildTime < output.buildTime) {
                map.put(render, output);
            }
        }

        return new ArrayList<>(map.values());
    }

    private ArrayList<ChunkBuildOutput> collectChunkBuildResults() {
        ArrayList<ChunkBuildOutput> results = new ArrayList<>();
        ChunkJobResult<ChunkBuildOutput> result;

        while ((result = this.buildResults.poll()) != null) {
            results.add(result.unwrap());
        }

        return results;
    }

    private void submitRebuildTasks(ChunkJobCollector collector, ChunkUpdateType type) {
        var queue = this.rebuildLists.get(type);

        while (!queue.isEmpty() && collector.canOffer()) {
            RenderSection section = queue.remove();

            if (section.isDisposed()) {
                continue;
            }

            // Because Sodium creates the update queue on the frame before it's processed,
            // the update type might no longer match. Filter out such a scenario.
            if (section.getPendingUpdate() != type) {
                continue;
            }

            int frame = this.lastUpdatedFrame;
            ChunkBuilderTask<ChunkBuildOutput> task = type.isSort() ? this.createSortTask(section, frame) : this.createRebuildTask(section, frame);

            if (task == null && type.isSort()) {
                // Ignore sorts that became invalid
                section.setPendingUpdate(null);
                continue;
            }

            if (task != null) {
                var job = this.builder.scheduleTask(task, type.isImportant(), collector::onJobFinished);
                collector.addSubmittedJob(job);

                section.setBuildCancellationToken(job);

                if (!type.isSort()) {
                    // Prevent further sorts from being performed on this section
                    section.setTranslucencySortStates(Collections.emptyMap());
                }
            } else {
                var result = ChunkJobResult.successfully(new ChunkBuildOutput(section, ContextBundle.empty(), Reference2ReferenceMaps.emptyMap(), frame));
                this.buildResults.add(result);

                section.setBuildCancellationToken(null);
            }

            section.setLastSubmittedFrame(frame);
            section.setPendingUpdate(null);
        }
    }

    protected abstract @Nullable ChunkBuilderTask<ChunkBuildOutput> createRebuildTask(RenderSection render, int frame);

    public ChunkBuilderSortTask createSortTask(RenderSection render, int frame) {
        Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> sortStates = render.getTranslucencySortStates();
        if(sortStates.isEmpty() || sortStates.values().stream().noneMatch(TranslucentQuadAnalyzer.SortState::requiresDynamicSorting))
            return null;
        return new ChunkBuilderSortTask(render, (float)cameraPosition.x, (float)cameraPosition.y, (float)cameraPosition.z, frame, sortStates);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean needsUpdate() {
        return this.needsUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        for (var result : this.collectChunkBuildResults()) {
            result.delete(); // delete resources for any pending tasks (including those that were cancelled)
        }

        this.resetRenderLists();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.getRenderLists().iterator();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    public void scheduleAsyncTask(Runnable runnable) {
        asyncSubmittedTasks.add(runnable);
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

    protected void scheduleSectionForRebuild(int x, int y, int z, boolean important) {
        RenderSection section = this.sectionByPosition.get(PositionUtil.packSection(x, y, z));

        if (section != null) {
            ChunkUpdateType pendingUpdate;

            if (allowImportantRebuilds() && (important || this.shouldPrioritizeRebuild(section))) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_REBUILD;
            } else {
                pendingUpdate = ChunkUpdateType.REBUILD;
            }

            pendingUpdate = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(), pendingUpdate);
            if (pendingUpdate != null) {
                section.setPendingUpdate(pendingUpdate);

                this.needsUpdate = true;
            }
        }
    }

    private static final float NEARBY_REBUILD_DISTANCE = MathUtil.square(16.0f);

    private boolean shouldPrioritizeRebuild(RenderSection section) {
        return this.lastCameraPosition != null && section.getSquaredDistanceFromBlockCenter(this.lastCameraPosition.x(), this.lastCameraPosition.y(), this.lastCameraPosition.z()) < NEARBY_REBUILD_DISTANCE;
    }

    protected abstract boolean allowImportantRebuilds();

    private float getEffectiveRenderDistance() {
        var color = ChunkShaderFogComponent.FOG_SERVICE.getFogColor();
        var alpha = color[3];
        var distance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        var renderDistance = this.getRenderDistance();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (Math.abs(alpha - 1.0f) >= 1.0E-5F) {
            return renderDistance;
        }

        return Math.min(renderDistance, distance + 0.5f);
    }

    private float getRenderDistance() {
        return this.renderDistance * 16.0f;
    }

    private void connectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(PositionUtil.packSection(x, y, z));
    }

    private Collection<String> getSortingStrings() {
        List<String> list = new ArrayList<>();

        int[] sectionCounts = new int[TranslucentQuadAnalyzer.Level.VALUES.length];

        for (Iterator<ChunkRenderList> it = this.getRenderLists().iterator(); it.hasNext(); ) {
            var renderList = it.next();
            var region = renderList.getRegion();
            var listIter = renderList.sectionsWithGeometryIterator(false);
            if(listIter != null) {
                while(listIter.hasNext()) {
                    RenderSection section = region.getSection(listIter.nextByteAsInt());
                    // Do not count sections without translucent data
                    if(section == null || section.getTranslucencySortStates().isEmpty()) {
                        continue;
                    }

                    sectionCounts[section.getHighestSortingLevel().ordinal()]++;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
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

        list.add(sb.toString());

        return list;
    }

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

        list.add(String.format("Geometry Pool: %d/%d MiB (%d buffers)", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated), count));
        if (indexCount > 0) {
            list.add(String.format("Index Pool: %d/%d MiB (%d buffers)", MathUtil.toMib(indexUsed), MathUtil.toMib(indexAllocated), indexCount));
        }
        list.add(String.format("Transfer Queue: %s", this.regions.getStagingBuffer().toString()));

        list.add(String.format("Chunk Builder: Permits=%02d | Busy=%02d | Total=%02d",
                this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), this.builder.getTotalThreadCount())
        );

        list.add(String.format("Chunk Queues: U=%02d (P0=%03d | P1=%03d | P2=%03d)",
                this.buildResults.size(),
                this.rebuildLists.get(ChunkUpdateType.IMPORTANT_REBUILD).size(),
                this.rebuildLists.get(ChunkUpdateType.REBUILD).size(),
                this.rebuildLists.get(ChunkUpdateType.INITIAL_BUILD).size())
        );

        if(this.translucencySorting) {
            list.addAll(getSortingStrings());
        }

        return list;
    }

    public @NotNull SortedRenderLists getRenderLists() {
        return isInShadowPass() ? this.shadowRenderLists : this.renderLists;
    }

    protected void setRenderLists(@NotNull SortedRenderLists renderLists) {
        if (isInShadowPass()) {
            this.shadowRenderLists = renderLists;
        } else {
            this.renderLists = renderLists;
        }
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
}
