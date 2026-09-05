package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.VoxelSlice;
import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.VoxelWorld;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.geometry.AreaFinder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.geometry.BoxFinder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Per-section cost of generating occluder boxes, which a real integration must pay on the chunk
 * build thread.
 *
 * <p>{@link #visibilityFloodFill} is the yardstick, not a baseline to subtract: it is the per-section
 * occlusion work meshing already does today and therefore a known-affordable cost. Read the result as
 * a ratio against it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class BoxFinderBench {
    private static final int SECTION_SAMPLE = 512;

    @State(Scope.Benchmark)
    public static class Sections {
        @Param({ "SURFACE", "CAVES" })
        public WorldType world;

        public long[][] opacity;
        public VoxelSlice[] slices;

        @Setup
        public void setup() {
            var voxelWorld = new VoxelWorld(this.world);
            var sampledSlices = new ArrayList<VoxelSlice>(SECTION_SAMPLE);
            var sampledOpacity = new ArrayList<long[]>(SECTION_SAMPLE);

            // Spiral out from the origin column so the sample spans terrain, caves and sky rather
            // than whichever corner a raster scan happens to start in.
            outer:
            for (int radius = 0; radius < 64; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                            continue;
                        }

                        for (int y = SyntheticWorld.MIN_SECTION_Y; y < SyntheticWorld.MAX_SECTION_Y; y++) {
                            var slice = voxelWorld.slice(x, y, z);

                            if (slice.isEmpty()) {
                                continue;
                            }

                            long[] bits = slice.opacityBits();

                            // Sections with no opaque blocks produce no occluders and would be
                            // skipped in production.
                            if (isAllClear(bits)) {
                                continue;
                            }

                            sampledSlices.add(slice);
                            sampledOpacity.add(bits);

                            if (sampledOpacity.size() == SECTION_SAMPLE) {
                                break outer;
                            }
                        }
                    }
                }
            }

            if (sampledOpacity.size() < SECTION_SAMPLE) {
                throw new IllegalStateException("only found " + sampledOpacity.size()
                        + " non-empty sections for world " + this.world);
            }

            this.opacity = sampledOpacity.toArray(new long[0][]);
            this.slices = sampledSlices.toArray(new VoxelSlice[0]);
        }

        private static boolean isAllClear(long[] bits) {
            for (long word : bits) {
                if (word != 0) {
                    return false;
                }
            }

            return true;
        }
    }

    @State(Scope.Thread)
    public static class Finder {
        public final BoxFinder boxFinder = new BoxFinder(new AreaFinder());
    }

    @Benchmark
    @OperationsPerInvocation(SECTION_SAMPLE)
    public void findBoxes(Sections sections, Finder finder, Blackhole bh) {
        for (long[] bits : sections.opacity) {
            finder.boxFinder.findBoxes(bits, 0);
            bh.consume(finder.boxFinder.boxes.size());
        }
    }

    @Benchmark
    @OperationsPerInvocation(SECTION_SAMPLE)
    public void visibilityFloodFill(Sections sections, Blackhole bh) {
        for (VoxelSlice slice : sections.slices) {
            bh.consume(slice.computeVisibility());
        }
    }
}
