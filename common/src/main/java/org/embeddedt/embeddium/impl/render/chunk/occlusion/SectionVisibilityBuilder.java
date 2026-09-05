package org.embeddedt.embeddium.impl.render.chunk.occlusion;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.geometry.BoxFinder;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.geometry.OccluderBoxes;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A helper class that computes the visibility graph for a section, inspired by the writeup on
 * <a href="https://tomcc.github.io/2014/08/31/visibility-1.html">Tommaso Checchi's blog</a>.
 */
public class SectionVisibilityBuilder {
    private static final int SECTION_AXIS_SIZE = 16;
    private static final int SECTION_AXIS_MASK = SECTION_AXIS_SIZE - 1;
    static final int TOTAL_BLOCKS = SECTION_AXIS_SIZE * SECTION_AXIS_SIZE * SECTION_AXIS_SIZE;
    private static final int BLOCKS_ON_ONE_FACE = SECTION_AXIS_SIZE * SECTION_AXIS_SIZE;
    private static final int BITS_PER_AXIS = 4;
    private static final int X_SHIFT = 0;
    private static final int Z_SHIFT = BITS_PER_AXIS;
    private static final int Y_SHIFT = BITS_PER_AXIS * 2;

    /**
     * All indices that are touching the edge of a section. These are used as the starting points for the floodfill.
     */
    private static final int[] INDICES_TO_INITIATE_FLOODFILL = buildFloodfillIndices();

    private final BitSet blocks;
    private final RenderableBounds renderableBounds = new RenderableBounds();

    private static int[] buildFloodfillIndices() {
        IntArrayList indicesList = new IntArrayList(TOTAL_BLOCKS - (SECTION_AXIS_SIZE - 2) * (SECTION_AXIS_SIZE - 2) * (SECTION_AXIS_SIZE - 2));
        for (int x = 0; x < SECTION_AXIS_SIZE; x++) {
            for(int z = 0; z < SECTION_AXIS_SIZE; z++) {
                for(int y = 0; y < SECTION_AXIS_SIZE; y++) {
                    if (x == 0 || x == (SECTION_AXIS_SIZE - 1) || y == 0 || y == (SECTION_AXIS_SIZE - 1) || z == 0 || z == (SECTION_AXIS_SIZE - 1)) {
                        indicesList.add(getIndex(x, y, z));
                    }
                }
            }
        }
        return indicesList.toIntArray();
    }

    public SectionVisibilityBuilder() {
        this.blocks = new BitSet(SECTION_AXIS_SIZE * SECTION_AXIS_SIZE * SECTION_AXIS_SIZE);
    }

    public long computeVisibilityEncoding() {
        int opaqueCount = blocks.cardinality();
        if (opaqueCount == TOTAL_BLOCKS) {
            // everything is opaque, so we can't see anything anywhere
            return VisibilityEncoding.NULL;
        } else if (opaqueCount < BLOCKS_ON_ONE_FACE) {
            // There are not enough blocks set to even fully cover one face. Therefore, we must be able
            // to see everything from everything.
            return VisibilityEncoding.EVERYTHING;
        } else {
            return computeWithFloodFill();
        }
    }

    public void markOpaque(int x, int y, int z) {
        this.blocks.set(getIndex(x & 15, y & 15, z & 15));
    }

    public void markRenderable(int x, int y, int z) {
        this.renderableBounds.mark(x & 15, y & 15, z & 15);
    }

    public int @Nullable [] computeOccluderBoxes() {
        return dataFrom(this.blocks, this.renderableBounds);
    }

    static int bounds(int[] data) {
        return data[0];
    }

    private static int @Nullable [] dataFrom(BitSet blocks, RenderableBounds bounds) {
        if (blocks.isEmpty() && bounds.isEmpty()) {
            return null;
        }

        int[] boxes = blocks.isEmpty() ? null
                : OccluderBoxes.from(Arrays.copyOf(blocks.toLongArray(), BoxFinder.INTERIOR_CACHE_WORDS));

        int count = boxes == null ? 0 : boxes.length;
        int[] data = new int[count + 1];
        data[0] = bounds.packed();

        if (boxes != null) {
            System.arraycopy(boxes, 0, data, 1, count);
        }

        return data;
    }

    private long computeWithFloodFill() {
        long resultEncoding = 0;
        // The fill marks cells as visited by setting their bit, which would corrupt the opacity data other
        // callers (computeOccluderBoxes()) still need. Clone it into scratch space instead of mutating
        // this.blocks in place; opaque cells start pre-marked since the fill must not pass through them.
        BitSet visited = (BitSet) this.blocks.clone();
        BitSet escapedFaces = new BitSet(GraphDirection.COUNT);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        for (int i : INDICES_TO_INITIATE_FLOODFILL) {
            if (!visited.get(i)) {
                escapedFaces.clear();
                queue.clear();
                exploreFrom(visited, escapedFaces, queue, i);
                for (int dir = escapedFaces.nextSetBit(0); dir >= 0; dir = escapedFaces.nextSetBit(dir+1)) {
                    for (int dir2 = escapedFaces.nextSetBit(0); dir2 >= 0; dir2 = escapedFaces.nextSetBit(dir2+1)) {
                        resultEncoding |= 1L << VisibilityEncoding.bit(dir, dir2);
                    }
                }
            }
        }
        return resultEncoding;
    }

    private static void exploreFrom(BitSet visited, BitSet escapedFaces, IntArrayFIFOQueue queue, int startIndex) {
        queue.enqueue(startIndex);
        // Mark the start location as handled
        visited.set(startIndex, true);
        while (!queue.isEmpty()) {
            int idx = queue.dequeueInt();
            for (int dir = 0; dir < GraphDirection.COUNT; dir++) {
                int neighborIdx = getNeighborIndex(idx, dir);
                if (neighborIdx >= 0) {
                    // We can move within the section in that direction
                    if (!visited.get(neighborIdx)) {
                        // Mark this location as handled
                        visited.set(neighborIdx, true);
                        queue.enqueue(neighborIdx);
                    }
                } else {
                    // We moved out of the section, mark this as an escaping face
                    escapedFaces.set(dir);
                }
            }
        }
    }

    private static int getNeighborIndex(int idx, int dir) {
        switch (dir) {
            case GraphDirection.UP -> {
                if (((idx >> Y_SHIFT) & SECTION_AXIS_MASK) == SECTION_AXIS_MASK) {
                    return -1;
                } else {
                    return idx + (1 << Y_SHIFT);
                }
            }
            case GraphDirection.DOWN -> {
                if (((idx >> Y_SHIFT) & SECTION_AXIS_MASK) == 0) {
                    return -1;
                } else {
                    return idx - (1 << Y_SHIFT);
                }
            }
            case GraphDirection.EAST -> {
                if (((idx >> X_SHIFT) & SECTION_AXIS_MASK) == SECTION_AXIS_MASK) {
                    return -1;
                } else {
                    return idx + (1 << X_SHIFT);
                }
            }
            case GraphDirection.WEST -> {
                if (((idx >> X_SHIFT) & SECTION_AXIS_MASK) == 0) {
                    return -1;
                } else {
                    return idx - (1 << X_SHIFT);
                }
            }
            case GraphDirection.SOUTH -> {
                if (((idx >> Z_SHIFT) & SECTION_AXIS_MASK) == SECTION_AXIS_MASK) {
                    return -1;
                } else {
                    return idx + (1 << Z_SHIFT);
                }
            }
            case GraphDirection.NORTH -> {
                if (((idx >> Z_SHIFT) & SECTION_AXIS_MASK) == 0) {
                    return -1;
                } else {
                    return idx - (1 << Z_SHIFT);
                }
            }
            default -> throw new IllegalArgumentException();
        }
    }

    static int getIndex(int x, int y, int z) {
        return (y << Y_SHIFT) | (z << Z_SHIFT) | (x << X_SHIFT);
    }
}
