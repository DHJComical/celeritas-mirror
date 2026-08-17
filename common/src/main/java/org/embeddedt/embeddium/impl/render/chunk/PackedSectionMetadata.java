package org.embeddedt.embeddium.impl.render.chunk;

import org.jetbrains.annotations.Nullable;

/**
 * The canonical packed encoding of everything the visibility graph search and its visitor need to know about a
 * section, in a single {@code long}.
 *
 * <h2>Why</h2>
 * The search walks tens of thousands of sections per frame. Every field it needs off a {@link RenderSection}
 * (visibility encoding, "does this have anything to render", pending update type, "is a build already in
 * flight") used to be a load off a randomly-placed object. Packed into one word they are a single load out of a
 * flat array the traversal already has the index for - the lattice's {@code sectionMeta}, see
 * {@code SectionLattice} - and the visitor can be handed plain ints.
 *
 * <h2>Layout</h2>
 * <pre>
 *   bits  0..45 : visibility encoding ({@link VisibilityEncoding}: bit {@code (from * 8) + to}, from/to in [0, 6))
 *   bits 46..48 : visuals flags ({@link RenderVisualsService#HAS_BLOCK_GEOMETRY} / {@code HAS_SPRITES} /
 *                 {@code HAS_BLOCK_ENTITIES}), i.e. {@code RenderSection.getVisualsServiceFlags()}
 *   bits 49..51 : pending update type; 0 = none, otherwise {@code ChunkUpdateType.ordinal() + 1}
 *   bit  52     : a build cancellation token is currently attached (a build is in flight)
 *   bits 53..63 : spare
 * </pre>
 * The visibility encoding structurally cannot occupy anything above bit 45 ({@code 5 * 8 + 5}), so bits 46 and
 * up are free. <b>They are not, however, harmless</b>: {@link VisibilityEncoding#getConnections(long)} folds the
 * top 32 bits of the word down onto the bottom 6 (bit 46 lands on EAST), so every read that feeds a
 * {@code getConnections} call <b>must</b> mask with {@link #VISIBILITY_MASK} first. See
 * {@code OcclusionCuller.process} and {@code OcclusionCuller.initWithinWorld}.
 *
 * <h2>The small "meta bits" form</h2>
 * Bits 46..52 - everything except the visibility encoding - are what the visitor actually consumes.
 *
 * <p>Note: {@link RenderSection} is the source of truth - it keeps this whole word in sync (including the
 * visibility encoding) via {@code updateCachedContextDataFlags()} and notifies {@code SectionLattice}, which
 * mirrors it into the flat {@code sectionMeta} array the search reads.</p>
 */
public final class PackedSectionMetadata {
    private PackedSectionMetadata() {
    }

    // Bits 0..45: visibility encoding, see VisibilityEncoding
    private static final int VISIBILITY_BITS = 46;
    public static final long VISIBILITY_MASK = (1L << VISIBILITY_BITS) - 1;

    // Bits 46..48: visuals flags (RenderVisualsService.HAS_*)
    private static final int VISUALS_FLAGS_SHIFT = 46;
    private static final long VISUALS_FLAGS_MASK = 0b111L;

    // The bits whose change alters a graph search's output (which sections traverse, which end up in the render
    // list): the visibility encoding plus the visuals flags. Changes confined outside this mask (pending update
    // type, build-in-flight) do not by themselves require re-running the search - their callers mark the graph
    // dirty separately.
    public static final long GRAPH_INPUT_MASK = VISIBILITY_MASK | (VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT);

    // Bits 49..51: pending update type; 0 = none, otherwise ChunkUpdateType.ordinal() + 1
    private static final int PENDING_UPDATE_SHIFT = 49;
    private static final long PENDING_UPDATE_MASK = 0b111L;

    // Bit 52: a build cancellation token is currently attached (a build is in flight)
    private static final int BUILD_IN_FLIGHT_BIT = 52;
    private static final long BUILD_IN_FLIGHT_FLAG = 1L << BUILD_IN_FLIGHT_BIT;

    public static long getVisibilityData(long packed) {
        return packed & VISIBILITY_MASK;
    }

    public static long withVisibilityData(long packed, long visibilityData) {
        return (packed & ~VISIBILITY_MASK) | (visibilityData & VISIBILITY_MASK);
    }

    public static int getVisualsFlags(long packed) {
        return (int) ((packed >>> VISUALS_FLAGS_SHIFT) & VISUALS_FLAGS_MASK);
    }

    public static long withVisualsFlags(long packed, int flags) {
        return (packed & ~(VISUALS_FLAGS_MASK << VISUALS_FLAGS_SHIFT))
                | (((long) flags & VISUALS_FLAGS_MASK) << VISUALS_FLAGS_SHIFT);
    }

    public static @Nullable ChunkUpdateType getPendingUpdate(long packed) {
        int encoded = (int) ((packed >>> PENDING_UPDATE_SHIFT) & PENDING_UPDATE_MASK);
        return encoded == 0 ? null : ChunkUpdateType.VALUES[encoded - 1];
    }

    public static long withPendingUpdate(long packed, @Nullable ChunkUpdateType type) {
        long encoded = type == null ? 0L : (type.ordinal() + 1L);
        return (packed & ~(PENDING_UPDATE_MASK << PENDING_UPDATE_SHIFT))
                | (encoded << PENDING_UPDATE_SHIFT);
    }

    public static boolean isBuildInFlight(long packed) {
        return (packed & BUILD_IN_FLIGHT_FLAG) != 0;
    }

    public static long withBuildInFlight(long packed, boolean inFlight) {
        return inFlight ? (packed | BUILD_IN_FLIGHT_FLAG) : (packed & ~BUILD_IN_FLIGHT_FLAG);
    }
}
