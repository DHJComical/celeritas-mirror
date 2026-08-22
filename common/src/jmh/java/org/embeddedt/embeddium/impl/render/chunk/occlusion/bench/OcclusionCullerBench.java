package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * One frame of chunk-occlusion graph search. Use at least two forks; a single fork cannot separate real effects
 * from heap-placement luck on this memory-bound workload.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class OcclusionCullerBench {
    private static final int ROTATION_STEPS = 8;
    private static final int TARGET_QUEUE_SIZE = 32 * 10;

    @State(Scope.Benchmark)
    public static class WorldState {
        @Param({ "SURFACE" })
        public WorldType world;

        @Param({ "32" })
        public int renderDistance;

        /** True = shuffled allocation order (realistic for a long-lived session); false = best case. */
        @Param({ "true" })
        public boolean scatteredAlloc;

        public SyntheticWorld syntheticWorld;
        public SectionLattice lattice;

        public float searchDistance;
        public int numRegions;

        public Viewport staticViewport;
        public Viewport[] rotatingViewports;

        /** Must increment per invocation; the culler early-outs on a reused frame number. */
        private int frame = 1;
        private int rotationIndex;

        @Setup
        public void setup() {
            BenchPlatform.ensureInitialized();

            this.syntheticWorld = new SyntheticWorld(this.world, this.renderDistance, this.scatteredAlloc);
            this.searchDistance = this.syntheticWorld.getSearchDistance();

            this.numRegions = BenchPlatform.regionManager().getRegionIdsLength();

            this.lattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y);

            for (RenderSection section : this.syntheticWorld.getConstructionOrder()) {
                this.lattice.attach(section);
            }

            double y = Cameras.surfaceCameraY(0, 0);

            this.staticViewport = Cameras.viewport(8.5, y, 8.5, this.renderDistance);
            this.rotatingViewports = Cameras.yawRing(8.5, y, 8.5, ROTATION_STEPS, Cameras.DEFAULT_PITCH,
                    this.renderDistance);
        }

        public int nextFrame() {
            return ++this.frame;
        }

        public Viewport nextRotation() {
            int index = this.rotationIndex;
            this.rotationIndex = (index + 1) % ROTATION_STEPS;
            return this.rotatingViewports[index];
        }

        public void search(OcclusionCuller.Visitor visitor, Viewport viewport) {
            this.lattice.ensureWindowCovers(viewport.getChunkCoord(), this.searchDistance);
            this.lattice.findVisible(visitor, viewport, this.searchDistance, this.numRegions, true, true, this.nextFrame());
        }

        /** One frame through the real {@code VisibleChunkCollector}. */
        public VisibleChunkCollector collect(Viewport viewport) {
            var collector = new VisibleChunkCollector(this.lattice, this.frame, this.numRegions, TARGET_QUEUE_SIZE);
            this.search(collector, viewport);

            return collector;
        }
    }

    /** Fixed camera — cheapest case (region cull cache always hits). */
    @Benchmark
    public void staticCamera(WorldState state, Blackhole bh) {
        consume(state.collect(state.staticViewport), bh);
    }

    /** Rotating camera — the realistic case. */
    @Benchmark
    public void rotatingCamera(WorldState state, Blackhole bh) {
        consume(state.collect(state.nextRotation()), bh);
    }

    /** Static camera through {@link CountingVisitor}: isolates traversal from collector cost. */
    @Benchmark
    public void staticCameraCountingOnly(WorldState state, Blackhole bh) {
        var visitor = new CountingVisitor();
        state.search(visitor, state.staticViewport);
        bh.consume(visitor.fold());
    }

    /** Both outputs must reach the blackhole or the JIT can drop the work. */
    private static void consume(VisibleChunkCollector collector, Blackhole bh) {
        bh.consume(collector.createRenderLists());
        bh.consume(collector.getRebuildLists());
    }
}
