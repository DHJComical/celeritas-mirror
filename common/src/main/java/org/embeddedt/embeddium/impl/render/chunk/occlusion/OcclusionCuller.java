package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.PackedSectionMetadata;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
import org.joml.Vector3ic;

import static org.embeddedt.embeddium.impl.common.util.MathUtil.nearestToZero;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.DIR_MASK;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.XYZ_STEP;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.XYZ_SHIFT;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.XYZ_LOCAL_MASK;

/**
 * Performs a visibility search over the installed cells of a
 * {@link SectionLattice}.
 *
 * <p>A search has three phases:
 * <ol>
 *   <li>prepare the reusable queue and the region cull cache;</li>
 *   <li>seed the search from the camera section, or scan the appropriate
 *       world-height plane when the camera is outside the world or unloaded;</li>
 *   <li>process the queue in traversal order, applying region/frustum and
 *       render-distance tests before expanding visible cells through the
 *       occlusion graph, reporting each processed cell directly to the {@link Visitor}.</li>
 * </ol>
 *
 * <p>Occlusion traversal accumulates the directions through which each cell
 * was reached, then asks {@link VisibilityEncoding} which outgoing directions
 * are possible. Expansion is additionally restricted to directions pointing
 * away from the camera, preventing paths from backtracking through the search
 * origin. If the camera is in an unloaded section, the search remains useful
 * as a frustum/render-distance scan but disables graph occlusion for that
 * search.
 *
 * <p>The queue is reused between searches. A single
 * invocation must run at a time, and the lattice's dimensions, coordinates,
 * installed membership, and metadata must remain stable until the invocation
 * completes. The owning {@code RenderListManager} provides that coordination.
 */
public class OcclusionCuller {
    private final SectionLattice lattice;
    private final int minSectionY, maxSectionY;

    // When stepping from a cell in dir, the neighbour is entered from the opposite face.
    private static final int[] INCOMING = new int[GraphDirection.COUNT];

    static {
        for (int dir = 0; dir < GraphDirection.COUNT; dir++) {
            INCOMING[dir] = GraphDirectionSet.of(GraphDirection.opposite(dir));
        }
    }

    /*
     * Single flat BFS queue. Each entry is
     * (packedLocalXYZ << 32) | latticeIndex, where packedLocalXYZ contains
     * the three 10-bit local coordinates used by SectionLattice. A live cell
     * is enqueued at most once per search, so tail never exceeds
     * lattice.installedCount and the queue needs no capacity check in the hot
     * loop after it has been sized for the search.
     */
    private long[] queue = new long[256];
    private int tail;
    private long[] apertures = new long[0];




    private final RegionCullCache regionCullCache = new RegionCullCache();

    private boolean isCameraInUnloadedSection;
    private boolean useFastFrustumClamping;

    public OcclusionCuller(SectionLattice lattice, int minSectionY, int maxSectionY) {
        this.lattice = lattice;
        this.minSectionY = minSectionY;
        this.maxSectionY = maxSectionY;
    }

    /**
     * Search the current lattice and report each reached section to
     * {@code visitor} in traversal order.
     *
     * <p>Reached sections are reported even when {@code visible} is false;
     * the flag controls whether the section contributes renderable work and
     * whether traversal expands through it. The caller must have prepared the
     * lattice with {@link SectionLattice#ensureWindowCovers} and must not
     * mutate its structure or metadata until this method returns.
     *
     * @param useOcclusionCulling whether section visibility metadata should
     *                             restrict graph expansion
     */
    public void findVisible(Visitor visitor,
                            Viewport viewport,
                            float searchDistance,
                            int numRegions,
                            boolean useOcclusionCulling,
                            int frame)
    {
        // Pre-size so enqueue is a bare store: at most one entry per installed cell.
        int installed = this.lattice.installedCount;
        if (this.queue.length < installed) {
            this.queue = new long[Math.max(installed, this.queue.length * 2)];
        }
        this.tail = 0;

        this.regionCullCache.begin(viewport, searchDistance, numRegions);

        this.isCameraInUnloadedSection = false;
        this.useFastFrustumClamping = false;
        this.init(visitor, viewport, searchDistance, useOcclusionCulling, frame);
        if (this.isCameraInUnloadedSection) {
            useOcclusionCulling = false;
        }

        this.process(visitor, viewport, searchDistance, useOcclusionCulling, frame, this.useFastFrustumClamping);
    }

    /**
     * Process the BFS queue. Each queued cell is classified once, reported
     * directly to the visitor, and expanded only when it is visible. Occlusion
     * metadata selects the possible exits; the outward-direction mask then
     * prevents moving back toward the camera. The visitor is invoked in
     * traversal order, with every argument already in registers; it must not
     * feed changes back into the running search.
     */
    private void process(Visitor visitor,
                         Viewport viewport,
                         float searchDistance,
                         boolean useOcclusionCulling,
                         int frame,
                         boolean useFastFrustumClamping)
    {
        final long[] visitState = this.lattice.visitState;
        final long[] sectionMeta = this.lattice.sectionMeta;
        final int[] regionOfCell = this.lattice.regionOfCell;
        final int[] delta = this.lattice.delta;
        final long[] queue = this.queue;
        final long[] apertures = this.apertures;
        final int baseX = this.lattice.baseX, baseY = this.lattice.baseY, baseZ = this.lattice.baseZ;

        final RegionCullCache cache = this.regionCullCache;
        final CameraTransform transform = viewport.getTransform();
        final Vector3ic camera = viewport.getChunkCoord();
        final int camX = camera.x(), camY = camera.y(), camZ = camera.z();
        final long frameStamp = SectionLattice.frameStamp(frame);

        int head = 0;
        int tail = this.tail;

        // Constant neighbour strides / packed-coord steps, hoisted so the unrolled expansion below uses
        // immediates instead of per-edge array loads. delta[EAST]=+strideX, delta[SOUTH]=+strideZ, Y-stride=1.
        final int strideX = delta[GraphDirection.EAST];
        final int strideZ = delta[GraphDirection.SOUTH];
        final int xStep = 1 << (2 * XYZ_SHIFT);
        final int yStep = 1 << XYZ_SHIFT;

        while (head < tail) {
            long entry = queue[head++];
            int idx = (int) entry;
            int xyz = (int) (entry >>> 32);

            int chunkX = baseX + ((xyz >>> (2 * XYZ_SHIFT)) & XYZ_LOCAL_MASK);
            int chunkY = baseY + ((xyz >>> XYZ_SHIFT) & XYZ_LOCAL_MASK);
            int chunkZ = baseZ + (xyz & XYZ_LOCAL_MASK);

            int regionId = regionOfCell[idx];
            long sm = sectionMeta[idx];
            int compactMeta = PackedSectionMetadata.toCompactMeta(sm);
            int classification = cache.classify(regionId,
                    regionOrigin(chunkX, RenderRegion.REGION_WIDTH_SH, RenderRegion.REGION_BLOCK_WIDTH),
                    regionOrigin(chunkY, RenderRegion.REGION_HEIGHT_SH, RenderRegion.REGION_BLOCK_HEIGHT),
                    regionOrigin(chunkZ, RenderRegion.REGION_LENGTH_SH, RenderRegion.REGION_BLOCK_LENGTH));

            // Fully-inside regions need no per-section frustum test. Sections
            // in a partial region are checked individually for both distance
            // and frustum visibility; outside regions are not traversed.
            boolean visible;

            if (classification == Frustum.PARTIALLY_INSIDE) {
                visible = isWithinRenderDistance(transform, chunkX, chunkY, chunkZ, searchDistance)
                        && isWithinFrustum(viewport, chunkX, chunkY, chunkZ);
            } else {
                visible = classification == Frustum.FULLY_INSIDE;
            }

            int sectionIndex = LocalSectionIndex.pack(chunkX, chunkY, chunkZ);
            visitor.visit(idx, regionId, sectionIndex, compactMeta, visible);

            if (!visible) {
                continue;
            }

            int connections;

            if (useOcclusionCulling) {
                // Merge the outgoing paths reachable from every incoming path
                // accumulated in this cell.
                int incoming = (int) (visitState[idx] & DIR_MASK);
                connections = VisibilityEncoding.getConnections(
                        sm & PackedSectionMetadata.VISIBILITY_MASK, incoming);
            } else {
                connections = GraphDirectionSet.ALL;
            }

            // We can only traverse outwards from the centre of the search.
            connections &= getOutwardDirections(chunkX, chunkY, chunkZ, camX, camY, camZ);

            if (useFastFrustumClamping) {
                long parentAperture = apertures[idx];
                int relativeX = Math.abs(chunkX - camX);
                int relativeY = Math.abs(chunkY - camY);
                int relativeZ = Math.abs(chunkZ - camZ);
                if ((connections & (1 << GraphDirection.DOWN)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx - 1, xyz - yStep, 1 << GraphDirection.UP, frameStamp, relativeX, relativeY + 1, relativeZ, tail);
                if ((connections & (1 << GraphDirection.UP)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx + 1, xyz + yStep, 1 << GraphDirection.DOWN, frameStamp, relativeX, relativeY + 1, relativeZ, tail);
                if ((connections & (1 << GraphDirection.NORTH)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx - strideZ, xyz - 1, 1 << GraphDirection.SOUTH, frameStamp, relativeX, relativeY, relativeZ + 1, tail);
                if ((connections & (1 << GraphDirection.SOUTH)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx + strideZ, xyz + 1, 1 << GraphDirection.NORTH, frameStamp, relativeX, relativeY, relativeZ + 1, tail);
                if ((connections & (1 << GraphDirection.WEST)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx - strideX, xyz - xStep, 1 << GraphDirection.EAST, frameStamp, relativeX + 1, relativeY, relativeZ, tail);
                if ((connections & (1 << GraphDirection.EAST)) != 0) tail = this.visitFfcNode(visitState, apertures, queue, parentAperture, idx + strideX, xyz + xStep, 1 << GraphDirection.WEST, frameStamp, relativeX + 1, relativeY, relativeZ, tail);
            } else {
                // Unrolled variant of visitNeighbors, with delta, step, incoming values inlined into each branch.
                // The unrolling measurably improves performance here.
                if ((connections & (1 << GraphDirection.DOWN)) != 0) {
                    final int n = idx - 1;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz - yStep) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.UP);
                }
                if ((connections & (1 << GraphDirection.UP)) != 0) {
                    final int n = idx + 1;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz + yStep) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.DOWN);
                }
                if ((connections & (1 << GraphDirection.NORTH)) != 0) {
                    final int n = idx - strideZ;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz - 1) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.SOUTH);
                }
                if ((connections & (1 << GraphDirection.SOUTH)) != 0) {
                    final int n = idx + strideZ;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz + 1) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.NORTH);
                }
                if ((connections & (1 << GraphDirection.WEST)) != 0) {
                    final int n = idx - strideX;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz - xStep) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.EAST);
                }
                if ((connections & (1 << GraphDirection.EAST)) != 0) {
                    final int n = idx + strideX;
                    long ns = visitState[n];
                    if (ns < frameStamp) { ns = frameStamp; queue[tail++] = ((long) (xyz + xStep) << 32) | (n & 0xFFFFFFFFL); }
                    visitState[n] = ns | (1 << GraphDirection.WEST);
                }
            }
        }

        this.tail = tail;
    }

    // Visit each selected neighbour using both its linear array offset and
    // its packed-coordinate offset. The lattice border makes idx + delta safe.
    private void visitNeighbors(long[] visitState, long[] queue, int[] delta, int idx, int xyz, int outgoing, long frameStamp) {
        while (outgoing != 0) {
            int dir = Integer.numberOfTrailingZeros(outgoing);
            outgoing &= outgoing - 1;

            this.visitNode(visitState, queue, idx + delta[dir], xyz + XYZ_STEP[dir], INCOMING[dir], frameStamp);
        }
    }

    /*
     * Ordered-compare visit gate. A slot is enqueued only on its first visit
     * this frame, while incoming directions are OR-ed even on later visits so
     * the cell sees the union of all reachable entry paths. SENTINEL cells,
     * including the border ring, never compare below frameStamp and therefore
     * need no separate occupancy check.
     */
    private void visitNode(long[] visitState, long[] queue, int idx, int xyz, int incoming, long frameStamp) {
        long state = visitState[idx];

        if (state < frameStamp) {
            visitState[idx] = frameStamp | incoming;
            queue[this.tail++] = ((long) xyz << 32) | (idx & 0xFFFFFFFFL);
        } else {
            visitState[idx] = state | incoming;
        }
    }

    private int visitFfcNode(long[] visitState, long[] apertures, long[] queue, long parentAperture, int idx, int xyz, int incoming, long frameStamp,
                             int relativeX, int relativeY, int relativeZ, int tail) {
        long aperture = FastFrustumClamping.clip(parentAperture, relativeX, relativeY, relativeZ);
        if (aperture == FastFrustumClamping.EMPTY) {
            return tail;
        }

        long state = visitState[idx];
        if (state < frameStamp) {
            apertures[idx] = aperture;
            visitState[idx] = frameStamp | incoming;
            queue[tail++] = ((long) xyz << 32) | (idx & 0xFFFFFFFFL);
        } else if (state != SectionLattice.SENTINEL) {
            apertures[idx] = FastFrustumClamping.hull(apertures[idx], aperture);
            visitState[idx] = state | incoming;
        }
        return tail;
    }

    // Allow movement only away from the camera on each axis. A coordinate
    // equal to the camera coordinate permits both directions on that axis.
    private static int getOutwardDirections(int chunkX, int chunkY, int chunkZ, int camX, int camY, int camZ) {
        int planes = 0;

        planes |= chunkX <= camX ? 1 << GraphDirection.WEST  : 0;
        planes |= chunkX >= camX ? 1 << GraphDirection.EAST  : 0;

        planes |= chunkY <= camY ? 1 << GraphDirection.DOWN  : 0;
        planes |= chunkY >= camY ? 1 << GraphDirection.UP    : 0;

        planes |= chunkZ <= camZ ? 1 << GraphDirection.NORTH : 0;
        planes |= chunkZ >= camZ ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    // Convert a section coordinate to the origin of its containing render
    // region, in block coordinates.
    private static int regionOrigin(int chunkCoord, int shift, int blockSize) {
        return (chunkCoord >> shift) * blockSize;
    }

    /**
     * Test the search's render-distance shape against a section bounding box.
     * Horizontal distance is circular in X/Z, while vertical distance is
     * tested independently so tall searches do not use a spherical cutoff.
     */
    private static boolean isWithinRenderDistance(CameraTransform camera, int chunkX, int chunkY, int chunkZ, float maxDistance) {
        // Origin point of the chunk's bounding box in view space.
        int ox = (chunkX << 4) - camera.intX;
        int oy = (chunkY << 4) - camera.intY;
        int oz = (chunkZ << 4) - camera.intZ;

        // Closest point within the bounding box to the camera at (0, 0, 0).
        // Testing the closest point avoids rejecting a section whose near face
        // is within range even when its origin is farther away.
        float dx = nearestToZero(ox, ox + 16) - camera.fracX;
        float dy = nearestToZero(oy, oy + 16) - camera.fracY;
        float dz = nearestToZero(oz, oz + 16) - camera.fracZ;

        return ((((dx * dx) + (dz * dz)) < (maxDistance * maxDistance)) && (Math.abs(dy) < maxDistance));
    }

    // isBoxVisible takes a section centre and half-size. The half-size is
    // 8 blocks for the section, plus model overhang and a small precision
    // epsilon (see GH#2132).
    private static final float CHUNK_SECTION_SIZE = 8.0f /* section half-size */
            + 1.0f /* maximum model extent */
            + 0.125f /* epsilon */;

    /**
     * Test a section's conservatively expanded bounding box against the
     * viewport frustum.
     */
    public static boolean isWithinFrustum(Viewport viewport, int chunkX, int chunkY, int chunkZ) {
        return viewport.isBoxVisible((chunkX << 4) + 8, (chunkY << 4) + 8, (chunkZ << 4) + 8, CHUNK_SECTION_SIZE);
    }

    /**
     * Choose the search seed. A loaded in-world camera section is processed
     * inline; an out-of-height or unloaded camera is handled by scanning a
     * horizontal plane of nearby loaded sections.
     */
    private void init(Visitor visitor,
                      Viewport viewport,
                      float searchDistance,
                      boolean useOcclusionCulling,
                      int frame)
    {
        var origin = viewport.getChunkCoord();

        if (origin.y() < this.minSectionY) {
            // Below the world: seed the lowest section with an incoming path
            // from below so traversal can proceed upward from the boundary.
            this.initOutsideWorldHeight(viewport, searchDistance, frame,
                    this.minSectionY, GraphDirectionSet.of(GraphDirection.DOWN));
        } else if (origin.y() >= this.maxSectionY) {
            // Above the world: seed the highest section with an incoming path
            // from above so traversal can proceed downward from the boundary.
            this.initOutsideWorldHeight(viewport, searchDistance, frame,
                    this.maxSectionY - 1, GraphDirectionSet.of(GraphDirection.UP));
        } else if (this.lattice.getRenderSection(origin.x(), origin.y(), origin.z()) == null) {
            // Inside the world height-wise, but in an unloaded section. Seed
            // both vertical entry paths and disable occlusion below because
            // there is no camera section from which to obtain visibility data.
            this.initOutsideWorldHeight(viewport, searchDistance, frame,
                    origin.y(), GraphDirectionSet.of(GraphDirection.UP) | GraphDirectionSet.of(GraphDirection.DOWN));
            this.isCameraInUnloadedSection = true;
        } else {
            this.initWithinWorld(visitor, viewport, useOcclusionCulling, frame);
        }
    }

    // The loaded camera section is the root: it is visited immediately, not
    // queued, and its visible paths seed the BFS with no incoming direction.
    private void initWithinWorld(Visitor visitor, Viewport viewport, boolean useOcclusionCulling, int frame) {
        final long[] visitState = this.lattice.visitState;
        final long[] queue = this.queue;
        final int[] delta = this.lattice.delta;

        var origin = viewport.getChunkCoord();
        int idx = this.lattice.indexOf(origin.x(), origin.y(), origin.z());

        // The camera section is loaded and, after ensureWindowCovers, installed
        // in the lattice interior.
        long frameStamp = SectionLattice.frameStamp(frame);
        visitState[idx] = frameStamp;

        this.useFastFrustumClamping = useOcclusionCulling;
        if (this.useFastFrustumClamping) {
            if (this.apertures.length < visitState.length) {
                this.apertures = new long[visitState.length];
            }
            this.apertures[idx] = FastFrustumClamping.FULL;
        }

        // Visit the origin immediately; it is processed inline rather than
        // enqueued so the BFS starts with its neighbours.
        int sectionIndex = LocalSectionIndex.pack(origin.x(), origin.y(), origin.z());
        long sm = this.lattice.sectionMeta[idx];
        visitor.visit(idx, this.lattice.regionOfCell[idx], sectionIndex, PackedSectionMetadata.toCompactMeta(sm), true);

        int outgoing;

        if (useOcclusionCulling) {
            // The camera is inside this chunk, so there are no incoming directions; enqueue any path out.
            outgoing = VisibilityEncoding.getConnections(sm & PackedSectionMetadata.VISIBILITY_MASK);
        } else {
            outgoing = GraphDirectionSet.ALL;
        }

        int xyz = this.lattice.packXyz(origin.x(), origin.y(), origin.z());
        if (this.useFastFrustumClamping) {
            while (outgoing != 0) {
                int dir = Integer.numberOfTrailingZeros(outgoing);
                outgoing &= outgoing - 1;
                this.tail = this.visitFfcNode(visitState, this.apertures, queue, FastFrustumClamping.FULL,
                        idx + delta[dir], xyz + XYZ_STEP[dir], INCOMING[dir], frameStamp,
                        Math.abs(GraphDirection.x(dir)), Math.abs(GraphDirection.y(dir)), Math.abs(GraphDirection.z(dir)), this.tail);
            }
        } else {
            this.visitNeighbors(visitState, queue, delta, idx, xyz, outgoing, frameStamp);
        }
    }

    /**
     * Seed a boundary plane with nearby loaded sections in deterministic
     * diamond-spiral order. The complete inner layers cover Manhattan distance
     * through {@code radius}; the remaining layers fill the surrounding square
     * out to the same X/Z radius without sorting. Each accepted section is
     * given the supplied incoming direction set before normal BFS processing.
     */
    private void initOutsideWorldHeight(Viewport viewport,
                                        float searchDistance,
                                        int frame,
                                        int height,
                                        int direction)
    {
        final long[] visitState = this.lattice.visitState;
        final long[] queue = this.queue;

        var origin = viewport.getChunkCoord();
        var radius = MathUtil.mojfloor(searchDistance / 16.0f);
        long frameStamp = SectionLattice.frameStamp(frame);

        // Layer 0: the section directly below/above the camera, if loaded and visible.
        this.tryVisitNode(visitState, queue, origin.x(), height, origin.z(), direction, frameStamp, viewport);

        // Complete inner layers, excluding layer 0.
        for (int layer = 1; layer <= radius; layer++) {
            for (int z = -layer; z < layer; z++) {
                int x = Math.abs(z) - layer;
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }

            for (int z = layer; z > -layer; z--) {
                int x = layer - Math.abs(z);
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }
        }

        // Complete the surrounding square with the outer portions of the
        // remaining diamond layers.
        for (int layer = radius + 1; layer <= 2 * radius; layer++) {
            int l = layer - radius;

            for (int z = -radius; z <= -l; z++) {
                int x = -z - layer;
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }

            for (int z = l; z <= radius; z++) {
                int x = z - layer;
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }

            for (int z = radius; z >= l; z--) {
                int x = layer - z;
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }

            for (int z = -l; z >= -radius; z--) {
                int x = layer + z;
                this.tryVisitNode(visitState, queue, origin.x() + x, height, origin.z() + z, direction, frameStamp, viewport);
            }
        }
    }

    // Seed-only visit: reject unloaded/out-of-window cells before doing a
    // frustum test, then pass the accepted cell through the normal visit gate.
    private void tryVisitNode(long[] visitState, long[] queue, int x, int y, int z, int direction, long frameStamp, Viewport viewport) {
        int idx = this.lattice.indexOf(x, y, z);

        // Out of the window or empty: visitNode would reject SENTINEL anyway,
        // but avoid the frustum test for cells that cannot be searched.
        if (idx < 0 || visitState[idx] == SectionLattice.SENTINEL) {
            return;
        }

        if (!isWithinFrustum(viewport, x, y, z)) {
            return;
        }

        this.visitNode(visitState, queue, idx, this.lattice.packXyz(x, y, z), direction, frameStamp);
    }

    /**
     * Receives one record for each section reached by a search, in traversal
     * order. A record can be non-visible: such a section is reported for
     * ordering/region bookkeeping but does not expand the search.
     */
    public interface Visitor {
        /**
         * @param latticeIndex installed {@link SectionLattice} slot for the section
         * @param regionId owning render-region identifier
         * @param sectionIndex section's compact local index within its region
         * @param meta compact collector metadata
         * @param visible whether the section passed the visibility tests
         */
        void visit(int latticeIndex, int regionId, int sectionIndex, int meta, boolean visible);
    }
}
