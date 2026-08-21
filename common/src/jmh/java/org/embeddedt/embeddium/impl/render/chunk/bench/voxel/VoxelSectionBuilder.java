package org.embeddedt.embeddium.impl.render.chunk.bench.voxel;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionVisibilityBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

/**
 * Meshes one section of a {@link VoxelWorld} into production's build buffers. The block-iteration loop is the
 * only non-production code on this path; everything downstream of {@code push()} is real.
 */
public final class VoxelSectionBuilder {
    private final ChunkVertexEncoder.Vertex[] quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private static final int FULL_LIGHT = 0x00F0_00F0;

    /** @return the build output, or {@code null} if the section has no geometry */
    public ChunkBuildOutput build(ChunkBuildContext context, RenderSection section, VoxelSlice slice,
                                  float cameraX, float cameraY, float cameraZ) {
        var renderData = new BuiltRenderSectionData();
        var occluder = new SectionVisibilityBuilder();

        var buffers = context.buffers;
        buffers.init(renderData, section.getSectionIndex());

        final int originX = section.getOriginX();
        final int originY = section.getOriginY();
        final int originZ = section.getOriginZ();

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int block = slice.get(x, y, z);

                    if (VoxelWorld.isOpaque(block)) {
                        occluder.markOpaque(x, y, z);
                    }

                    if (!VoxelWorld.isRenderable(block)) {
                        continue;
                    }

                    var material = BenchRenderPasses.materialFor(block);

                    this.emitBlock(buffers.get(material), material, slice, block, x, y, z);
                }
            }
        }

        var meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,
                cameraX - originX, cameraY - originY, cameraZ - originZ);

        renderData.hasBlockGeometry = !meshes.isEmpty();
        renderData.visibilityData = occluder.computeVisibilityEncoding();

        if (meshes.isEmpty()) {
            return null;
        }

        return new ChunkBuildOutput(section, renderData, meshes, 0);
    }

    /** Pushes a quad for each exposed face. */
    private void emitBlock(ChunkModelBuilder builder, Material material, VoxelSlice slice, int block,
                           int x, int y, int z) {
        for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
            int neighbour = slice.get(x + facing.getStepX(), y + facing.getStepY(), z + facing.getStepZ());

            if (VoxelWorld.isFaceVisible(block, neighbour)) {
                this.emitFace(builder, material, facing, x, y, z);
            }
        }
    }

    /** Pushes one unit face at section-relative coordinates. */
    private void emitFace(ChunkModelBuilder builder, Material material, ModelQuadFacing facing,
                          int x, int y, int z) {
        float x0 = x, y0 = y, z0 = z;
        float x1 = x + 1.0f, y1 = y + 1.0f, z1 = z + 1.0f;

        switch (facing) {
            case POS_X -> this.setQuad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
            case NEG_X -> this.setQuad(x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
            case POS_Y -> this.setQuad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
            case NEG_Y -> this.setQuad(x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0);
            case POS_Z -> this.setQuad(x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
            case NEG_Z -> this.setQuad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
            default -> throw new IllegalArgumentException(facing.name());
        }

        int normal = facing.getPackedNormal();

        for (int i = 0; i < 4; i++) {
            var vertex = this.quad[i];

            vertex.u = (i == 0 || i == 1) ? 0.0f : 1.0f;
            vertex.v = (i == 0 || i == 3) ? 0.0f : 1.0f;
            vertex.color = 0xFFFF_FFFF;
            vertex.light = FULL_LIGHT;
            vertex.vanillaNormal = normal;
            vertex.trueNormal = normal;
        }

        builder.getVertexBuffer(facing).push(this.quad, material);
    }

    private void setQuad(float ax, float ay, float az, float bx, float by, float bz,
                         float cx, float cy, float cz, float dx, float dy, float dz) {
        this.set(0, ax, ay, az);
        this.set(1, bx, by, bz);
        this.set(2, cx, cy, cz);
        this.set(3, dx, dy, dz);
    }

    private void set(int index, float x, float y, float z) {
        var vertex = this.quad[index];
        vertex.x = x;
        vertex.y = y;
        vertex.z = z;
    }
}
