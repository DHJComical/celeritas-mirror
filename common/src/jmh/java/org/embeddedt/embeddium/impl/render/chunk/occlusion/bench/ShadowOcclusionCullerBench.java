package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.lists.VisibleChunkCollector;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.joml.Vector3f;
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
 * One frame of the shadow-pass section search: the receiver-driven, light-monotone BFS rooted at the cells the
 * terrain search reported visible.
 *
 * <p>{@code frustumScan*} runs the same shadow viewport through the old frustum-only scan (the {@code lightVector
 * == null} path), which is both the pre-existing behaviour and the upper bound the new search has to beat.
 *
 * <p>The {@code shadowSearch*} benchmarks reuse one terrain search's root set across every invocation: the roots stay
 * valid until the next terrain search, and holding them fixed keeps the terrain search's cost out of the measurement.
 * {@code fullFrame} runs both searches per invocation for the end-to-end number.
 *
 * <p>Use at least two forks; like {@link OcclusionCullerBench} this workload is memory-bound and one fork cannot
 * separate a real effect from heap placement.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class ShadowOcclusionCullerBench {
    private static final int LIGHT_STEPS = 8;
    private static final int TARGET_QUEUE_SIZE = 32 * 10;

    @State(Scope.Benchmark)
    public static class ShadowState {
        @Param({ "SURFACE" })
        public WorldType world;

        @Param({ "32" })
        public int renderDistance;

        /** Shadow render distance in chunks; bounds the shadow search independently of the terrain one. */
        @Param({ "16" })
        public int shadowDistance;

        /** Sun height above the horizon. Low sun means long, shallow rays and the deepest searches. */
        @Param({ "45" })
        public float sunElevation;

        /** True = shuffled allocation order (realistic for a long-lived session); false = best case. */
        @Param({ "true" })
        public boolean scatteredAlloc;

        public SectionLattice lattice;

        public float searchDistance;
        public float shadowSearchDistance;
        public int numRegions;

        public Viewport cameraViewport;
        public Viewport staticShadowViewport;
        public Viewport[] rotatingShadowViewports;
        public Vector3f staticLight;
        public Vector3f[] rotatingLights;

        /** Must increment per invocation; the culler early-outs on a reused frame number. */
        private int frame = 1;
        private int lightIndex;

        @Setup
        public void setup() {
            BenchPlatform.ensureInitialized();

            var syntheticWorld = new SyntheticWorld(this.world, this.renderDistance, this.scatteredAlloc);
            this.searchDistance = syntheticWorld.getSearchDistance();
            this.shadowSearchDistance = this.shadowDistance * 16.0f;
            this.numRegions = BenchPlatform.regionManager().getRegionIdsLength();

            // hasShadowPass: allocates the second visit-state array the shadow search stamps.
            this.lattice = new SectionLattice(SyntheticWorld.MIN_SECTION_Y, SyntheticWorld.MAX_SECTION_Y, true, false);

            for (RenderSection section : syntheticWorld.getConstructionOrder()) {
                this.lattice.attach(section);
            }

            double y = Cameras.surfaceCameraY(0, 0);

            this.cameraViewport = Cameras.viewport(8.5, y, 8.5, this.renderDistance);

            this.staticLight = Cameras.lightVector(0.0f, this.sunElevation);
            this.rotatingLights = Cameras.lightRing(LIGHT_STEPS, this.sunElevation);

            this.staticShadowViewport = Cameras.shadowViewport(8.5, y, 8.5, this.staticLight,
                    this.shadowSearchDistance);
            this.rotatingShadowViewports = new Viewport[LIGHT_STEPS];

            for (int i = 0; i < LIGHT_STEPS; i++) {
                this.rotatingShadowViewports[i] = Cameras.shadowViewport(8.5, y, 8.5, this.rotatingLights[i],
                        this.shadowSearchDistance);
            }

            // Seed the root set once. The shadow-only benchmarks reuse it; fullFrame refreshes it every invocation.
            // The cells the terrain search reports visible are exactly the ones it records as roots.
            System.out.println("# shadow bench roots: " + this.terrainSearch().visible + " visible cells");
        }

        public int nextFrame() {
            return ++this.frame;
        }

        public int nextLight() {
            int index = this.lightIndex;
            this.lightIndex = (index + 1) % LIGHT_STEPS;
            return index;
        }

        /** The terrain search that produces the shadow search's root set. */
        public CountingVisitor terrainSearch() {
            var visitor = new CountingVisitor();

            this.lattice.ensureWindowCovers(this.cameraViewport.getChunkCoord(), this.searchDistance);
            this.lattice.findVisible(visitor, this.cameraViewport, this.searchDistance,
                    this.numRegions, true, true, this.nextFrame());

            return visitor;
        }

        /** One shadow search. A null {@code light} selects the frustum-only scan. */
        public void shadowSearch(CountingVisitor visitor, Viewport shadowViewport, Vector3f light) {
            this.lattice.findShadowVisible(visitor, shadowViewport, this.shadowSearchDistance, this.numRegions,
                    light, this.nextFrame());
        }

        /** One shadow search through the real {@code VisibleChunkCollector}. */
        public VisibleChunkCollector collectShadow(Viewport shadowViewport, Vector3f light) {
            var collector = new VisibleChunkCollector(this.lattice, this.frame, this.numRegions, TARGET_QUEUE_SIZE);
            this.lattice.findShadowVisible(collector, shadowViewport, this.shadowSearchDistance, this.numRegions,
                    light, this.nextFrame());

            return collector;
        }
    }

    /** Fixed sun — cheapest case (the region cull cache always hits). */
    @Benchmark
    public void shadowSearch(ShadowState state, Blackhole bh) {
        var visitor = new CountingVisitor();
        state.shadowSearch(visitor, state.staticShadowViewport, state.staticLight);
        bh.consume(visitor.fold());
    }

    /** Moving sun — the realistic case, and the one that re-derives the direction masks every frame. */
    @Benchmark
    public void rotatingLight(ShadowState state, Blackhole bh) {
        int index = state.nextLight();
        var visitor = new CountingVisitor();
        state.shadowSearch(visitor, state.rotatingShadowViewports[index], state.rotatingLights[index]);
        bh.consume(visitor.fold());
    }

    /** Fixed sun through the real collector: traversal plus the render-list build the shadow pass actually pays. */
    @Benchmark
    public void shadowSearchCollector(ShadowState state, Blackhole bh) {
        consume(state.collectShadow(state.staticShadowViewport, state.staticLight), bh);
    }

    /** Baseline: the frustum-only scan the shadow pass used before, over the same viewport. */
    @Benchmark
    public void frustumScan(ShadowState state, Blackhole bh) {
        var visitor = new CountingVisitor();
        state.shadowSearch(visitor, state.staticShadowViewport, null);
        bh.consume(visitor.fold());
    }

    /**
     * Terrain search plus shadow search, the way a shadowed frame runs them. Also the only benchmark that runs a
     * terrain search over a shadow-enabled lattice, so it is where the visible-cell recording is paid for.
     */
    @Benchmark
    public void fullFrame(ShadowState state, Blackhole bh) {
        bh.consume(state.terrainSearch().fold());

        int index = state.nextLight();
        var visitor = new CountingVisitor();
        state.shadowSearch(visitor, state.rotatingShadowViewports[index], state.rotatingLights[index]);
        bh.consume(visitor.fold());
    }

    /** Both outputs must reach the blackhole or the JIT can drop the work. */
    private static void consume(VisibleChunkCollector collector, Blackhole bh) {
        bh.consume(collector.createRenderLists());
        bh.consume(collector.getRebuildLists());
    }
}
