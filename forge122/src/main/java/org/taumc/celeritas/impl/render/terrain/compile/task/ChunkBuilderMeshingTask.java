package org.taumc.celeritas.impl.render.terrain.compile.task;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBufferSorter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl3.opengl.GL11C;
import org.lwjgl3.system.MemoryUtil;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.VintageRenderSectionBuiltInfo;

import java.nio.ByteBuffer;
import java.util.*;

public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final int buildTime;
    private final Vector3d camera;
    private final BufferBuilder[] worldRenderers = new BufferBuilder[BlockRenderLayer.values().length];

    private static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();

    public ChunkBuilderMeshingTask(RenderSection render, int time, Vector3d camera) {
        this.render = render;
        this.buildTime = time;
        this.camera = camera;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        VintageChunkBuildContext buildContext = (VintageChunkBuildContext)context;
        ContextBundle<RenderSection> renderData = new ContextBundle<>(RenderSection.class);
        initializeContextBundle(renderData);
        VisGraph occluder = new VisGraph();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);
        BlockPos.MutableBlockPos modelOffset = new BlockPos.MutableBlockPos();

        // TODO
        IBlockAccess slice = Minecraft.getMinecraft().world;

        var dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.setPos(x, y, z);

                        IBlockState blockState = slice.getBlockState(blockPos);
                        var block = blockState.getBlock();

                        if (block == Blocks.AIR) {
                            continue;
                        }

                        for (BlockRenderLayer layer : LAYERS) {
                            if (block.canRenderInLayer(blockState, layer)) {
                                ForgeHooksClient.setRenderLayer(layer);
                                var buffer = this.getBufferBuilderForLayer(layer);
                                dispatcher.renderBlock(blockState, blockPos, slice, buffer);
                            }
                        }

                        if (blockState.isOpaqueCube()) {
                            occluder.setOpaqueCube(blockPos);
                        }
                    }
                }
            }
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getCrashReport(), slice, blockPos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.makeCrashReport(ex, "Encountered exception while building chunk meshes"), slice, blockPos);
        }

        this.convertVanillaDataToCeleritasData(buffers);

        Map<TerrainRenderPass, BuiltSectionMeshParts> meshes = new Reference2ReferenceOpenHashMap<>();

        for (TerrainRenderPass pass : buffers.getRenderPassConfiguration().renderPasses()) {
            BuiltSectionMeshParts mesh = buffers.createMesh(pass);

            if (mesh != null) {
                if(pass.isSorted()) {
                    Objects.requireNonNull(mesh.getIndexData());
                    ChunkBufferSorter.sort(
                            mesh.getIndexData(),
                            mesh.getSortState(),
                            (float)camera.x - minX,
                            (float)camera.y - minY,
                            (float)camera.z - minZ
                    );
                }
                meshes.put(pass, mesh);
                renderData.setContext(VintageRenderSectionBuiltInfo.HAS_BLOCK_GEOMETRY, true);
            }
        }

        encodeVisibilityData(occluder, renderData);

        return new ChunkBuildOutput(this.render, renderData, meshes, this.buildTime);
    }

    private ReportedException fillCrashInfo(CrashReport report, IBlockAccess slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.makeCategory("Block being rendered");

        IBlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.addBlockInfo(crashReportSection, pos, state);

        crashReportSection.addCrashSection("Chunk section", this.render);
        /*
        if (this.renderContext != null) {
            crashReportSection.addCrashSection("Render context volume", this.renderContext.getVolume());
        }

         */

        return new ReportedException(report);
    }

    private BufferBuilder getBufferBuilderForLayer(BlockRenderLayer layer) {
        var builder = this.worldRenderers[layer.ordinal()];
        if (builder != null) {
            return builder;
        }
        builder = new BufferBuilder(131072);
        builder.begin(GL11C.GL_QUADS, DefaultVertexFormats.BLOCK);
        this.worldRenderers[layer.ordinal()] = builder;
        return builder;
    }

    private void convertVanillaDataToCeleritasData(ChunkBuildBuffers buffers) {
        var renderers = this.worldRenderers;
        for (int i = 0; i < renderers.length; i++) {
            var bufferBuilder = renderers[i];
            if (bufferBuilder != null) {
                bufferBuilder.finishDrawing();
                ByteBuffer rawBuffer = bufferBuilder.getByteBuffer();
                var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(LAYERS[i]);
                copyBlockData(rawBuffer, buffers.get(material), material);
            }
        }
    }

    private void copyBlockData(ByteBuffer source, ChunkModelBuilder dest, Material material) {
        int vsize = DefaultVertexFormats.BLOCK.getSize();
        int numQuads = source.limit() / (vsize * 4);
        long ptr = MemoryUtil.memAddress(source);
        var quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
        for(int q = 0; q < numQuads; q++) {
            for(int v = 0; v < 4; v++) {
                var vertex = quad[v];
                vertex.x = MemoryUtil.memGetFloat(ptr);
                vertex.y = MemoryUtil.memGetFloat(ptr + 4);
                vertex.z = MemoryUtil.memGetFloat(ptr + 8);
                vertex.color = MemoryUtil.memGetInt(ptr + 12);
                vertex.u = MemoryUtil.memGetFloat(ptr + 16);
                vertex.v = MemoryUtil.memGetFloat(ptr + 20);
                vertex.light = MemoryUtil.memGetInt(ptr + 24);
                ptr += vsize;
            }
            dest.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(quad, material);
        }
    }

    private static final EnumFacing[] FACINGS = new EnumFacing[GraphDirection.COUNT];

    static {
        FACINGS[GraphDirection.UP] = EnumFacing.UP;
        FACINGS[GraphDirection.DOWN] = EnumFacing.DOWN;
        FACINGS[GraphDirection.WEST] = EnumFacing.WEST;
        FACINGS[GraphDirection.EAST] = EnumFacing.EAST;
        FACINGS[GraphDirection.NORTH] = EnumFacing.NORTH;
        FACINGS[GraphDirection.SOUTH] = EnumFacing.SOUTH;
    }

    private static void initializeContextBundle(ContextBundle<RenderSection> renderData) {
        renderData.setContext(VintageRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES, new ArrayList<>());
        renderData.setContext(VintageRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES, new ArrayList<>());
        renderData.setContext(VintageRenderSectionBuiltInfo.ANIMATED_SPRITES, new ArrayList<>());
    }

    private static void encodeVisibilityData(VisGraph occluder, ContextBundle<RenderSection> renderData) {
        var data = occluder.computeVisibility();
        renderData.setContext(RenderSection.VISIBILITY_DATA, VisibilityEncoding.encode((from, to) -> data.isVisible(FACINGS[from], FACINGS[to])));

        renderData.mapContext(VintageRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(VintageRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(VintageRenderSectionBuiltInfo.ANIMATED_SPRITES, List::copyOf);
    }

}
