package org.embeddedt.embeddium.impl.render.chunk.occlusion.bench;

/** Terrain shapes for the bench world. */
public enum WorldType {
    /** No blocks. Traversal worst case (nothing to cull); useless for drawing benchmarks. */
    EMPTY,
    /** Sine heightmap with caves and water. Closest to a real overworld; the default. */
    SURFACE,
    /** Level ground at the SURFACE world's height at the origin, with caves beneath. Nothing above ground occludes anything. */
    FLAT,
    /**
     * Level solid ground, no caves and no cutouts. The search cannot enter the ground, so everything it reaches
     * is the one surface layer and the raster has nothing to cull: the worst case for its cost.
     */
    PLAINS,
    /** SURFACE heightmap over solid ground, no caves and no cutouts. Hills hide only what lies behind them. */
    HILLS,
    /** PLAINS with a plateau beyond {@code VoxelWorld.CLIFF_RADIUS}. The cliff hides the plateau surface, from beyond the floor's reach. */
    CLIFF,
    /** Stone to the top of the world with caves. Heavily occluded. */
    CAVES
}
