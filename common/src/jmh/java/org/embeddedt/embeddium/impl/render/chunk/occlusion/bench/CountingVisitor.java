package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

import org.embeddedt.embeddium.impl.render.chunk.occlusion.OcclusionCuller;

/**
 * Trivial visitor that folds arguments into a checksum and counts visits. Isolates traversal cost from the
 * real collector's memory behaviour; do not read its number as "the cost of culling".
 */
public final class CountingVisitor implements OcclusionCuller.Visitor {
    public int visited;
    public int visible;
    public long checksum;

    @Override
    public void visit(int latticeIndex, int regionId, int sectionIndex, int meta, boolean visible) {
        this.visited++;

        if (visible) {
            this.visible++;
        }

        this.checksum = (this.checksum * 31) + latticeIndex + regionId + sectionIndex + meta;
    }

    public long fold() {
        return this.checksum + this.visited + this.visible;
    }

    @Override
    public String toString() {
        return "visited=" + this.visited + ", visible=" + this.visible;
    }
}
