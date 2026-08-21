package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

/** Terrain shapes for the bench world. */
public enum WorldType {
    /** No blocks. Traversal worst case (nothing to cull); useless for drawing benchmarks. */
    EMPTY,
    /** Sine heightmap with caves and water. Closest to a real overworld; the default. */
    SURFACE,
    /** Stone to the top of the world with caves. Heavily occluded. */
    CAVES
}
