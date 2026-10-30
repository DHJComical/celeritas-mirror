package org.embeddedt.embeddium.impl.render.chunk.terrain.sampling;

import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;

/**
 * How chunk shaders sample the block atlas. Platform code must bind the atlas with NEAREST filtering for
 * {@link Native}, and with LINEAR_MIPMAP_LINEAR/LINEAR filtering for the other variants.
 */
public sealed interface TerrainSampling {
    TerrainSampling NATIVE = new Native();

    enum Filter {
        /** Nearest filtering emulated with a LINEAR sampler, which antialiases texel edges. */
        NEAREST_EMULATED,
        /** Rotated-grid supersampling, blended with {@link #NEAREST_EMULATED} up close. */
        RGSS
    }

    void addDefines(ShaderConstants.Builder constants);

    default void delete() {
    }

    /**
     * A plain texture lookup, filtered by the sampler.
     */
    record Native() implements TerrainSampling {
        @Override
        public void addDefines(ShaderConstants.Builder constants) {
        }
    }

    /**
     * Every sprite in the atlas is surrounded by at least {@code padding} texels that repeat its edge.
     */
    record Padded(Filter filter, int atlasWidth, int atlasHeight, int padding) implements TerrainSampling {
        public Padded {
            if (atlasWidth <= 0 || atlasHeight <= 0 || padding <= 0) {
                throw new IllegalArgumentException("Atlas size and padding must be positive");
            }
        }

        @Override
        public void addDefines(ShaderConstants.Builder constants) {
            addFilterDefines(constants, this.filter, this.atlasWidth, this.atlasHeight);
            constants.add("CELERITAS_ATLAS_PADDING", this.padding + ".0");
        }
    }

    /**
     * Taps are clamped using an {@link AtlasEdgeMask}. The platform's pipeline state must bind the mask's texture to
     * the unit it assigns to {@code ChunkShaderTextureSlot.ATLAS_EDGE_MASK}, together with sampler 0, and call
     * {@link AtlasEdgeMask#upload()} while it is bound.
     */
    record EdgeMasked(Filter filter, AtlasEdgeMask mask) implements TerrainSampling {
        @Override
        public void addDefines(ShaderConstants.Builder constants) {
            addFilterDefines(constants, this.filter, this.mask.atlasWidth(), this.mask.atlasHeight());
            constants.add("CELERITAS_ATLAS_EDGE_MASK");
            constants.add("CELERITAS_ATLAS_MAX_MIP", this.mask.mipLevels() + ".0");
            constants.add("CELERITAS_ATLAS_CELL_SIZE", this.mask.cellSize() + ".0");
            constants.add("CELERITAS_ATLAS_EDGE_MASK_CELLS", "ivec2(" + this.mask.cellsX() + ", " + this.mask.cellsY() + ")");
        }

        @Override
        public void delete() {
            this.mask.delete();
        }
    }

    private static void addFilterDefines(ShaderConstants.Builder constants, Filter filter, int atlasWidth, int atlasHeight) {
        constants.add(filter == Filter.RGSS ? "CELERITAS_TEX_FILTER_RGSS" : "CELERITAS_TEX_FILTER_NEAREST_EMULATED");
        constants.add("CELERITAS_ATLAS_SIZE", "vec2(" + atlasWidth + ".0, " + atlasHeight + ".0)");
    }
}
