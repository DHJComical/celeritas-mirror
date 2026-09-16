package org.embeddedt.embeddium.impl.render.chunk.sorting;

public sealed interface SortState {
    SortState NONE = new None();

    /** Variant names for the debug overlay, indexed by {@link #debugIndex()} and ordered by sorting cost */
    String[] DEBUG_NAMES = { "NONE", "STATIC", "TREE", "DYNAMIC" };

    /** Position in {@link #DEBUG_NAMES}, doubling as the cost ranking used to pick a section's highest variant */
    int debugIndex();

    int quadCount();

    SortState compactForStorage();

    default boolean requiresDynamicSorting() {
        return this instanceof Resortable;
    }

    /** A state whose order depends on the camera, and so survives compaction to be re-sorted on every move */
    sealed interface Resortable extends SortState permits Dynamic, PartitionTree {
        @Override
        default Resortable compactForStorage() {
            return this;
        }
    }

    /** Any draw order is correct */
    record None() implements SortState {
        @Override
        public int debugIndex() {
            return 0;
        }

        @Override
        public int quadCount() {
            return 0;
        }

        @Override
        public SortState compactForStorage() {
            return NONE;
        }
    }

    /** One camera-independent order, computed once during meshing */
    record Static(int[] order) implements SortState {
        @Override
        public int debugIndex() {
            return 1;
        }

        @Override
        public int quadCount() {
            return order.length;
        }

        // The order is consumed by the index buffer written at build time, and can never change afterwards
        @Override
        public SortState compactForStorage() {
            return NONE;
        }
    }

    /** Resortable, so the quad centers are kept for every re-sort */
    record Dynamic(float[] centers) implements Resortable {
        @Override
        public int debugIndex() {
            return 3;
        }

        @Override
        public int quadCount() {
            return centers.length / 3;
        }
    }
}
