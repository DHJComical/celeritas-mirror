package org.taumc.celeritas.impl.render.terrain.compile.task;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.impl.extensions.TessellatorExtension;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.occlusion.ChunkOcclusionDataBuilder;
import org.taumc.celeritas.impl.render.util.Direction;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;
import org.taumc.celeritas.mixin.core.TessellatorAccessor;

import java.util.*;

public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final int buildTime;
    private final Vector3d camera;
    private final ChunkRenderContext renderContext;

    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext context, int time, Vector3d camera) {
        this.render = render;
        this.buildTime = time;
        this.camera = camera;
        this.renderContext = context;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        PrimitiveChunkBuildContext buildContext = (PrimitiveChunkBuildContext)context;
        var renderData = new PrimitiveBuiltRenderSectionData();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        Vector3i blockPos = new Vector3i(minX, minY, minZ);

        var world = buildContext.world;
        var chunk = world.getChunk(this.render.getChunkX(), this.render.getChunkZ());
        var renderBlocks = new BlockRenderManager(world);
        var tesselator = Tessellator.INSTANCE;
        var extTesselator = (TessellatorExtension)tesselator;

        TessellatorAccessor.celeritas$setTriangleMode(false);
        tesselator.setOffset(-this.render.getOriginX(), -this.render.getOriginY(), -this.render.getOriginZ());
        Chunk.hasSkyLight = false;

        // Beta is insane and updates the matrix inside the tessellation logic
        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.set(x, y, z);

                        var blockId = chunk.getBlockId(x & 15, y, z & 15);

                        if (blockId == 0) {
                            continue;
                        }

                        Block block = Block.BLOCKS[blockId];

                        if (Block.BLOCKS_WITH_ENTITY[blockId]) {
                            BlockEntity tileEntity = chunk.getBlockEntity(x & 15, y, z & 15);
                            if (BlockEntityRenderDispatcher.INSTANCE.hasRenderer(tileEntity)) {
                                renderData.globalBlockEntities.add(tileEntity);
                            }
                        }

                        int pass = block.getRenderLayer();

                        tesselator.startQuads();
                        renderBlocks.render(block, x, y, z);
                        buildContext.copyRawBuffer(extTesselator.celeritas$getRawBuffer(), extTesselator.celeritas$getVertexCount(), buffers, buffers.getRenderPassConfiguration().getMaterialForRenderType(pass));
                        extTesselator.celeritas$reset();

                        if (Block.BLOCKS_OPAQUE[blockId]) {
                            occluder.markClosed(blockPos);
                        }
                    }
                }
            }
        } finally {
            tesselator.setOffset(0, 0, 0);
            TessellatorAccessor.celeritas$setTriangleMode(true);
        }


        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,(float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.hasBlockGeometry = true;
        }

        renderData.hasSkyLight = Chunk.hasSkyLight;

        encodeVisibilityData(occluder, renderData);

        return new ChunkBuildOutput(this.render, renderData, meshes, this.buildTime);
    }

    private static final Direction[] FACINGS = new Direction[GraphDirection.COUNT];

    static {
        FACINGS[GraphDirection.UP] = Direction.UP;
        FACINGS[GraphDirection.DOWN] = Direction.DOWN;
        FACINGS[GraphDirection.WEST] = Direction.WEST;
        FACINGS[GraphDirection.EAST] = Direction.EAST;
        FACINGS[GraphDirection.NORTH] = Direction.NORTH;
        FACINGS[GraphDirection.SOUTH] = Direction.SOUTH;
    }

    private static void encodeVisibilityData(ChunkOcclusionDataBuilder occluder, BuiltRenderSectionData renderData) {
        var data = occluder.build();
        renderData.visibilityData = VisibilityEncoding.encode((from, to) -> data.isVisibleThrough(FACINGS[from], FACINGS[to]));
    }

}
