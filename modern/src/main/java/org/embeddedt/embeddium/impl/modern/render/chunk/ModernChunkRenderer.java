package org.embeddedt.embeddium.impl.modern.render.chunk;

import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderListIterable;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;

public class ModernChunkRenderer extends DefaultChunkRenderer {
    public ModernChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        super(device, renderPassConfiguration);
    }

    /*
    @Override
    protected GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        if (options.vertexType() == ChunkMeshFormats.VANILLA_LIKE) {
            return CoreShaderSupport.createCeleritasCoreShader(options);
        } else {
            return super.createShader(path, options);
        }
    }
     */

    @Override
    protected boolean useBlockFaceCulling() {
        return Celeritas.options().performance.useBlockFaceCulling /*? if shaders {*/ && !net.irisshaders.iris.shadows.ShadowRenderingState.areShadowsCurrentlyBeingRendered() /*?}*/;
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera) {
        super.render(matrices, commandList, renderLists, renderPass, camera);
    }
}
