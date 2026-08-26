package org.embeddedt.embeddium.impl.render.chunk.multidraw.bench;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.BenchRenderPasses;
import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.VoxelSectionBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkTaskOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobResult;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.SortedRenderLists;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.BatchAssembler;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.BenchPlatform;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.Cameras;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.SyntheticWorld;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.WorldType;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CPU half of multidraw command emission: one frame of {@link BatchAssembler#fillRegion} over visible regions.
 * Render lists and mesh data are real (occlusion search + voxel meshing + upload), not hand-written.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class MultiDrawBench {
    private static final int ROTATION_STEPS = 8;
    private static final int TARGET_QUEUE_SIZE = 32 * 10;
    private static final int UPLOAD_BATCH = 256;

    @State(Scope.Benchmark)
    public static class FrameState {
        @Param({ "SURFACE" })
        public WorldType world;

        /** Drop to 16 for a quick run; 32 meshes ~10k sections per trial. */
        @Param({ "32" })
        public int renderDistance;

        /** See {@link org.embeddedt.embeddium.impl.render.chunk.bench.voxel.VoxelWorld#DEFAULT_CAVE_WIDTH}. */
        @Param({ "0.03" })
        public double caveWidth;

        /** True uses the cull-mask dispatch (what the game does); false collapses to a single run. */
        @Param({ "true" })
        public boolean useBlockFaceCulling;

        public SyntheticWorld syntheticWorld;
        public SectionLattice lattice;
        public CameraTransform camera;

        public SortedRenderLists[] frames;

        private ChunkBuildContext buildContext;
        private int rotationIndex;
        private int frameCounter = 1;

        @Setup
        public void setup() {
            BenchPlatform.ensureInitialized();

            this.syntheticWorld = new SyntheticWorld(this.world, this.renderDistance, SyntheticWorld.DEFAULT_SEED,
                    this.caveWidth, true, 0.0D, false);

            this.lattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, false);

            for (RenderSection section : this.syntheticWorld.getConstructionOrder()) {
                this.lattice.attach(section);
            }

            double cameraX = 8.5, cameraZ = 8.5;
            double cameraY = Cameras.surfaceCameraY(0, 0);

            this.camera = new CameraTransform(cameraX, cameraY, cameraZ);

            var viewports = Cameras.yawRing(cameraX, cameraY, cameraZ, ROTATION_STEPS, Cameras.DEFAULT_PITCH,
                    this.renderDistance);

            // First pass: find visible sections to mesh. Second pass (after upload): cache for measurement.
            this.uploadGeometry(this.visibleSections(viewports), (float) cameraX, (float) cameraY, (float) cameraZ);

            this.frames = new SortedRenderLists[ROTATION_STEPS];

            for (int i = 0; i < ROTATION_STEPS; i++) {
                this.frames[i] = this.collect(viewports[i]);
            }

        }

        /** {@return all sections visible in any cached frame, deduplicated} */
        private List<RenderSection> visibleSections(Viewport[] viewports) {
            var seen = new HashSet<Long>();
            var sections = new ArrayList<RenderSection>();

            for (Viewport viewport : viewports) {
                var lists = this.collect(viewport);
                var iterator = lists.iterator(false);

                while (iterator.hasNext()) {
                    ChunkRenderList list = iterator.next();
                    RenderRegion region = list.getRegion();

                    byte[] indices = list.getSectionsWithGeometry();

                    for (int i = 0; i < list.getSectionsWithGeometryCount(); i++) {
                        int index = indices[i] & 0xFF;

                        var section = this.syntheticWorld.getSection(
                                region.getChunkX() + LocalSectionIndex.unpackX(index),
                                region.getChunkY() + LocalSectionIndex.unpackY(index),
                                region.getChunkZ() + LocalSectionIndex.unpackZ(index));

                        if (section != null && seen.add(section.positionAsLong())) {
                            sections.add(section);
                        }
                    }
                }
            }

            return sections;
        }

        /** Meshes and uploads through the real path. {@code RenderRegionManager.update()} is skipped (no draw). */
        private void uploadGeometry(List<RenderSection> sections, float cameraX, float cameraY, float cameraZ) {
            var regionManager = BenchPlatform.regionManager();
            var builder = new VoxelSectionBuilder();
            var voxelWorld = this.syntheticWorld.getVoxelWorld();

            this.buildContext = new ChunkBuildContext(BenchRenderPasses.configuration());

            var pending = new ArrayList<ChunkJobResult.Success<? extends ChunkTaskOutput>>(UPLOAD_BATCH);

            try (var commandList = RenderDevice.INSTANCE.createCommandList()) {
                for (RenderSection section : sections) {
                    var slice = voxelWorld.slice(section.getChunkX(), section.getChunkY(), section.getChunkZ());
                    ChunkBuildOutput output = builder.build(this.buildContext, section, slice,
                            cameraX, cameraY, cameraZ);

                    if (output == null) {
                        continue;
                    }

                    pending.add(new ChunkJobResult.Success<>(output, 0L));

                    if (pending.size() >= UPLOAD_BATCH) {
                        regionManager.uploadMeshes(commandList, pending, () -> { });
                        pending.clear();
                    }
                }

                if (!pending.isEmpty()) {
                    regionManager.uploadMeshes(commandList, pending, () -> { });
                }
            }
        }

        /** One frame through the real collector. */
        private SortedRenderLists collect(Viewport viewport) {
            int frame = ++this.frameCounter;

            int regionIds = BenchPlatform.regionManager().getRegionIdsLength();
            var collector = new VisibleChunkCollector(this.lattice, frame, regionIds, TARGET_QUEUE_SIZE);

            this.lattice.ensureWindowCovers(viewport.getChunkCoord(), this.syntheticWorld.getSearchDistance());
            this.lattice.findVisible(collector, viewport, this.syntheticWorld.getSearchDistance(),
                    regionIds, true, true, frame);

            return collector.createRenderLists();
        }

        public SortedRenderLists nextFrame() {
            int index = this.rotationIndex;
            this.rotationIndex = (index + 1) % ROTATION_STEPS;
            return this.frames[index];
        }

        @TearDown
        public void tearDown() {
            if (this.buildContext != null) {
                this.buildContext.cleanup();
            }
        }
    }

    /** Unsorted pass with cull-mask dispatch (the main case). */
    @Benchmark
    public void emitSolid(FrameState state, Blackhole bh) {
        bh.consume(emit(state, BenchRenderPasses.SOLID));
    }

    /** Second unsorted pass — colder storage. */
    @Benchmark
    public void emitCutout(FrameState state, Blackhole bh) {
        bh.consume(emit(state, BenchRenderPasses.CUTOUT_MIPPED));
    }

    /** Sorted pass: one command per populated facing. */
    @Benchmark
    public void emitTranslucent(FrameState state, Blackhole bh) {
        bh.consume(emit(state, BenchRenderPasses.TRANSLUCENT));
    }

    /** {@return total draw commands emitted for one frame of a single pass} */
    private static int emit(FrameState state, TerrainRenderPass pass) {
        var camera = state.camera;
        boolean useBlockFaceCulling = state.useBlockFaceCulling && !pass.isSorted();

        var iterator = state.nextFrame().iterator(pass.isReverseOrder());

        int commands = 0;

        while (iterator.hasNext()) {
            ChunkRenderList renderList = iterator.next();

            var region = renderList.getRegion();
            var storage = region.getStorage(pass);

            if (storage == null) {
                continue;
            }

            var batch = BatchAssembler.fillRegion(region, storage, renderList, camera, pass, useBlockFaceCulling);

            if (batch != null) {
                commands += batch.size();
                batch.delete();
            }
        }

        return commands;
    }
}
