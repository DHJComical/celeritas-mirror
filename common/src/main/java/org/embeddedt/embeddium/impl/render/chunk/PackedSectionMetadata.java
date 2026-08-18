package org.embeddedt.embeddium.impl.render.chunk;

import org.jetbrains.annotations.Nullable;

/**
 * Packs the per-section state consumed by visibility traversal and render-list
 * collection into one {@code long}.
 *
 * <p>{@link RenderSection} owns the authoritative word. The
 * {@link org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionLattice}
 * mirrors it in a flat array so the hot traversal can read all of a section's
 * state with one indexed load instead of following several object references.
 * The visitor receives the unpacked fields without needing the section object
 * during the search.
 *
 * <p>Bit layout:
 * <pre>
 * 63                 53 52 51       49 48      46 45                0
 * +--------------------+--+-----------+----------+-------------------+
 * |       spare        |IF| pending   | visuals  | visibility graph  |
 * +--------------------+--+-----------+----------+-------------------+
 * </pre>
 * <ul>
 *   <li>Bits {@code 0..45}: {@link VisibilityEncoding} entries. Entry
 *       {@code (from * 8) + to} describes visibility from one of six faces to
 *       another. Each eight-bit row uses its six low bits; the two padding
 *       positions are not part of the visibility field (the final row's
 *       padding is reused by the visual flags).</li>
 *   <li>Bits {@code 46..48}: {@link RenderVisualsService} visual flags.</li>
 *   <li>Bits {@code 49..51}: pending update type; zero means none, otherwise
 *       {@code ChunkUpdateType.ordinal() + 1}.</li>
 *   <li>Bit {@code 52}: a build is in flight.</li>
 *   <li>Bits {@code 53..63}: reserved.</li>
 * </ul>
 *
 * <p>The visibility graph uses only bits {@code 0..45}, but
 * {@link VisibilityEncoding#getConnections(long)} folds high bits down into
 * its direction result. Therefore every value passed to that method must first
 * be masked with {@link #VISIBILITY_MASK}.
 *
 * <p>{@link #GRAPH_INPUT_MASK} identifies the fields that affect the graph
 * search or the render-list contents. Pending-update and build-in-flight bits
 * are consumed separately by the visitor and do not change graph traversal by
 * themselves.
 */
public final class PackedSectionMetadata {
    private PackedSectionMetadata() {
    }

    // Bits 0..45: visibility graph encoding. The 46-bit width is 6 * 8 - 2,
    // because six directions occupy six entries in each eight-bit row and the
    // final row is only needed through its sixth entry.
    private static final int VISIBILITY_BITS = 46;
    public static final long VISIBILITY_MASK = (1L << VISIBILITY_BITS) - 1;

    // Bits 46..48: RenderVisualsService.HAS_* flags.
    private static final int VISUALS_FLAGS_SHIFT = 46;
    private static final long VISUALS_FLAGS_MASK = 0b111L;

    /**
     * Fields whose changes can alter the result of a visibility search or its
     * render-list output: visibility graph data and visual-presence flags.
     */
    public static final long GRAPH_INPUT_MASK = VISIBILITY_MASK | (VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT);

    // Bits 49..51: 0 means no pending update; otherwise ordinal + 1.
    private static final int PENDING_UPDATE_SHIFT = 49;
    private static final long PENDING_UPDATE_MASK = 0b111L;

    // Bit 52: a build cancellation token is currently attached.
    private static final int BUILD_IN_FLIGHT_BIT = 52;
    private static final long BUILD_IN_FLIGHT_FLAG = 1L << BUILD_IN_FLIGHT_BIT;

    /** Returns only the visibility graph field. */
    public static long getVisibilityData(long packed) {
        return packed & VISIBILITY_MASK;
    }

    /**
     * Replaces the visibility graph field and preserves every other field.
     * Bits outside {@link #VISIBILITY_MASK} in {@code visibilityData} are
     * discarded.
     */
    public static long withVisibilityData(long packed, long visibilityData) {
        return (packed & ~VISIBILITY_MASK) | (visibilityData & VISIBILITY_MASK);
    }

    /** Returns the three visual-presence flags. */
    public static int getVisualsFlags(long packed) {
        return (int) ((packed >>> VISUALS_FLAGS_SHIFT) & VISUALS_FLAGS_MASK);
    }

    /**
     * Replaces the visual-presence flags and preserves every other field.
     * Only the three low bits of {@code flags} are stored.
     */
    public static long withVisualsFlags(long packed, int flags) {
        return (packed & ~(VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT))
                | (((long) flags & VISUALS_FLAGS_MASK) << VISUALS_FLAGS_SHIFT);
    }

    /**
     * Decodes the pending update field.
     *
     * @return the pending update type, or {@code null} when the field is zero
     */
    public static @Nullable ChunkUpdateType getPendingUpdate(long packed) {
        int encoded = (int) ((packed >>> PENDING_UPDATE_SHIFT) & PENDING_UPDATE_MASK);
        return encoded == 0 ? null : ChunkUpdateType.VALUES[encoded - 1];
    }

    /**
     * Replaces the pending update field and preserves every other field.
     * {@code null} clears the field; non-null values use ordinal plus one so
     * zero remains the no-update sentinel.
     */
    public static long withPendingUpdate(long packed, @Nullable ChunkUpdateType type) {
        long encoded = type == null ? 0L : (type.ordinal() + 1L);
        return (packed & ~(PENDING_UPDATE_MASK << PENDING_UPDATE_SHIFT))
                | (encoded << PENDING_UPDATE_SHIFT);
    }

    /** Returns whether a section build is currently in flight. */
    public static boolean isBuildInFlight(long packed) {
        return (packed & BUILD_IN_FLIGHT_FLAG) != 0;
    }

    /*
     * Compact collector metadata: includes only the info the VisibleChunkCollector cares about.
     *
     *   6  5     3 2       0
     *   +--+-------+--------+
     *   |IF|pending|visuals |
     *   +--+-------+--------+
     */
    private static final int COMPACT_META_SHIFT = VISUALS_FLAGS_SHIFT;
    private static final long COMPACT_META_FIELDS =
            (VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT)
                    | (PENDING_UPDATE_MASK << PENDING_UPDATE_SHIFT)
                    | BUILD_IN_FLIGHT_FLAG;
    private static final int COMPACT_META_MASK = (int) (COMPACT_META_FIELDS >>> COMPACT_META_SHIFT);
    private static final int COMPACT_PENDING_SHIFT = PENDING_UPDATE_SHIFT - COMPACT_META_SHIFT;
    private static final int COMPACT_BUILD_IN_FLIGHT_BIT = BUILD_IN_FLIGHT_BIT - COMPACT_META_SHIFT;

    public static int toCompactMeta(long packed) {
        return (int) (packed >>> COMPACT_META_SHIFT) & COMPACT_META_MASK;
    }

    public static int getCompactVisualsFlags(int meta) {
        return meta & (int) VISUALS_FLAGS_MASK;
    }

    /** @return the pending update type, or {@code null} when the field is zero */
    public static @Nullable ChunkUpdateType getCompactPendingUpdate(int meta) {
        int encoded = (meta >>> COMPACT_PENDING_SHIFT) & (int) PENDING_UPDATE_MASK;
        return encoded == 0 ? null : ChunkUpdateType.VALUES[encoded - 1];
    }

    public static boolean isCompactBuildInFlight(int meta) {
        return (meta & (1 << COMPACT_BUILD_IN_FLIGHT_BIT)) != 0;
    }

    /**
     * Replaces the build-in-flight flag and preserves every other field.
     */
    public static long withBuildInFlight(long packed, boolean inFlight) {
        return inFlight ? (packed | BUILD_IN_FLIGHT_FLAG) : (packed & ~BUILD_IN_FLIGHT_FLAG);
    }
}
