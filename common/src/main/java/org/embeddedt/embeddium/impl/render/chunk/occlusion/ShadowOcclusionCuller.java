package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.PackedSectionMetadata;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;

import java.util.Arrays;

import static org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller.ANGLE_REFINEMENT_MASKS;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller.isVisibleInPartialRegion;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller.regionOrigin;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.XYZ_LOCAL_MASK;
import static org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice.XYZ_SHIFT;

/**
 * Receiver-driven, light-monotone visibility search for a directional shadow pass.
 *
 * <p>A section can only cast a shadow the player sees if it lies on a light ray through a point the camera can
 * see. The search therefore starts from every cell the main pass reported visible and expands only toward the light,
 * through the face-pair visibility graph, until a blocker. Blockers are reported (they are shadow casters) but do not
 * get queued for further traversal. This design means geometry the camera cannot see (such as the surface sections
 * above an enclosed cave) is never reached.
 *
 * <p>Less things depend on the camera position than the standard occlusion culler. The outward-direction and
 * angle-refinement masks follow from the light vector alone and are constant for the whole search, and there is no
 * angular aperture to track.
 *
 * <p>In this culler variant, a section is not necessarily dequeued only once. A later arrival through a
 * previously unused incoming face can widen the exits available to that section, so it must be enqueued and expanded
 * again. {@code REPORTED_BIT} prevents duplicate visitor reports. (The main culler avoids this by normally enqueuing
 * only one root and expanding outwards. This guarantees every path to a cell arrives at the same queue depth, so
 * all incoming apertures are merged before that cell is dequeued. Edge cases needing multiple roots disable aperture
 * clamping to sidestep the problem.)
 *
 * <p>The search stamps {@link SectionLattice#shadowVisitState}, leaving the main search's state intact, and reads
 * the root set from {@link SectionLattice#visibleCells}, so it must run after the main search for the same frame.
 */
final class ShadowOcclusionCuller {
    // A light component smaller than this is treated as zero: both directions on that axis stay open.
    private static final float AXIS_EPSILON = 1e-4f;

    // Marks a root cell. A root is where a light ray ends, not somewhere it passes through, so it is expanded in
    // every direction toward the light no matter which neighbours reach it later.
    private static final int ROOT_BIT = 1 << GraphDirection.COUNT;

    private static final int REPORTED_BIT = 1 << (GraphDirection.COUNT + 1);

    private static final int INCOMING_MASK = GraphDirectionSet.ALL;

    private static final int DOWN_BIT = 1 << GraphDirection.DOWN;
    private static final int UP_BIT = 1 << GraphDirection.UP;
    private static final int NORTH_BIT = 1 << GraphDirection.NORTH;
    private static final int SOUTH_BIT = 1 << GraphDirection.SOUTH;
    private static final int WEST_BIT = 1 << GraphDirection.WEST;
    private static final int EAST_BIT = 1 << GraphDirection.EAST;

    private final SectionLattice lattice;
    private final RegionCullCache regionCullCache = new RegionCullCache();

    // Same entry format as OcclusionCuller's queue: (packedLocalXYZ << 32) | latticeIndex.
    private long[] queue = new long[256];

    ShadowOcclusionCuller(SectionLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Search from the main pass's visible cells toward the light and report each reached cell to {@code visitor}.
     *
     * @param viewport       the shadow viewport; its frustum and camera bound the search like in the main pass
     * @param lightX         unit vector toward the shadow light
     */
    void findVisible(OcclusionCuller.Visitor visitor,
                     Viewport viewport,
                     float searchDistance,
                     int numRegions,
                     float lightX, float lightY, float lightZ,
                     int frame)
    {
        // Room for one entry per installed cell, which is all a cell's first visit can need. Only re-expansion can
        // want more, and the loop below grows the queue if it does.
        int installed = this.lattice.installedCount;
        if (this.queue.length < installed) {
            this.queue = new long[Math.max(installed, this.queue.length * 2)];
        }

        this.regionCullCache.begin(viewport, searchDistance, numRegions);

        final int towardLight = towardLightDirections(lightX, lightY, lightZ);
        final long refinementMask = refinementMask(lightX, lightY, lightZ);
        final long frameStamp = SectionLattice.frameStamp(frame);

        int tail = this.seed(frameStamp);
        this.process(visitor, viewport, searchDistance, towardLight, refinementMask, frameStamp, tail);
    }

    /**
     * Directions a light ray may step in: the sign of each light component. A component near zero keeps both
     * directions open, so a near-vertical sun does not flicker between sides from float jitter.
     */
    static int towardLightDirections(float lightX, float lightY, float lightZ) {
        int directions = 0;

        if (lightX <= AXIS_EPSILON) directions |= WEST_BIT;
        if (lightX >= -AXIS_EPSILON) directions |= EAST_BIT;

        if (lightY <= AXIS_EPSILON) directions |= DOWN_BIT;
        if (lightY >= -AXIS_EPSILON) directions |= UP_BIT;

        if (lightZ <= AXIS_EPSILON) directions |= NORTH_BIT;
        if (lightZ >= -AXIS_EPSILON) directions |= SOUTH_BIT;

        return directions;
    }

    /**
     * Visibility mask dropping the straight-through face pairs of every axis that is strictly shorter than the
     * light's dominant component: a ray crossing a full section along such an axis would travel more than a section
     * along the dominant one. Ties keep their pairs.
     */
    static long refinementMask(float lightX, float lightY, float lightZ) {
        float ax = Math.abs(lightX), ay = Math.abs(lightY), az = Math.abs(lightZ);
        float max = Math.max(ax, Math.max(ay, az));

        int sel = 0;
        if (ax < max - AXIS_EPSILON) sel |= 1;
        if (ay < max - AXIS_EPSILON) sel |= 2;
        if (az < max - AXIS_EPSILON) sel |= 4;

        return ANGLE_REFINEMENT_MASKS[sel];
    }

    /**
     * Enqueue every main-pass visible cell whose region is not wholly outside the shadow frustum. Cells are installed
     * by construction (the lattice is structurally unchanged between the two searches), so only the stamp gate is
     * needed to avoid duplicates.
     */
    private int seed(long frameStamp) {
        final long[] roots = this.lattice.visibleCells;
        final int rootCount = this.lattice.visibleCount;
        final long[] visitState = this.lattice.shadowVisitState;
        final int[] regionOfCell = this.lattice.regionOfCell;
        final long[] queue = this.queue;
        final RegionCullCache cache = this.regionCullCache;
        final int baseX = this.lattice.baseX, baseY = this.lattice.baseY, baseZ = this.lattice.baseZ;

        int tail = 0;

        for (int i = 0; i < rootCount; i++) {
            long entry = roots[i];
            int idx = (int) entry;

            // Defensive check if roots somehow contain duplicates
            if (visitState[idx] >= frameStamp) {
                continue;
            }

            // The chunk coords are only needed to work out the region's bounds, and each region is measured once
            // per search. With thousands of roots per region that is nearly always a cache hit, so unpack the
            // coordinates inside the miss rather than for every root.
            int regionId = regionOfCell[idx];
            int classification = cache.cached(regionId);

            if (classification == RegionCullCache.UNCOMPUTED) {
                int xyz = (int) (entry >>> 32);
                int chunkX = baseX + ((xyz >>> (2 * XYZ_SHIFT)) & XYZ_LOCAL_MASK);
                int chunkY = baseY + ((xyz >>> XYZ_SHIFT) & XYZ_LOCAL_MASK);
                int chunkZ = baseZ + (xyz & XYZ_LOCAL_MASK);

                classification = cache.classify(regionId,
                        regionOrigin(chunkX, RenderRegion.REGION_WIDTH_SH, RenderRegion.REGION_BLOCK_WIDTH),
                        regionOrigin(chunkY, RenderRegion.REGION_HEIGHT_SH, RenderRegion.REGION_BLOCK_HEIGHT),
                        regionOrigin(chunkZ, RenderRegion.REGION_LENGTH_SH, RenderRegion.REGION_BLOCK_LENGTH));
            }

            if (classification == RegionCullCache.OUTSIDE) {
                // Shadow frustum is guaranteed to exclude section, skip
                continue;
            }

            visitState[idx] = frameStamp | ROOT_BIT;
            queue[tail++] = entry;
        }

        return tail;
    }

    private void process(OcclusionCuller.Visitor visitor,
                         Viewport viewport,
                         float searchDistance,
                         int towardLight,
                         long refinementMask,
                         long frameStamp,
                         int tail)
    {
        final long[] visitState = this.lattice.shadowVisitState;
        final long[] sectionMeta = this.lattice.sectionMeta;
        final int[] regionOfCell = this.lattice.regionOfCell;
        final int[] delta = this.lattice.delta;
        long[] queue = this.queue;
        final int baseX = this.lattice.baseX, baseY = this.lattice.baseY, baseZ = this.lattice.baseZ;

        final RegionCullCache cache = this.regionCullCache;
        final CameraTransform transform = viewport.getTransform();

        final int strideX = delta[GraphDirection.EAST];
        final int strideZ = delta[GraphDirection.SOUTH];
        final int xStep = 1 << (2 * XYZ_SHIFT);
        final int yStep = 1 << XYZ_SHIFT;

        int head = 0;

        while (head < tail) {
            // Expanding this cell adds at most one entry per direction. We hoist the bounds check for that here to avoid
            // needing to do it multiple times later.
            if (tail + GraphDirection.COUNT > queue.length) {
                queue = this.grow(tail + GraphDirection.COUNT);
            }

            long entry = queue[head++];
            int idx = (int) entry;
            int xyz = (int) (entry >>> 32);

            int chunkX = baseX + ((xyz >>> (2 * XYZ_SHIFT)) & XYZ_LOCAL_MASK);
            int chunkY = baseY + ((xyz >>> XYZ_SHIFT) & XYZ_LOCAL_MASK);
            int chunkZ = baseZ + (xyz & XYZ_LOCAL_MASK);

            // Safe to read now and write back further down: expanding a cell only ever writes its neighbours,
            // so nothing between here and there touches this cell's state.
            long state = visitState[idx];
            boolean first = (state & REPORTED_BIT) == 0;

            int regionId = regionOfCell[idx];
            long sm = sectionMeta[idx];
            int classification = cache.classify(regionId,
                    regionOrigin(chunkX, RenderRegion.REGION_WIDTH_SH, RenderRegion.REGION_BLOCK_WIDTH),
                    regionOrigin(chunkY, RenderRegion.REGION_HEIGHT_SH, RenderRegion.REGION_BLOCK_HEIGHT),
                    regionOrigin(chunkZ, RenderRegion.REGION_LENGTH_SH, RenderRegion.REGION_BLOCK_LENGTH));

            boolean visible;

            if (classification == RegionCullCache.FULLY_INSIDE) {
                visible = true;
            } else if (classification == RegionCullCache.OUTSIDE) {
                visible = false;
            } else {
                visible = isVisibleInPartialRegion(classification, viewport, transform, chunkX, chunkY, chunkZ, searchDistance);
            }

            // Report the cell the first time it comes off the queue. A later pass over it has nothing new to
            // report, only new faces to expand. Cells that failed the visibility tests are marked too.
            if (first) {
                visitState[idx] = state | REPORTED_BIT;

                int sectionIndex = LocalSectionIndex.pack(chunkX, chunkY, chunkZ);
                visitor.visit(idx, regionId, sectionIndex, PackedSectionMetadata.toCompactMeta(sm), visible);
            }

            if (!visible) {
                continue;
            }

            int connections;

            if ((state & ROOT_BIT) != 0) {
                // A root is a camera-visible cell where a shadow may be received. Do not require the light path to
                // pass through the root's section: even a solid root can contain a visible receiving surface. Start
                // with every face open; the towardLight mask below restricts traversal to directions toward the light.
                connections = GraphDirectionSet.ALL;
            } else {
                // For a non-root, the search has entered through one or more faces. Follow only the face-to-face
                // paths that the section's occlusion data allows; blocked paths stop at this section instead of
                // reaching sections farther along the light ray.
                connections = VisibilityEncoding.getConnections(sm & refinementMask, (int) (state & INCOMING_MASK));
            }

            // Only ever step toward the light.
            connections &= towardLight;

            if ((connections & DOWN_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx - 1, xyz - yStep, UP_BIT, refinementMask, towardLight, frameStamp, tail);
            }
            if ((connections & UP_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx + 1, xyz + yStep, DOWN_BIT, refinementMask, towardLight, frameStamp, tail);
            }
            if ((connections & NORTH_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx - strideZ, xyz - 1, SOUTH_BIT, refinementMask, towardLight, frameStamp, tail);
            }
            if ((connections & SOUTH_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx + strideZ, xyz + 1, NORTH_BIT, refinementMask, towardLight, frameStamp, tail);
            }
            if ((connections & WEST_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx - strideX, xyz - xStep, EAST_BIT, refinementMask, towardLight, frameStamp, tail);
            }
            if ((connections & EAST_BIT) != 0) {
                tail = visit(visitState, sectionMeta, queue, idx + strideX, xyz + xStep, WEST_BIT, refinementMask, towardLight, frameStamp, tail);
            }
        }
    }

    // Keeps the entries still waiting in the queue. The caller reads the queue through a local, so it has to take
    // the new array back.
    private long[] grow(int minimumCapacity) {
        this.queue = Arrays.copyOf(this.queue, Math.max(minimumCapacity, this.queue.length * 2));
        return this.queue;
    }

    /**
     * Queue a neighbour the first time it is reached this frame, and again if it is later reached through a face it
     * was not expanded with. Reaching it through a face it already has just records the direction.
     *
     * <p>The second test is written as one mask because the bits it looks at cannot overlap. It asks for three
     * things: the face is new, the cell is not a root, and the cell has already been expanded. A root is skipped
     * because it already leaves in every direction it can, so another face adds nothing. A cell that is still
     * waiting its turn is skipped because it will see the face when that turn comes.
     */
    private static int visit(long[] visitState, long[] sectionMeta, long[] queue, int idx, int xyz, int incoming,
                             long refinementMask, int towardLight, long frameStamp, int tail) {
        long state = visitState[idx];

        if (state < frameStamp) {
            state = frameStamp;
            queue[tail++] = ((long) xyz << 32) | (idx & 0xFFFFFFFFL);
        } else if ((state & (incoming | ROOT_BIT | REPORTED_BIT)) == REPORTED_BIT
                && widensExits(sectionMeta[idx], refinementMask, towardLight, (int) state, incoming)) {
            queue[tail++] = ((long) xyz << 32) | (idx & 0xFFFFFFFFL);
        }

        visitState[idx] = state | incoming;

        return tail;
    }

    /**
     * Whether coming in through {@code incoming} as well lets the cell reach somewhere it could not before.
     *
     * <p>Usually it does not. If a section can be seen through between every pair of its faces, then it does not
     * matter which face the ray came in by, because it can leave by all the same ones either way. Only a section
     * that blocks some of those pairs behaves differently, and there the way out really does depend on the way in.
     * Ordinary terrain blocks some pairs; a section holding two separate air pockets is the clearest example.
     *
     * <p>{@code state} is the exact set of faces the cell was last expanded with, because expanding a cell always
     * uses every face it has at that moment. So a face that turns up while the cell is waiting for a second turn
     * gets used anyway, whatever this returns.
     */
    private static boolean widensExits(long sm, long refinementMask, int towardLight, int state, int incoming) {
        long visibility = sm & refinementMask;

        // Can be seen through between every pair of faces the light allows, so it already leaves in every
        // direction it can and no further face changes that.
        if (visibility == refinementMask) {
            return false;
        }

        int before = VisibilityEncoding.getConnections(visibility, state & INCOMING_MASK) & towardLight;
        int after = VisibilityEncoding.getConnections(visibility, (state | incoming) & INCOMING_MASK) & towardLight;

        return after != before;
    }
}
