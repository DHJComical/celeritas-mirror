package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.gl.array.GlVertexArray;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.tessellation.*;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderListIterable;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.BatchAssembler;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import java.util.Iterator;

public abstract class DefaultChunkRenderer extends ShaderChunkRenderer {
    private final Reference2ReferenceMap<ChunkPrimitiveType, SharedQuadIndexBuffer> sharedIndexBuffers;

    private TerrainRenderPass currentRenderPass;
    private GlVertexFormat currentVertexFormat;

    public DefaultChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        super(device, renderPassConfiguration);

        this.sharedIndexBuffers = new Reference2ReferenceOpenHashMap<>();
    }

    protected boolean useBlockFaceCulling() {
        return true;
    }

    protected final SharedQuadIndexBuffer getSharedIndexBuffer(ChunkPrimitiveType type, CommandList commandList) {
        var buffer = this.sharedIndexBuffers.get(type);
        if (buffer == null) {
            buffer = new SharedQuadIndexBuffer(commandList, type);
            this.sharedIndexBuffers.put(type, buffer);
        }
        return buffer;
    }

    protected abstract void configureShaderInterface(ChunkShaderInterface shader);

    @Override
    public void render(ChunkRenderMatrices matrices,
                       CommandList commandList,
                       ChunkRenderListIterable renderLists,
                       TerrainRenderPass renderPass,
                       CameraTransform occlusionCamera,
                       CameraTransform camera) {
        if (!renderLists.hasPass(renderPass)) {
            return;
        }

        this.begin(renderPass);

        // If there is no active program, shader compilation probably failed, and we can't render anything.
        if (this.activeProgram != null) {
            boolean useBlockFaceCulling = this.useBlockFaceCulling();

            GLDebug.pushGroup(770, renderPass.name() + " terrain pass");

            ChunkShaderInterface shader = this.activeProgram.getInterface();
            shader.setProjectionMatrix(matrices.projection());
            shader.setModelViewMatrix(matrices.modelView());

            var primitiveType = shader.getPrimitiveType();

            Iterator<ChunkRenderList> iterator = renderLists.iterator(renderPass.isReverseOrder());

            this.currentRenderPass = renderPass;
            this.currentVertexFormat = this.renderPassConfiguration.getVertexTypeForPass(this.currentRenderPass).getVertexFormat();

            this.configureShaderInterface(shader);

            long timestamp = System.nanoTime();

            useBlockFaceCulling = useBlockFaceCulling && !renderPass.isSorted();
            var cacheParams = new SectionRenderDataStorage.BatchCacheParams(useBlockFaceCulling);

            int numRebuilds = 0;

            while (iterator.hasNext()) {
                ChunkRenderList renderList = iterator.next();

                var region = renderList.getRegion();
                var storage = region.getStorage(renderPass);

                if (storage == null) {
                    continue;
                }

                var cached = storage.getCachedMultiDrawBatch(cacheParams);

                if (cached == null || !cached.isValidFor(renderList.getSectionsWithGeometry(), renderList.getSectionsWithGeometryCount(),
                        occlusionCamera.intX, occlusionCamera.intY, occlusionCamera.intZ)) {
                    numRebuilds++;
                    cached = BatchAssembler.createCachedBatch(region, storage, renderList, occlusionCamera, renderPass,
                            useBlockFaceCulling, commandList);

                    storage.storeCachedMultiDrawBatch(cacheParams, cached);
                }

                var batch = cached.getBatch();

                if (batch != null && !batch.isEmpty()) {
                    if (!renderPass.isSorted()) {
                        getSharedIndexBuffer(renderPassConfiguration.getPrimitiveTypeForPass(renderPass), commandList)
                                .ensureCapacity(commandList, batch.getIndexBufferSize());
                    }

                    var tessellation = this.prepareTessellation(commandList, region);

                    setModelMatrixUniforms(shader, region, camera);
                    shader.setSectionAges(timestamp, region.getSectionLoadTimes());
                    batch.execute(commandList, tessellation, primitiveType);
                }
            }

            this.currentVertexFormat = null;
            this.currentRenderPass = null;

            GLDebug.popGroup();
        }

        this.end(renderPass);
    }

    private static void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, CameraTransform camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        shader.setRegionOffset(x, y, z);
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion region) {
        var resources = region.getResources(this.currentVertexFormat);
        var tessellation = this.currentRenderPass.isSorted() ? resources.getIndexedTessellation() : resources.getTessellation();

        if (tessellation == null) {
            tessellation = this.createRegionTessellation(commandList, resources);
            if (this.currentRenderPass.isSorted()) {
                resources.updateIndexedTessellation(commandList, tessellation);
            } else {
                resources.updateTessellation(commandList, tessellation);
            }
        }

        return tessellation;
    }

    protected TessellationBinding[] makeTessellationBindingArray(CommandList commandList, RenderRegion.DeviceResources resources) {
        return new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(resources.getVertexBuffer(), this.currentVertexFormat),
                TessellationBinding.forElementBuffer(this.currentRenderPass.isSorted() ? resources.getIndexBuffer() : this.getSharedIndexBuffer(this.renderPassConfiguration.getPrimitiveTypeForPass(this.currentRenderPass), commandList).getBufferObject())
        };
    }

    protected GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.DeviceResources resources) {
        var bindings = makeTessellationBindingArray(commandList, resources);
        GlVertexArrayTessellation tessellation = new GlVertexArrayTessellation(new GlVertexArray(), bindings);
        tessellation.init(commandList);

        return tessellation;
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.sharedIndexBuffers.values().forEach(buffer -> buffer.delete(commandList));
    }
}
