package org.taumc.celeritas.impl.render.terrain.compile.task;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.GraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.taumc.celeritas.impl.compat.fluidlogged.FluidloggedCompat;
import org.taumc.celeritas.impl.render.terrain.compile.VintageChunkBuildContext;
import org.taumc.celeritas.impl.render.terrain.compile.VintageRenderSectionBuiltInfo;
import org.taumc.celeritas.impl.world.cloned.ChunkRenderContext;

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

        var slice = buildContext.getWorldSlice();
        slice.copyData(this.renderContext);

        var dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        buildContext.setupTranslation(minX, minY, minZ);

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

                        if (block.hasTileEntity(blockState)) {
                            TileEntity tileEntity = slice.getTileEntity(blockPos);
                            if (tileEntity != null) {
                                TileEntitySpecialRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(tileEntity);

                                if (tesr != null) {
                                    renderData.getContext(tesr.isGlobalRenderer(tileEntity) ? VintageRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES : VintageRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES).add(tileEntity);
                                }
                            }
                        }

                        for (BlockRenderLayer layer : VintageChunkBuildContext.LAYERS) {
                            if (block.canRenderInLayer(blockState, layer)) {
                                ForgeHooksClient.setRenderLayer(layer);
                                var buffer = buildContext.getBufferBuilderForLayer(layer);
                                dispatcher.renderBlock(blockState, blockPos, slice, buffer);
                            }
                        }

                        if (FluidloggedCompat.IS_LOADED) {
                            FluidloggedCompat.renderFluidState(slice, blockPos, blockState, buildContext, dispatcher);
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

        buildContext.convertVanillaDataToCeleritasData(buffers);

        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,(float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.setContext(VintageRenderSectionBuiltInfo.HAS_BLOCK_GEOMETRY, true);
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
        renderData.setContext(VintageRenderSectionBuiltInfo.ANIMATED_SPRITES, new ObjectOpenHashSet<>());
    }

    private static void encodeVisibilityData(VisGraph occluder, ContextBundle<RenderSection> renderData) {
        var data = occluder.computeVisibility();
        renderData.setContext(RenderSection.VISIBILITY_DATA, VisibilityEncoding.encode((from, to) -> data.isVisible(FACINGS[from], FACINGS[to])));

        renderData.mapContext(VintageRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(VintageRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(VintageRenderSectionBuiltInfo.ANIMATED_SPRITES, List::copyOf);
    }

}
