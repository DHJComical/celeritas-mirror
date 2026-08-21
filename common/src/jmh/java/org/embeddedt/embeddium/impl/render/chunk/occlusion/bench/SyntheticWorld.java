package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.VoxelWorld;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegionManager;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Deterministic world of real {@link RenderSection}s covering a box around the origin. Position-hashed: content
 * is derived from coordinates, so {@code scatteredAlloc} is a pure heap-layout knob. Visibility comes from
 * production's {@code SectionVisibilityBuilder} over {@link VoxelWorld} blocks.
 */
public final class SyntheticWorld {
    public static final int MIN_SECTION_Y = -4;
    public static final int MAX_SECTION_Y = 20;

    public static final int MARGIN = 2;

    public static final long DEFAULT_SEED = 0x5EED_C0FFEE_1234L;

    public static final double PENDING_UPDATE_FRACTION = 0.05D;
    public static final double BUILD_IN_FLIGHT_FRACTION = 0.5D;

    private final WorldType type;
    private final int renderDistance;
    private final long seed;
    private final boolean scatteredAlloc;
    private final double unloadedProbability;

    private final VoxelWorld voxelWorld;

    private final Long2ReferenceOpenHashMap<RenderSection> sections;
    private final RenderSection[] constructionOrder;
    private final int regionCount;
    private final int sectionCount;

    public SyntheticWorld(WorldType type, int renderDistance) {
        this(type, renderDistance, DEFAULT_SEED, VoxelWorld.DEFAULT_CAVE_WIDTH, false, 0.0D, false);
    }

    public SyntheticWorld(WorldType type, int renderDistance, boolean scatteredAlloc) {
        this(type, renderDistance, DEFAULT_SEED, VoxelWorld.DEFAULT_CAVE_WIDTH, scatteredAlloc, 0.0D, false);
    }

    /**
     * @param scatteredAlloc      shuffled allocation order (models a long-lived session)
     * @param unloadedProbability fraction of sections absent from the box
     * @param allFlagged          full visuals mask on every section (sensitivity knob, not realistic)
     */
    public SyntheticWorld(WorldType type, int renderDistance, long seed, double caveWidth, boolean scatteredAlloc,
                          double unloadedProbability, boolean allFlagged) {
        this.type = type;
        this.renderDistance = renderDistance;
        this.seed = seed;
        this.scatteredAlloc = scatteredAlloc;
        this.unloadedProbability = unloadedProbability;
        this.voxelWorld = new VoxelWorld(type, seed, caveWidth);

        final int radius = renderDistance + MARGIN;
        final int width = (radius * 2) + 1;
        final int count = width * width * (MAX_SECTION_Y - MIN_SECTION_Y);

        var regionManager = BenchPlatform.regionManager();
        var regions = new Long2ReferenceOpenHashMap<RenderRegion>();

        // Pass 1: enumerate positions and mint regions (id assignment is order-independent).
        long[] positions = new long[count];
        int i = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = MIN_SECTION_Y; y < MAX_SECTION_Y; y++) {
                    if (this.isUnloaded(x, y, z)) {
                        continue;
                    }

                    regionFor(regionManager, regions, x, y, z);
                    positions[i++] = PositionUtil.packSection(x, y, z);
                }
            }
        }

        final int present = i;

        if (present < count) {
            positions = Arrays.copyOf(positions, present);
        }

        if (scatteredAlloc) {
            shuffle(positions, seed ^ 0x9E3779B97F4A7C15L);
        }

        // Pass 1.5: visibility is a pure function of position, so compute it on every core up front. Only the
        // arithmetic moves off this thread — pass 2 still allocates every section itself, in order, because the
        // heap layout of those sections is exactly what scatteredAlloc exists to control and letting worker
        // threads allocate them into their own TLABs would quietly change what the benchmark measures.
        long[] visibility = this.computeVisibility(radius, width);

        // Pass 2: allocate sections in the requested order.
        this.sections = new Long2ReferenceOpenHashMap<>(present, 0.75f);
        this.constructionOrder = new RenderSection[present];

        for (int n = 0; n < present; n++) {
            long packed = positions[n];
            int x = unpackX(packed), y = unpackY(packed), z = unpackZ(packed);

            var section = new RenderSection(regionFor(regionManager, regions, x, y, z), x, y, z);

            section.setInfo(new BenchSectionData(visibility[gridIndex(x, y, z, radius, width)], x, y, z,
                    allFlagged));
            applyPendingUpdate(section, x, y, z);

            this.sections.put(packed, section);
            this.constructionOrder[n] = section;
        }

        this.regionCount = regions.size();
        this.sectionCount = this.sections.size();
    }

    private static final CancellationToken BUILD_IN_FLIGHT = new CancellationToken() {
        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCancelled() {
        }
    };

    /** Position-hashed pending updates and in-flight builds. */
    private static void applyPendingUpdate(RenderSection section, int x, int y, int z) {
        long h = (x * 0xD6E8FEB86659FD93L) ^ (y * 0xBF58476D1CE4E5B9L) ^ (z * 0x94D049BB133111EBL);
        h ^= 0x2545F4914F6CDD1DL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;

        double roll = (h >>> 11) * 0x1.0p-53;

        if (roll >= PENDING_UPDATE_FRACTION) {
            return;
        }

        var types = ChunkUpdateType.VALUES;
        section.setPendingUpdate(types[(int) ((h >>> 3) % types.length)]);

        if ((roll / PENDING_UPDATE_FRACTION) < BUILD_IN_FLIGHT_FRACTION) {
            section.setBuildCancellationToken(BUILD_IN_FLIGHT);
        }
    }

    /** Seeded Fisher-Yates shuffle. */
    private static void shuffle(long[] values, long seed) {
        var random = new SplittableRandom(seed);

        for (int i = values.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            long tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 42);
    }

    public static int unpackZ(long packed) {
        return (int) ((packed << 22) >> 42);
    }

    public static int unpackY(long packed) {
        return (int) ((packed << 44) >> 44);
    }

    /** Creates or returns the region covering a section position. */
    private static RenderRegion regionFor(RenderRegionManager regionManager,
                                          Long2ReferenceOpenHashMap<RenderRegion> regions, int x, int y, int z) {
        long key = RenderRegion.key(x >> RenderRegion.REGION_WIDTH_SH,
                y >> RenderRegion.REGION_HEIGHT_SH,
                z >> RenderRegion.REGION_LENGTH_SH);

        RenderRegion region = regions.get(key);

        if (region == null) {
            region = regionManager.createForChunk(x, y, z);
            regions.put(key, region);
        }

        return region;
    }

    // ------------------------------------------------------------------
    // Visibility generation
    // ------------------------------------------------------------------

    /**
     * {@return the visibility encoding of every section in the box, indexed by {@link #gridIndex}}
     *
     * Work is striped by section column so that each worker walks whole columns and gets full value from its
     * {@code VoxelWorld}'s heightmap cache. Output does not depend on the striping: visibility is a pure
     * function of position.
     */
    private long[] computeVisibility(int radius, int width) {
        final int height = MAX_SECTION_Y - MIN_SECTION_Y;
        var visibility = new long[width * width * height];

        int workers = Math.min(Runtime.getRuntime().availableProcessors(), width);

        if (workers <= 1) {
            this.computeVisibilityStripe(this.voxelWorld, visibility, radius, width, 0, 1);
            return visibility;
        }

        var threads = new Thread[workers];

        for (int w = 0; w < workers; w++) {
            final int worker = w;

            // A private VoxelWorld per worker: its heightmap cache is not thread safe, and worlds sharing a
            // seed generate identical blocks. They are dropped with the threads, releasing those caches.
            threads[w] = new Thread(() -> this.computeVisibilityStripe(
                    new VoxelWorld(this.type, this.seed, this.voxelWorld.getCaveWidth()),
                    visibility, radius, width, worker, workers), "bench-worldgen-" + worker);

            threads[w].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while generating the synthetic world", e);
            }
        }

        return visibility;
    }

    /** Runs the real visibility flood fill over every section in this worker's share of the columns. */
    private void computeVisibilityStripe(VoxelWorld world, long[] visibility, int radius, int width,
                                         int worker, int workers) {
        for (int xi = worker; xi < width; xi += workers) {
            int x = xi - radius;

            for (int zi = 0; zi < width; zi++) {
                int z = zi - radius;

                for (int y = MIN_SECTION_Y; y < MAX_SECTION_Y; y++) {
                    if (this.isUnloaded(x, y, z)) {
                        continue;
                    }

                    visibility[gridIndex(x, y, z, radius, width)] = world.slice(x, y, z).computeVisibility();
                }
            }
        }
    }

    /** Dense index into the visibility array for a position in the box. */
    private static int gridIndex(int x, int y, int z, int radius, int width) {
        return ((((x + radius) * width) + (z + radius)) * (MAX_SECTION_Y - MIN_SECTION_Y)) + (y - MIN_SECTION_Y);
    }

    /** Surface height at the given section coordinates. */
    public static int surfaceSectionY(int x, int z) {
        return (int) Math.floor(VoxelWorld.sectionHeight(x, z));
    }

    /** Position-hashed absence test. */
    private boolean isUnloaded(int x, int y, int z) {
        if (this.unloadedProbability <= 0.0) {
            return false;
        }

        long h = this.seed ^ 0xA24BAED4963EE407L;
        h = h * 0x9E3779B97F4A7C15L + x * 0xBF58476D1CE4E5B9L;
        h = h * 0x9E3779B97F4A7C15L + y * 0x94D049BB133111EBL;
        h = h * 0x9E3779B97F4A7C15L + z * 0xD6E8FEB86659FD93L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;

        return (h >>> 11) * 0x1.0p-53 < this.unloadedProbability;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public RenderSection getSection(int x, int y, int z) {
        return this.sections.get(PositionUtil.packSection(x, y, z));
    }

    public Long2ReferenceMap<RenderSection> getSections() {
        return this.sections;
    }

    /** Sections in allocation order (linear or shuffled). Attach to the lattice in this order. */
    public RenderSection[] getConstructionOrder() {
        return this.constructionOrder;
    }

    public boolean isScatteredAlloc() {
        return this.scatteredAlloc;
    }

    public int getRegionCount() {
        return this.regionCount;
    }

    public int getSectionCount() {
        return this.sectionCount;
    }

    public int getRenderDistance() {
        return this.renderDistance;
    }

    public WorldType getType() {
        return this.type;
    }

    /** {@return the voxel world these sections were generated from} */
    public VoxelWorld getVoxelWorld() {
        return this.voxelWorld;
    }

    /** Search distance in blocks. */
    public float getSearchDistance() {
        return this.renderDistance * 16.0f;
    }

    @Override
    public String toString() {
        return "SyntheticWorld{type=" + this.type + ", rd=" + this.renderDistance
                + ", sections=" + this.sectionCount + ", regions=" + this.regionCount
                + ", scatteredAlloc=" + this.scatteredAlloc + "}";
    }
}
