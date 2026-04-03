package org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.modern.render.chunk.ContextAwareChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

public class SpecialBlockRenderer {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    public void voxelizeLightBlock(BlockPos relativeBlockPos, BlockState blockState, ChunkBuildBuffers buffers) {
        var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(RenderType.cutout());

        if (buffers.get(material).getEncoder() instanceof ContextAwareChunkVertexEncoder encoder) {
            int relX = relativeBlockPos.getX();
            int relY = relativeBlockPos.getY();
            int relZ = relativeBlockPos.getZ();

            ChunkModelBuilder buildBuffers = buffers.get(material);
            encoder.prepareToVoxelizeLight(blockState);
            for (int i = 0; i < 4; i++) {
                vertices[i].x = (float) ((relX & 15)) + 0.25f;
                vertices[i].y = (float) ((relY & 15)) + 0.25f;
                vertices[i].z = (float) ((relZ & 15)) + 0.25f;
                vertices[i].u = 0;
                vertices[i].v = 0;
                vertices[i].color = 0;
                vertices[i].light = blockState.getLightEmission() << 4 | blockState.getLightEmission() << 20;
            }
            buildBuffers.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(vertices, material);
            encoder.finishRenderingBlock();
        }
    }
}
