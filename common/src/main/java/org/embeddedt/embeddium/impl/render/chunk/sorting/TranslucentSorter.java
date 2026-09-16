package org.embeddedt.embeddium.impl.render.chunk.sorting;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.util.sorting.MergeSort;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

public final class TranslucentSorter {
    /**
     * How far along the shared normal the imaginary camera used by {@link #sortNormalRelative} sits. Any value
     * comfortably beyond the section is equivalent, since only the ordering of the projections matters.
     */
    static final int FAKE_STATIC_CAMERA_OFFSET = 1000;

    /**
     * How closely two normals must agree to be treated as parallel. Only +/-1 truly imply a shared normal, but
     * accepting near misses lets very slightly slanted water (the edges of underwater lakes) sort statically.
     */
    private static final float PARALLEL_THRESHOLD = 0.98f;

    private TranslucentSorter() {
    }

    /**
     * Orders quads by their distance to the camera, farthest first.
     */
    public static int[] sortByDistance(float[] centers, int quadCount, float camX, float camY, float camZ) {
        float[] distances = new float[quadCount];

        for (int quadIdx = 0; quadIdx < quadCount; quadIdx++) {
            int centerIdx = quadIdx * 3;

            float qX = centers[centerIdx] - camX;
            float qY = centers[centerIdx + 1] - camY;
            float qZ = centers[centerIdx + 2] - camZ;

            distances[quadIdx] = qX * qX + qY * qY + qZ * qZ;
        }

        return MergeSort.mergeSort(distances);
    }

    /**
     * Orders quads that all share the normal {@code (nx, ny, nz)}, up to a sign given by {@code flipped}. Because
     * such quads only ever occlude each other along that normal, the resulting order is correct from any camera
     * position, and can be computed once during meshing.
     * <p>
     * Distance is measured by projecting onto the shared normal from a camera placed far along it. Quads facing the
     * camera are ordered back to front and are drawn before the quads facing away, which are ordered front to back.
     */
    public static int[] sortNormalRelative(float[] centers, int quadCount, float nx, float ny, float nz, BitSet flipped) {
        float camX = centers[0] + nx * FAKE_STATIC_CAMERA_OFFSET;
        float camY = centers[1] + ny * FAKE_STATIC_CAMERA_OFFSET;
        float camZ = centers[2] + nz * FAKE_STATIC_CAMERA_OFFSET;

        float[] distances = new float[quadCount];

        for (int quadIdx = 0; quadIdx < quadCount; quadIdx++) {
            int centerIdx = quadIdx * 3;

            float qX = centers[centerIdx] - camX;
            float qY = centers[centerIdx + 1] - camY;
            float qZ = centers[centerIdx + 2] - camZ;

            distances[quadIdx] = (nx * qX + ny * qY + nz * qZ) * (flipped.get(quadIdx) ? 1 : -1);
        }

        return MergeSort.mergeSort(distances);
    }

    public static int @Nullable [] order(SortState state, float camX, float camY, float camZ) {
        if (state instanceof SortState.Static staticState) {
            return staticState.order();
        }

        if (state instanceof SortState.Dynamic dynamic) {
            return sortByDistance(dynamic.centers(), dynamic.quadCount(), camX, camY, camZ);
        }

        if (state instanceof PartitionTree tree) {
            return tree.order(camX, camY, camZ);
        }

        return null;
    }

    /**
     * Decides the cheapest sort a section's translucent geometry can get away with. First match wins:
     * <ol>
     *     <li>no quads, or none that cover any pixels</li>
     *     <li>everything in one plane</li>
     *     <li>every quad flat on the section's bounding box, facing outward</li>
     *     <li>all normals parallel, so one order works from everywhere</li>
     *     <li>otherwise the order depends on the camera</li>
     * </ol>
     */
    public static SortState analyze(QuadSet quads) {
        int quadCount = quads.count();

        if (quadCount == 0) {
            return SortState.NONE;
        }

        int reference = firstNonDegenerateQuad(quads);

        if (reference < 0) {
            // every quad is degenerate, so none of them cover a pixel and no order can be wrong
            return SortState.NONE;
        }

        if (allCoplanarWith(quads, reference)) {
            // quads in one plane never occlude each other
            return SortState.NONE;
        }

        if (allOutwardOnBoundingBox(quads)) {
            // each quad is on a different face of the section's bounding box, facing away from the inside, so no
            // two of them can ever overlap on screen
            return SortState.NONE;
        }

        float nx = quads.normalX(reference), ny = quads.normalY(reference), nz = quads.normalZ(reference);

        BitSet flipped = new BitSet(quadCount);
        boolean allParallel = true;

        for (int quadIdx = 0; quadIdx < quadCount; quadIdx++) {
            if (quads.isDegenerate(quadIdx)) continue;

            float dot = quads.normalDot(quadIdx, reference);

            if (Math.abs(dot) < PARALLEL_THRESHOLD) {
                allParallel = false;
                break;
            }

            if (dot < 0) {
                // this quad faces the opposite way to the reference
                flipped.set(quadIdx);
            }
        }

        if (allParallel) {
            // parallel but spread over several planes: the order cannot change, so sort once and keep only the result
            return new SortState.Static(sortNormalRelative(quads.centers(), quadCount, nx, ny, nz, flipped));
        }

        if (quadCount <= MAX_TOPO_QUADS) {
            int[] order = topologicalOrder(quads);

            if (order != null) {
                return new SortState.Static(order);
            }
        }

        PartitionTree tree = PartitionTree.build(quads);
        if (tree != null) return tree;

        return new SortState.Dynamic(quads.centers());
    }

    /**
     * The first quad with a non-zero normal, or -1 if every quad is degenerate
     */
    private static int firstNonDegenerateQuad(QuadSet quads) {
        float[] normals = quads.normals();

        for (int quadIdx = 0; quadIdx < quads.count(); quadIdx++) {
            int normalOffset = quadIdx * 3;

            if (normals[normalOffset] != 0 || normals[normalOffset + 1] != 0 || normals[normalOffset + 2] != 0) {
                return quadIdx;
            }
        }

        return -1;
    }

    private static boolean allCoplanarWith(QuadSet quads, int reference) {
        for (int quadIdx = 0; quadIdx < quads.count(); quadIdx++) {
            if (!quads.isCoplanar(reference, quadIdx)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Whether every quad lies flat on the outer shell of the union of all their bounds, facing outward. Faces may be
     * missing (a block against an opaque neighbour), but an inward or interior face fails, because it can be seen
     * through the others.
     */
    private static boolean allOutwardOnBoundingBox(QuadSet quads) {
        QuadSet.Bounds box = quads.union();

        for (int quadIdx = 0; quadIdx < quads.count(); quadIdx++) {
            ModelQuadFacing facing = quads.facing(quadIdx);

            if (!facing.isDirection()) {
                // A slanted or degenerate quad is not flat on any face of the box
                return false;
            }

            float nx = facing.getStepX(), ny = facing.getStepY(), nz = facing.getStepZ();

            // the largest projection is the far side of the box, and a quad lying in a plane has no thickness at all
            float quadMin = quads.minDot(quadIdx, nx, ny, nz);
            float quadMax = quads.maxDot(quadIdx, nx, ny, nz);

            if (Math.abs(quadMax - quadMin) > QuadSet.EPSILON) {
                return false;
            }

            if (Math.abs(quadMax - box.maxDot(nx, ny, nz)) > QuadSet.EPSILON) {
                return false;
            }
        }

        return true;
    }

    public static final int MAX_TOPO_QUADS = 2048;

    /** returns null if the relation has a cycle */
    public static int @Nullable [] topologicalOrder(QuadSet quads) {
        int[] all = new int[quads.count()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return topologicalOrder(quads, all);
    }

    /**
     * The group's quads in an order correct from every camera, or null if their visibility relation has a cycle.
     * A depth-first search over implicit edges: an acyclic group costs every pair, a cyclic one only until the first
     * back edge, which dense cyclic geometry produces almost immediately.
     */
    static int @Nullable [] topologicalOrder(QuadSet quads, int[] group) {
        int n = group.length;

        // 0 = unvisited, 1 = on the stack, 2 = finished
        byte[] state = new byte[n];
        int[] stack = new int[n], next = new int[n];
        int[] out = new int[n];
        int finished = 0;

        for (int root = n - 1; root >= 0; root--) {
            if (state[root] != 0) continue;

            int depth = 0;
            stack[0] = root;
            next[0] = n - 1;
            state[root] = 1;

            while (depth >= 0) {
                int current = stack[depth];
                int candidate = next[depth];

                // advance to the next quad that must be drawn after the current one
                while (candidate >= 0 && (state[candidate] == 2 || candidate == current
                        || !quads.isSeenThrough(group[current], group[candidate]))) {
                    candidate--;
                }

                if (candidate < 0) {
                    state[current] = 2;
                    out[n - 1 - finished++] = group[current]; // reverse postorder: predecessors first
                    depth--;
                    continue;
                }

                if (state[candidate] == 1) {
                    return null; // back edge: a cycle
                }

                next[depth] = candidate - 1;
                depth++;
                stack[depth] = candidate;
                next[depth] = n - 1;
                state[candidate] = 1;
            }
        }

        return out;
    }
}
