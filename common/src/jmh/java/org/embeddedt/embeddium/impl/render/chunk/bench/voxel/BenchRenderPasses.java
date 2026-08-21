package org.embeddedt.embeddium.impl.render.chunk.bench.voxel;

import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;

import java.util.List;
import java.util.Map;

/**
 * Solid, cutout and sorted-translucent passes for the bench world, mirroring what every platform defines.
 * Three passes so the assembler's sorted/unsorted dispatch and multi-storage region walk are both exercised.
 * {@link String} keys stand in for the platform's render-type token, which nothing here looks up.
 */
public final class BenchRenderPasses {
    public static final TerrainRenderPass SOLID;
    public static final TerrainRenderPass CUTOUT_MIPPED;
    public static final TerrainRenderPass TRANSLUCENT;

    public static final Material SOLID_MATERIAL;
    public static final Material CUTOUT_MIPPED_MATERIAL;
    public static final Material TRANSLUCENT_MATERIAL;

    private BenchRenderPasses() {
    }

    private static TerrainRenderPass.TerrainRenderPassBuilder builder() {
        // No GL state: the benchmark never draws.
        return TerrainRenderPass.builder()
                .pipelineState(TerrainRenderPass.PipelineState.DEFAULT)
                .vertexType(ChunkMeshFormats.COMPACT)
                .primitiveType(QuadPrimitiveType.TRIANGULATED);
    }

    static {
        SOLID = builder()
                .name("bench_solid")
                .fragmentDiscard(false)
                .useReverseOrder(false)
                .build();

        CUTOUT_MIPPED = builder()
                .name("bench_cutout_mipped")
                .fragmentDiscard(true)
                .useReverseOrder(false)
                .build();

        TRANSLUCENT = builder()
                .name("bench_translucent")
                .fragmentDiscard(false)
                .useReverseOrder(true)
                .useTranslucencySorting(true)
                .build();

        SOLID_MATERIAL = new Material(SOLID, AlphaCutoffParameter.ZERO, true);
        CUTOUT_MIPPED_MATERIAL = new Material(CUTOUT_MIPPED, AlphaCutoffParameter.ONE_TENTH, true);
        TRANSLUCENT_MATERIAL = new Material(TRANSLUCENT, AlphaCutoffParameter.ZERO, true);
    }

    /** {@return the material a block of this {@link VoxelWorld} type is drawn with} */
    public static Material materialFor(int block) {
        return switch (block) {
            case VoxelWorld.SOLID -> SOLID_MATERIAL;
            case VoxelWorld.CUTOUT -> CUTOUT_MIPPED_MATERIAL;
            case VoxelWorld.WATER -> TRANSLUCENT_MATERIAL;
            default -> throw new IllegalArgumentException("Block " + block + " has no geometry");
        };
    }

    public static RenderPassConfiguration<String> configuration() {
        return new RenderPassConfiguration<>(
                Map.of("solid", SOLID_MATERIAL, "cutout_mipped", CUTOUT_MIPPED_MATERIAL,
                        "translucent", TRANSLUCENT_MATERIAL),
                Map.of("solid", List.of(SOLID, CUTOUT_MIPPED), "translucent", List.of(TRANSLUCENT)),
                SOLID_MATERIAL,
                CUTOUT_MIPPED_MATERIAL,
                TRANSLUCENT_MATERIAL);
    }
}
