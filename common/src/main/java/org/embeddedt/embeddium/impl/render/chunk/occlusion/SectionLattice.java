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
 * establishes their base coordinates. If a later camera position no longer
 * fits, a same-sized window is normally slid in place: the overlapping cells
 * are copied, and only the newly exposed X/Z band is looked up in
 * {@code sections}. A resize or a move with no overlap falls back to a full
 * {@link #rebase}.
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

    // Two reusable destination buffers for VisibilitySnapshot, alternated every search. Copying into a
    // recycled buffer avoids allocating a fresh snapshot per frame. Two are needed rather than one
    // so a search never overwrites the buffer backing the previously returned snapshot, which a reader on
    // another thread may still hold: search k writes buffer[k % 2] while buffer[(k-1) % 2] stays readable.
    // The buffers hold only the decoded per-cell frame stamps, not full visitState words: the snapshot
    // answers nothing but frame queries, and copying ints halves the memory traffic of the per-search copy.
    private final int[][] snapshotBuffers = new int[2][];
    private int snapshotParity;

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
     * Self-contained view of the lattice's visibility state, safe to query from
     * a thread other than the one mutating the lattice.
     *
     * <p>{@link SectionLattice#findVisible} runs asynchronously and mutates the
     * live lattice while another thread queries section visibility. Rather than
     * read the live arrays across that boundary, a search returns a snapshot: a
     * copy of each cell's decoded frame stamp plus the window geometry that
     * indexes it. The snapshot's array is not the live lattice, so a running
     * search's mutations cannot race a query against it.
     *
     * <p>To avoid a per-frame allocation, {@code visibleFrames} is a buffer
     * recycled from {@code snapshotBuffers} rather than a fresh clone. The
     * lattice alternates between two buffers so the buffer a new search
     * overwrites is never the one backing the currently published snapshot; see
     * {@link SectionLattice#snapshot()}.
     *
     * @param visibleFrames the frame in which each cell was last reached by a
     *                      search, already shifted down from the visitState
     *                      encoding; a recycled buffer that stays valid until
     *                      the search two generations later overwrites it
     */
    public record VisibilitySnapshot(int[] visibleFrames, int baseX, int baseY, int baseZ,
                                     int dimX, int dimY, int dimZ) {
        /** A snapshot with no window, reporting every section as not visible. */
        public static final VisibilitySnapshot EMPTY =
                new VisibilitySnapshot(new int[0], 0, 0, 0, 0, 0, 0);

        /**
         * Tests whether the section at a world position was marked visible in or
         * after the requested frame. Positions outside the captured window, or
         * never visited, are treated as never-visible.
         */
        public boolean isSectionVisible(int x, int y, int z, int minVisibleFrame) {
            int lx = x - this.baseX;
            int ly = y - this.baseY;
            int lz = z - this.baseZ;

            // Same interior test as SectionLattice.indexOf: only cells strictly
            // inside the sentinel border can hold a section, so anything outside
            // it (including an empty snapshot) is never visible.
            if (lx < 1 || lx > this.dimX - 2
                    || ly < 1 || ly > this.dimY - 2
                    || lz < 1 || lz > this.dimZ - 2) {
                return false;
            }

            int idx = (lx * this.dimZ + lz) * this.dimY + ly;

            // The frame in which the slot was last marked visible by the search. The snapshot copy already
            // decoded the stamp; an unvisited or empty cell held INITIAL/SENTINEL, which decode to a value
            // below any real frame and are therefore treated as never-visible.
            return this.visibleFrames[idx] >= minVisibleFrame;
        }
    }

    private VisibilitySnapshot snapshot() {
        if (this.visitState == null) {
            return VisibilitySnapshot.EMPTY;
        }

        int parity = this.snapshotParity;
        this.snapshotParity ^= 1;

        long[] src = this.visitState;
        int[] dst = this.snapshotBuffers[parity];

        if (dst == null || dst.length != src.length) {
            dst = new int[src.length];
            this.snapshotBuffers[parity] = dst;
        }

        // Decode each cell's frame stamp while copying. Narrowing (int) after the logical shift recovers
        // the 32-bit frame, and INITIAL/SENTINEL both truncate to -1, below every real frame. The loop
        // auto-vectorizes on JDK 21 C2 and, being store-bound, stays cheaper than a full long[] copy even
        // on JVMs that compile it scalar.
        for (int i = 0; i < src.length; i++) {
            dst[i] = (int) (src[i] >>> FRAME_SHIFT);
        }

        return new VisibilitySnapshot(dst, this.baseX, this.baseY, this.baseZ,
                this.dimX, this.dimY, this.dimZ);
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
     * <p>The window is allocated lazily and grows when necessary. A first
     * allocation or resize invalidates the old coordinates and requires a full
     * rebuild. Otherwise, when the camera makes the current window invalid,
     * the existing window is typically slid to the centered base: overlapping cells are
     * retained and only the newly exposed band is resolved through the
     * authoritative section map.
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
            int newBaseX = cameraSectionPos.x() - this.dimX / 2;
            int newBaseZ = cameraSectionPos.z() - this.dimZ / 2;

            if (this.established) {
                // The current window is valid to slide from: reuse its retained
                // contents and only resolve the newly exposed cells.
                this.shiftRebase(newBaseX, newBaseZ);
            } else {
                // Fresh or resized arrays have no reusable contents.
                this.rebase(newBaseX, newBaseZ);
            }

            this.established = true;
        }
    }

    /**
     * Run the visibility search over the currently installed lattice cells.
     * The caller must have completed {@link #ensureWindowCovers} and must keep
     * lattice structure and metadata stable until this method returns.
     *
     * @return a snapshot of the visit state this search produced
     */
    public VisibilitySnapshot findVisible(OcclusionCuller.Visitor visitor,
                                          Viewport viewport,
                                          float searchDistance,
                                          int numRegions,
                                          boolean useOcclusionCulling,
                                          boolean allowFrustumClamping,
                                          int frame) {
        this.culler.findVisible(visitor, viewport, searchDistance, numRegions, useOcclusionCulling, allowFrustumClamping, frame);

        return this.snapshot();
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
     * Rebuilds the lattice from scratch after allocation, resizing, or a
     * non-overlapping camera jump.
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
     * Slide the existing window to a new X/Z origin without rebuilding it.
     *
     * <p>{@code dX} and {@code dZ} are the changes in world-space base
     * coordinates.
     *
     * <p>Before overwriting the old array cells, {@link #dropLeaving} removes
     * the installed-count contribution of cells outside that intersection.
     * After the move, {@link #fillNewBand} clears the cells that entered the
     * window and installs only sections found in the authoritative map. Thus
     * the map is consulted only for the exposed band, rather than once for
     * every attached section as in {@link #rebase}.
     *
     * <p>When the old and new interiors do not intersect, there is no useful
     * data to preserve and this method delegates to {@link #rebase}.
     */
    private void shiftRebase(int newBaseX, int newBaseZ) {
        int dX = newBaseX - this.baseX;
        int dZ = newBaseZ - this.baseZ;

        int interiorXHi = this.dimX - 2;
        int interiorZHi = this.dimZ - 2;

        int xLo = Math.max(1, 1 - dX);
        int xHi = Math.min(interiorXHi, interiorXHi - dX);
        int zLo = Math.max(1, 1 - dZ);
        int zHi = Math.min(interiorZHi, interiorZHi - dZ);

        if (xLo > xHi || zLo > zHi) {
            // No overlap: sliding would move nothing, so reinstall wholesale.
            this.rebase(newBaseX, newBaseZ);
            return;
        }

        this.dropLeaving(xLo + dX, xHi + dX, zLo + dZ, zHi + dZ);

        if (dZ == 0) {
            this.shiftContentsXOnly(dX, xLo, xHi);
        } else {
            this.shiftContents(dX, dZ, xLo, xHi, zLo, zHi);
        }

        // The arrays now describe cells relative to the new origin.
        this.baseX = newBaseX;
        this.baseZ = newBaseZ;

        this.fillNewBand(xLo, xHi, zLo, zHi);
    }

    /**
     * Move retained contents for a pure X shift.
     *
     * <p>Whole X planes (including their permanent sentinel rows) translate
     * uniformly by {@code dX}, so the retained planes form one contiguous
     * range. A single {@link System#arraycopy} per parallel array performs the
     * overlapping move safely.
     */
    private void shiftContentsXOnly(int dX, int xLo, int xHi) {
        int destOff = xLo * this.strideX;
        int srcOff = (xLo + dX) * this.strideX;
        int len = (xHi - xLo + 1) * this.strideX;

        System.arraycopy(this.visitState, srcOff, this.visitState, destOff, len);
        System.arraycopy(this.sectionMeta, srcOff, this.sectionMeta, destOff, len);
        System.arraycopy(this.regionOfCell, srcOff, this.regionOfCell, destOff, len);
        System.arraycopy(this.latticeSection, srcOff, this.latticeSection, destOff, len);
    }

    /**
     * Move retained contents for a shift that includes Z.
     *
     * <p>A Z shift cannot cross an X-plane boundary, so it moves one
     * contiguous {@code Z/Y} block per retained X-plane. The loop direction
     * keeps a plane's source intact until it has been copied;
     * {@link System#arraycopy} handles overlap within that block.
     */
    private void shiftContents(int dX, int dZ, int xLo, int xHi, int zLo, int zHi) {
        // Each Z within [zLo, zHi] contributes a full contiguous Y run, so the
        // whole per-plane block is one run. The Y sentinels carried along are
        // copied from equivalent source sentinels and stay empty.
        int runLen = (zHi - zLo + 1) * this.strideZ;

        // When data moves to lower indices (dX > 0) copy ascending so a source
        // plane is read before a later write can reach it; descend otherwise.
        int first = dX < 0 ? xHi : xLo;
        int last = dX < 0 ? xLo : xHi;
        int step = dX < 0 ? -1 : 1;

        for (int lx = first; lx != last + step; lx += step) {
            int destOff = lx * this.strideX + zLo * this.strideZ;
            int srcOff = (lx + dX) * this.strideX + (zLo + dZ) * this.strideZ;

            System.arraycopy(this.visitState, srcOff, this.visitState, destOff, runLen);
            System.arraycopy(this.sectionMeta, srcOff, this.sectionMeta, destOff, runLen);
            System.arraycopy(this.regionOfCell, srcOff, this.regionOfCell, destOff, runLen);
            System.arraycopy(this.latticeSection, srcOff, this.latticeSection, destOff, runLen);
        }
    }

    /**
     * Account for cells that leave the window before their slots are reused.
     *
     * <p>The source rectangle is the old interior that supplies the retained
     * destination rectangle. Every installed cell outside it loses its slot,
     * so its contribution to {@link #installedCount} is decremented here.
     * Those sections remain in {@link #sections}; their stale array entries
     * are subsequently overwritten by {@link #shiftContentsXOnly} or
     * {@link #shiftContents}, then cleared by {@link #fillNewBand}.
     *
     * <p>Because the old and new windows differ by a translation, the retained
     * source rectangle touches an edge in each shifted axis. Its complement is
     * therefore two non-overlapping rectangles: an X-only strip and a Z-only
     * strip. Iterating those rectangles avoids testing rectangle membership for
     * every cell.
     */
    private void dropLeaving(int srcXLo, int srcXHi, int srcZLo, int srcZHi) {
        int interiorXHi = this.dimX - 2;
        int interiorZHi = this.dimZ - 2;
        int interiorYHi = this.dimY - 2;

        int xBandLo = srcXLo == 1 ? srcXHi + 1 : 1;
        int xBandHi = srcXLo == 1 ? interiorXHi : srcXLo - 1;
        int zBandLo = srcZLo == 1 ? srcZHi + 1 : 1;
        int zBandHi = srcZLo == 1 ? interiorZHi : srcZLo - 1;

        this.dropLeavingRectangle(xBandLo, xBandHi, 1, interiorZHi, interiorYHi);
        this.dropLeavingRectangle(srcXLo, srcXHi, zBandLo, zBandHi, interiorYHi);
    }

    private void dropLeavingRectangle(int xLo, int xHi, int zLo, int zHi, int interiorYHi) {
        for (int lx = xLo; lx <= xHi; lx++) {
            for (int lz = zLo; lz <= zHi; lz++) {
                int col = lx * this.strideX + lz * this.strideZ;
                for (int ly = 1; ly <= interiorYHi; ly++) {
                    if (this.latticeSection[col + ly] != null) {
                        this.installedCount--;
                    }
                }
            }
        }
    }

    /**
     * Clear and repopulate the L-shaped band of cells that entered the window.
     *
     * <p>As with {@link #dropLeaving}, the band is visited as two
     * non-overlapping rectangles: the X-only strip outside the retained X
     * range, followed by the Z-only strip inside the retained X range.
     */
    private void fillNewBand(int keepXLo, int keepXHi, int keepZLo, int keepZHi) {
        int interiorXHi = this.dimX - 2;
        int interiorZHi = this.dimZ - 2;
        int interiorYHi = this.dimY - 2;

        int xBandLo = keepXLo == 1 ? keepXHi + 1 : 1;
        int xBandHi = keepXLo == 1 ? interiorXHi : keepXLo - 1;
        int zBandLo = keepZLo == 1 ? keepZHi + 1 : 1;
        int zBandHi = keepZLo == 1 ? interiorZHi : keepZLo - 1;

        this.fillBandRectangle(xBandLo, xBandHi, 1, interiorZHi, interiorYHi);
        this.fillBandRectangle(keepXLo, keepXHi, zBandLo, zBandHi, interiorYHi);
    }

    private void fillBandRectangle(int xLo, int xHi, int zLo, int zHi, int interiorYHi) {
        for (int lx = xLo; lx <= xHi; lx++) {
            int worldX = this.baseX + lx;

            for (int lz = zLo; lz <= zHi; lz++) {
                int worldZ = this.baseZ + lz;
                int col = lx * this.strideX + lz * this.strideZ;

                for (int ly = 1; ly <= interiorYHi; ly++) {
                    int idx = col + ly;

                    this.visitState[idx] = SENTINEL;
                    this.sectionMeta[idx] = VisibilityEncoding.NULL;
                    this.regionOfCell[idx] = -1;
                    this.latticeSection[idx] = null;

                    RenderSection section = this.sections.get(
                            PositionUtil.packSection(worldX, this.baseY + ly, worldZ));

                    if (section != null) {
                        this.install(section);
                    }
                }
            }
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
