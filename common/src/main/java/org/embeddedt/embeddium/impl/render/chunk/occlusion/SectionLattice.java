package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.common.util.MathUtil;
import org.embeddedt.embeddium.impl.render.chunk.PackedSectionMetadata;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.util.PositionUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;

import java.util.Arrays;

/**
 * Camera-centered spatial index backing the occlusion visibility traversal.
 *
 * <p>The lattice is a dense, flat window around the camera. It gives the
 * traversal constant-time access to a section and to its six neighbours,
 * while avoiding an object allocation for every cell on the hot path.
 *
 * <p>This class has two layers of state:
 * <ul>
 *   <li>{@code sections} is the authoritative set of every attached section.
 *       It is used for world-coordinate lookup and for rebuilding the window.</li>
 *   <li>The lattice is a dense X/Z window containing the attached sections
 *       that can participate in the current search. A section outside that
 *       window remains attached, but has no lattice slot until a rebase brings
 *       it inside the window.</li>
 * </ul>
 * The lattice is therefore an index of {@code sections}, not a second
 * collection or an overflow cache; {@code latticeSection} only contains the
 * subset that is currently installed.
 *
 * <p>The arrays are parallel: for a given lattice index,
 * {@code latticeSection}, {@code sectionMeta}, and {@code regionOfCell}
 * describe the same cell. X and Z are camera-windowed, while Y covers the
 * entire world height. The window is allocated lazily by
 * {@link #ensureWindowCovers}. The first call allocates the arrays and then
 * establishes their base coordinates; later calls rebase when the camera
 * approaches an edge or the window grows. Each rebase re-installs every
 * attached section that falls in the new window.
 *
 * <p>The installable interior ({@code [1, dim-2]} on each axis) is wrapped by
 * a permanent one-cell ring of sentinel cells (index {@code 0} and
 * {@code dim-1}). Every installable cell therefore has a valid array index one
 * step away in all six directions, so traversal can add a value from
 * {@link #delta} without a bounds check: the neighbour is always in-array,
 * whether it holds another cell or a sentinel. The border ring is never
 * installable, even when the window holds no sections.
 *
 * <p>The normal lifecycle is construction with no lattice allocation, followed
 * by {@link #ensureWindowCovers}, which allocates and establishes the window,
 * then attachment/detachment and metadata updates between searches, and finally
 * {@link #findVisible}. Structural state is single-threaded: attachment,
 * detachment, metadata updates, allocation, and rebasing must not overlap the
 * search. The search may run asynchronously and writes per-frame visit state
 * itself, but its array lengths, window coordinates, and installed membership
 * must remain stable for its duration. {@code RenderListManager} enforces this
 * boundary.
 */
public final class SectionLattice {
    private static final int SLACK = 4;

    /*
     * visitState representation:
     *
     * 63                    40 39                 8 7       0
     * +----------------------+---------------------+---------+
     * |       unused         |     frame stamp     | dir bits |
     * +----------------------+---------------------+---------+
     *
     * The frame stamp is the unsigned 32-bit search frame in bits 8..39;
     * the low byte accumulates incoming GraphDirectionSet bits. SENTINEL is
     * larger than every valid stamped value, so empty cells fail the ordered
     * visit gate. INITIAL is smaller than every frame stamp, so an installed
     * but unvisited cell is eligible for the current search.
     */
    static final int FRAME_SHIFT = 8;
    static final int DIR_MASK = 0xFF;

    // Empty slot, including the permanent border ring.
    static final long SENTINEL = Long.MAX_VALUE;
    // Installed slot that has not yet been visited by a search.
    static final long INITIAL = -1L;

    /*
     * Packed local coordinate: (lx << 20) | (ly << 10) | lz. Each local axis
     * therefore needs fewer than 1024 positions. MAX_DIM must stay within
     * that 10-bit limit, independently of MAX_LATTICE_SLOTS, which limits
     * the total number of cells (and therefore the allocation size).
     * packXyz produces this representation; XYZ_LOCAL_MASK extracts an axis;
     * XYZ_STEP is the corresponding per-direction increment.
     */
    static final int XYZ_SHIFT = 10;
    static final int XYZ_LOCAL_MASK = (1 << XYZ_SHIFT) - 1;
    static final int[] XYZ_STEP = new int[GraphDirection.COUNT];

    static {
        for (int dir = 0; dir < GraphDirection.COUNT; dir++) {
            XYZ_STEP[dir] = GraphDirection.x(dir) * (1 << (2 * XYZ_SHIFT))
                    + GraphDirection.y(dir) * (1 << XYZ_SHIFT)
                    + GraphDirection.z(dir);
        }
    }

    private static final int MAX_DIM = 1 << XYZ_SHIFT;
    private static final int MAX_LATTICE_SLOTS = 1 << 22;

    private final Long2ReferenceMap<RenderSection> sections = new Long2ReferenceOpenHashMap<>();
    private final OcclusionCuller culler;

    private final int minSectionY, maxSectionY;

    /*
     * Parallel primitive/object arrays, allocated lazily once the search
     * distance determines the X/Z dimensions. The linear index is
     * (lx * dimZ + lz) * dimY + ly, where local coordinates are relative to
     * (baseX, baseY, baseZ). Package-private visibility lets OcclusionCuller
     * read these arrays directly on its hot path.
     *
     * Invariants:
     * - only local coordinates [1, dim-2] are installable;
     * - the outer ring is always SENTINEL and has no section or metadata;
     * - all parallel arrays describe the same cell;
     * - installedCount equals the number of non-null latticeSection entries.
     */
    long[] visitState;
    long[] sectionMeta;
    int[] regionOfCell;
    RenderSection[] latticeSection;

    // Y is never windowed: the complete world height plus one sentinel cell at each vertical edge.
    final int dimY;
    final int baseY;
    int dimX, dimZ;
    // baseX/baseZ become meaningful after the first rebase; baseY is fixed by the world-height bounds.
    int baseX, baseZ;
    // Linear offset for one neighbour step in each direction; the sentinel border makes every step safe.
    final int[] delta = new int[GraphDirection.COUNT];
    private int strideX, strideZ;

    // True when the current dimensions, base coordinates, and installed placement are ready for a search.
    private boolean established;
    // Number of sections currently installed in the lattice, not total attached sections.
    int installedCount;

    public SectionLattice(int minSectionY, int maxSectionY) {
        this.minSectionY = minSectionY;
        this.maxSectionY = maxSectionY;
        this.baseY = minSectionY - 1;
        this.dimY = (maxSectionY - minSectionY) + 2;
        this.culler = new OcclusionCuller(this, minSectionY, maxSectionY);

        if (this.dimY > MAX_DIM) {
            throw new IllegalStateException("World height " + this.dimY + " exceeds occlusion lattice limit " + MAX_DIM);
        }
    }

    /**
     * Returns the attached section at a world position, regardless of whether
     * it currently has an installed lattice slot.
     *
     * @return the authoritative section at the position, or {@code null} if
     *         no section is attached
     */
    @Nullable
    public RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(PositionUtil.packSection(x, y, z));
    }

    /**
     * Attach a section to the authoritative collection and install it when
     * the lattice has been allocated and the section lies in its interior.
     * Attaching before the first window allocation is valid; the section will
     * be installed by the next allocation or rebase that covers it.
     */
    public void attach(RenderSection section) {
        var key = section.positionAsLong();

        if (this.sections.get(key) != null) {
            throw new IllegalStateException("Section already attached to lattice: " + section);
        }

        this.sections.put(key, section);

        this.install(section);
    }

    /**
     * Detach a section from the authoritative collection and remove its
     * lattice slot, if it is currently installed.
     */
    public void detach(RenderSection section) {
        var key = section.positionAsLong();

        RenderSection removed = this.sections.remove(key);

        if (removed == null) {
            throw new IllegalStateException("Section not attached to lattice: " + section);
        }

        this.uninstall(section);
    }

    /**
     * Mirror a section's full packed metadata word into its slot.
     *
     * @return true if the change altered the graph search's output (see {@link PackedSectionMetadata#GRAPH_INPUT_MASK})
     *         and a re-search is therefore warranted
     */
    public boolean updateSectionMetadata(int x, int y, int z, long packedMetadata) {
        int idx = this.installedIndex(x, y, z);

        if (idx < 0) {
            // Not installed (unloaded, no window yet, or outside the current
            // window): it is not traversed, and its metadata is read fresh
            // from the section when it is eventually installed.
            return false;
        }

        long previous = this.sectionMeta[idx];
        this.sectionMeta[idx] = packedMetadata;

        return ((previous ^ packedMetadata) & PackedSectionMetadata.GRAPH_INPUT_MASK) != 0;
    }

    /**
     * Returns the section installed at a lattice index.
     *
     * <p>Used by deferred visitor replay to resolve the section lazily; the
     * caller must provide an index belonging to an installed cell.
     */
    public RenderSection sectionAt(int latticeIndex) {
        return this.latticeSection[latticeIndex];
    }

    /**
     * Encodes a frame for the ordered visit gate. The low byte is reserved for
     * incoming direction bits, so the 32-bit frame occupies bits 8..39.
     */
    static long frameStamp(int frame) {
        return (frame & 0xFFFFFFFFL) << FRAME_SHIFT;
    }

    /**
     * Tests whether an installed section was marked visible in or after the
     * requested frame. Sections without an installed slot are never visible.
     */
    public boolean isSectionVisible(int x, int y, int z, int minVisibleFrame) {
        int idx = this.installedIndex(x, y, z);

        // The frame in which the slot was last marked visible by the search. Narrowing (int) after the logical
        // shift recovers the 32-bit frame; an uninstalled section (idx < 0) is treated as never-visible.
        return idx >= 0 && (int) (this.visitState[idx] >>> FRAME_SHIFT) >= minVisibleFrame;
    }

    /**
     * Returns the actual installed slot at a world position.
     *
     * <p>This is intentionally stricter than {@link #indexOf}: {@code indexOf}
     * computes a possible interior slot, whereas this method also verifies
     * that a section is attached and installed there.
     */
    private int installedIndex(int x, int y, int z) {
        if (this.visitState == null) {
            return -1;
        }

        int idx = this.indexOf(x, y, z);
        return idx >= 0 && this.latticeSection[idx] != null ? idx : -1;
    }

    /**
     * Ensure the lattice window covers the search radius around the camera.
     *
     * <p>The traversal can reach {@code radius} sections from the camera and
     * then inspect one more neighbour, so every section in that reach must be
     * in the installable interior rather than merely in the backing array.
     * The extra cell is what makes the border safe for unchecked neighbour
     * access. {@link #SLACK} leaves room for camera movement before another
     * rebase is needed.
     *
     * <p>The window is allocated lazily and grows when necessary. If the
     * camera or dimensions make the current window invalid, all attached
     * sections are reinstalled against the new base coordinates.
     *
     * <p>Call this on the render thread before dispatching a search. The
     * asynchronous search must not observe allocation, rebasing, attachment,
     * detachment, or metadata updates while it is running.
     */
    public void ensureWindowCovers(Vector3ic cameraSectionPos, float searchDistance) {
        int radius = MathUtil.mojfloor(searchDistance / 16.0f);
        int neededDimXZ = 2 * (radius + SLACK) + 3;

        if (this.visitState == null || neededDimXZ > this.dimX) {
            this.allocate(neededDimXZ);
        }

        int lx = cameraSectionPos.x() - this.baseX;
        int lz = cameraSectionPos.z() - this.baseZ;

        // Include the neighbour of the outermost section the traversal can visit.
        int reach = radius + 1;

        boolean valid = this.established
                && lx - reach >= 1 && lx + reach <= this.dimX - 2
                && lz - reach >= 1 && lz + reach <= this.dimZ - 2;

        if (!valid) {
            this.rebase(cameraSectionPos.x() - this.dimX / 2, cameraSectionPos.z() - this.dimZ / 2);
            this.established = true;
        }
    }

    /**
     * Run the visibility search over the currently installed lattice cells.
     * The caller must have completed {@link #ensureWindowCovers} and must keep
     * lattice structure and metadata stable until this method returns.
     */
    public void findVisible(OcclusionCuller.Visitor visitor,
                            Viewport viewport,
                            float searchDistance,
                            int numRegions,
                            boolean useOcclusionCulling,
                            int frame) {
        this.culler.findVisible(visitor, viewport, searchDistance, numRegions, useOcclusionCulling, frame);
    }

    private void allocate(int newDimXZ) {
        long slotsLong = (long) newDimXZ * newDimXZ * this.dimY;

        if (newDimXZ > MAX_DIM || slotsLong >= MAX_LATTICE_SLOTS) {
            throw new IllegalStateException("Occlusion lattice window too large (dimXZ=" + newDimXZ
                    + ", slots=" + slotsLong + "); this render distance is not supported");
        }

        this.dimX = newDimXZ;
        this.dimZ = newDimXZ;
        this.strideZ = this.dimY;
        this.strideX = this.dimY * this.dimZ;

        for (int dir = 0; dir < GraphDirection.COUNT; dir++) {
            this.delta[dir] = GraphDirection.x(dir) * this.strideX
                    + GraphDirection.z(dir) * this.strideZ
                    + GraphDirection.y(dir);
        }

        int slots = (int) slotsLong;
        this.visitState = new long[slots];
        this.sectionMeta = new long[slots];
        this.regionOfCell = new int[slots];
        this.latticeSection = new RenderSection[slots];

        // Existing coordinates are no longer valid after resizing; the next
        // ensureWindowCovers must rebase and reinstall every attached section.
        this.established = false;
    }

    /**
     * Clear the old window, move its X/Z origin, and reinstall every attached
     * section that falls in the new interior. Sections outside the interior
     * remain authoritative in {@link #sections} but are not traversable.
     */
    private void rebase(int newBaseX, int newBaseZ) {
        Arrays.fill(this.visitState, SENTINEL);
        Arrays.fill(this.sectionMeta, VisibilityEncoding.NULL);
        Arrays.fill(this.regionOfCell, -1);
        Arrays.fill(this.latticeSection, null);

        this.baseX = newBaseX;
        this.baseZ = newBaseZ;
        this.installedCount = 0;

        for (RenderSection section : this.sections.values()) {
            this.install(section);
        }
    }

    /**
     * Computes the potential lattice slot for a world position.
     *
     * @return the slot if the position is in the installable interior, or
     *         {@code -1} if it is outside the window, in the sentinel border,
     *         or outside the supported world-height range. This method does
     *         not test whether a section is actually installed at the slot.
     */
    int indexOf(int x, int y, int z) {
        int lx = x - this.baseX;
        int ly = y - this.baseY;
        int lz = z - this.baseZ;

        if (lx < 1 || lx > this.dimX - 2
                || ly < 1 || ly > this.dimY - 2
                || lz < 1 || lz > this.dimZ - 2) {
            return -1;
        }

        return (lx * this.dimZ + lz) * this.dimY + ly;
    }

    /**
     * Packs a world position's local coordinates into 10 bits per axis for
     * the traversal queue. The caller must pass a position in this lattice's
     * addressable window; {@code MAX_DIM} guarantees that each coordinate fits
     * in its 10-bit field, while {@code MAX_LATTICE_SLOTS} limits the total
     * backing-array size.
     */
    int packXyz(int x, int y, int z) {
        return ((x - this.baseX) << (2 * XYZ_SHIFT)) | ((y - this.baseY) << XYZ_SHIFT) | (z - this.baseZ);
    }

    // Install only into the interior. An attached section outside the current
    // window remains in sections and is picked up by a later rebase.
    private void install(RenderSection section) {
        if (this.visitState == null) {
            return;
        }

        int idx = this.indexOf(section.getChunkX(), section.getChunkY(), section.getChunkZ());

        if (idx < 0) {
            return;
        }

        this.visitState[idx] = INITIAL;
        this.latticeSection[idx] = section;
        this.regionOfCell[idx] = section.getRegion().getId();
        this.sectionMeta[idx] = section.getPackedMetadata();
        this.installedCount++;
    }

    // Remove the section's complete parallel-array entry. Write SENTINEL
    // first so traversal can never treat the remaining fields as live.
    private void uninstall(RenderSection section) {
        int idx = this.installedIndex(section.getChunkX(), section.getChunkY(), section.getChunkZ());

        if (idx < 0) {
            return;
        }

        this.visitState[idx] = SENTINEL;
        this.sectionMeta[idx] = VisibilityEncoding.NULL;
        this.regionOfCell[idx] = -1;
        this.latticeSection[idx] = null;
        this.installedCount--;
    }
}
