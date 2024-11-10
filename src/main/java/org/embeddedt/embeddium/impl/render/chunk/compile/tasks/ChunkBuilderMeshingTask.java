package org.embeddedt.embeddium.impl.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBufferSorter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderCache;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.GeometryCategory;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionInfo;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.18
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
//? if forge && >=1.19
import net.minecraftforge.client.model.data.ModelData;
//? if forge && <1.19
/*import net.minecraftforge.client.ForgeHooksClient;*/
//? if neoforge
/*import net.neoforged.neoforge.client.model.data.ModelData;*/
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;
import org.embeddedt.embeddium.impl.chunk.MeshAppenderRenderer;
//? if forgelike
import org.embeddedt.embeddium.impl.model.ModelDataSnapshotter;
import org.embeddedt.embeddium.impl.model.UnwrappableBakedModel;

import java.util.Map;
import java.util.Objects;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {

    //? if >=1.19 {
    private final RandomSource random = new SingleThreadedRandomSource(42L);
     //?} else
    /*private final Random random = new XoRoShiRoRandom(42L);*/

    private final RenderSection render;
    private final ChunkRenderContext renderContext;

    private final int buildTime;
    private final Vec3 camera;


    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext renderContext, int time, Vec3 camera) {
        this.render = render;
        this.renderContext = renderContext;
        this.buildTime = time;
        this.camera = camera;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext buildContext, CancellationToken cancellationToken) {
        BuiltSectionInfo.Builder renderData = new BuiltSectionInfo.Builder();
        VisGraph occluder = new VisGraph();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        BlockRenderCache cache = buildContext.cache;
        cache.init(this.renderContext);

        WorldSlice slice = cache.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);
        BlockPos.MutableBlockPos modelOffset = new BlockPos.MutableBlockPos();

        BlockRenderContext context = new BlockRenderContext(slice);

        //? if forgelike
        ModelDataSnapshotter.Getter modelDataGetter = slice.getModelDataGetter(this.render.getChunkX(), this.render.getChunkY(), this.render.getChunkZ());

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        BlockState blockState = slice.getBlockState(x, y, z);

                        // Fast path - skip blocks that are air and don't have any custom logic
                        if (blockState.isAir() && blockState.getRenderShape() == RenderShape.INVISIBLE && !WorldUtil.hasBlockEntity(blockState)) {
                            continue;
                        }

                        blockPos.set(x, y, z);
                        modelOffset.set(x & 15, y & 15, z & 15);

                        if (blockState.getRenderShape() == RenderShape.MODEL) {
                            BakedModel model = cache.getBlockModels()
                                .getBlockModel(blockState);
                            //? if forgelike
                            var modelData = model.getModelData(context.localSlice(), blockPos, blockState, modelDataGetter.getModelData(blockPos));

                            long seed = blockState.getSeed(blockPos);
                            random.setSeed(seed);

                            // Embeddium: Ideally we'd do this before the call to getModelData, but that requires an
                            // LVT reordering to move "long seed" further up. We will have to do this in 21.
                            model = UnwrappableBakedModel.unwrapIfPossible(model, random);

                            random.setSeed(seed);

                            //? if forgelike && >=1.19 {
                            for (RenderType layer : model.getRenderTypes(blockState, random, modelData)) {
                                context.update(GeometryCategory.BLOCK, blockPos, modelOffset, blockState, model, seed, layer);
                                context.setModelData(modelData);
                                cache.getBlockRenderer()
                                        .renderModel(context, buffers);
                            }
                            //?} else if forge && <1.19 {
                            /*for (RenderType layer : cache.getBlockRenderLayerCache().forState(blockState)) {
                                //? if >=1.17 {
                                ForgeHooksClient.setRenderType(layer);
                                //?} else
                                /^ForgeHooksClient.setRenderLayer(layer);^/
                                context.update(GeometryCategory.BLOCK, blockPos, modelOffset, blockState, model, seed, layer);
                                context.setModelData(modelData);
                                cache.getBlockRenderer()
                                        .renderModel(context, buffers);
                            }
                            *///?} else {
                            /*context.update(GeometryCategory.BLOCK, blockPos, modelOffset, blockState, model, seed, ItemBlockRenderTypes.getChunkRenderType(blockState));
                            cache.getBlockRenderer()
                                    .renderModel(context, buffers);
                            *///?}
                        }

                        FluidState fluidState = blockState.getFluidState();

                        if (!fluidState.isEmpty()) {
                            context.update(GeometryCategory.FLUID, blockPos, modelOffset, blockState, null, 42L, ItemBlockRenderTypes.getRenderLayer(fluidState));
                            cache.getFluidRenderer().render(context, buffers);
                        }

                        if (WorldUtil.hasBlockEntity(blockState)) {
                            BlockEntity entity = slice.getBlockEntity(blockPos);

                            if (entity != null) {
                                //? if >=1.18 {
                                BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
                                //?} else
                                /*BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.instance.getRenderer(entity);*/

                                if (renderer != null) {
                                    renderData.addBlockEntity(entity, !renderer.shouldRenderOffScreen(entity));
                                }
                            }
                        }

                        if (blockState.isSolidRender(/*? if <1.21.2 {*/slice, blockPos/*?}*/)) {
                            occluder.setOpaque(blockPos);
                        }
                    }
                }
            }

            MeshAppenderRenderer.renderMeshAppenders(renderContext.getMeshAppenders(), context.localSlice(), renderContext.getOrigin(), buffers);
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, blockPos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.forThrowable(ex, "Encountered exception while building chunk meshes"), slice, blockPos);
        }

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
                renderData.addRenderPass(pass);
            }
        }

        renderData.setOcclusionData(occluder.resolve());

        ChunkDataBuiltEvent.BUS.post(new ChunkDataBuiltEvent(renderData));

        return new ChunkBuildOutput(this.render, renderData.build(), meshes, this.buildTime);
    }

    private ReportedException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.addCategory("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.populateBlockDetails(crashReportSection, /*? if >=1.18 {*/ slice, /*?}*/ pos, state);

        crashReportSection.setDetail("Chunk section", this.render);
        if (this.renderContext != null) {
            crashReportSection.setDetail("Render context volume", this.renderContext.getVolume());
        }

        return new ReportedException(report);
    }
}
