package org.taumc.celeritas.impl.render.terrain.compile.task;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.world.WorldRegion;
import net.minecraft.world.chunk.WorldChunk;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11C;
import org.taumc.celeritas.impl.extensions.TessellatorExtension;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;
import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.occlusion.ChunkOcclusionDataBuilder;
import org.taumc.celeritas.impl.render.util.Direction;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;
import org.taumc.celeritas.mixin.core.TessellatorAccessor;

import java.nio.IntBuffer;

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

        var world = buildContext.world;
        var chunk = world.getChunkAt(this.render.getChunkX(), this.render.getChunkZ());
        var tesselator = buildContext.tesselator;

        //? if <1.8 {
        Vector3i blockPos = new Vector3i(minX, minY, minZ);
        var region = new WorldRegion(world, minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1 /*? if >=1.7.10 {*//*, 1*//*?}*/);
        var renderBlocks = new net.minecraft.client.render.BlockRenderer(region);
        var extTesselator = (TessellatorExtension)tesselator;
        //? if <1.7
        TessellatorAccessor.celeritas$setTriangleMode(false);
        WorldChunk.hasSkyLight = false;
        //?} else {
        /*var blockPos = new net.minecraft.util.math.BlockPos.Mutable(minX, minY, minZ);
        var region = new WorldRegion(world, new net.minecraft.util.math.BlockPos(minX, minY, minZ), new net.minecraft.util.math.BlockPos(maxX, maxY, maxZ), 1);
        var renderBlocks = net.minecraft.client.Minecraft.getInstance().getBlockRenderDispatcher();
        *///?}

        tesselator.offset(-this.render.getOriginX(), -this.render.getOriginY(), -this.render.getOriginZ());


        // Beta is insane and updates the matrix inside the tessellation logic
        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.set(x, y, z);

                        //? if <1.7 {
                        var blockId = chunk.getBlockAt(x & 15, y, z & 15);

                        if (blockId == 0) {
                            continue;
                        }

                        Block block = Block.BY_ID[blockId];
                        //?} else if <1.8 {
                        /*Block block = chunk.getBlockAt(x & 15, y, z & 15);

                        if (block == net.minecraft.block.Blocks.AIR) {
                            continue;
                        }
                        *///?} else {
                        /*var blockState = chunk.getBlockState(blockPos);
                        var block = blockState.getBlock();

                        if (block == net.minecraft.block.Blocks.AIR) {
                            continue;
                        }
                        *///?}

                        //? if <1.2.5 {
                        /*boolean hasBlockEntity = Block.HAS_BLOCK_ENTITY[blockId];
                        *///?} else
                        boolean hasBlockEntity = block.hasBlockEntity();

                        if (hasBlockEntity) {
                            //? if <1.8 {
                            BlockEntity tileEntity = chunk.getBlockEntityAt(x & 15, y, z & 15);
                            //?} else
                            /*BlockEntity tileEntity = chunk.getBlockEntity(blockPos, WorldChunk.BlockEntityCreationType.CHECK);*/
                            if (BlockEntityRenderDispatcher.INSTANCE.getRenderer(tileEntity) != null) {
                                renderData.globalBlockEntities.add(tileEntity);
                            }
                        }

                        var pass = block.getRenderLayer();

                        //? if <1.8 {
                        tesselator.start();
                        renderBlocks.tessellateBlock(block, x, y, z);
                        buildContext.copyRawBuffer(IntBuffer.wrap(extTesselator.celeritas$getRawBuffer()), extTesselator.celeritas$getVertexCount(), buffers, buffers.getRenderPassConfiguration().getMaterialForRenderType(pass));
                        extTesselator.celeritas$reset();
                        //?} else {
                        /*tesselator.begin(GL11C.GL_QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK);
                        renderBlocks.tessellate(blockState, blockPos, region, tesselator);
                        tesselator.end();
                        buildContext.copyRawBuffer(tesselator.getBuffer().asIntBuffer(), tesselator.getVertexCount(), buffers, buffers.getRenderPassConfiguration().getMaterialForRenderType(pass));
                        tesselator.clear();
                        *///?}

                        //? if <1.7 {
                        boolean opaque = Block.IS_OPAQUE[blockId];
                        //?} else
                        /*boolean opaque = block.isOpaqueCube();*/

                        if (opaque) {
                            //? if >=1.8 {
                            /*occluder.markClosed(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                            *///?} else
                            occluder.markClosed(blockPos);
                        }
                    }
                }
            }
        } finally {
            tesselator.offset(0, 0, 0);
            //? if <1.7
            TessellatorAccessor.celeritas$setTriangleMode(true);
        }


        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,(float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.hasBlockGeometry = true;
        }

        //? if <1.8
        renderData.hasSkyLight = WorldChunk.hasSkyLight;

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
