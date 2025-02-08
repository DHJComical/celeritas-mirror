package org.embeddedt.embeddium.impl.modern.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.render.chunk.SectionInfoBuilder;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionBuiltInfo;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.ModernChunkBuildContext;
import org.embeddedt.embeddium.impl.modern.render.chunk.occlusion.ModernGraphDirection;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.*;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderCache;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.GeometryCategory;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.VisibilityEncoding;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.WorldUtil;
import org.embeddedt.embeddium.impl.util.collections.SetUtil;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ChunkRenderContext;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
import org.joml.Vector3d;

import java.util.*;
import java.util.function.Predicate;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final ChunkRenderContext renderContext;

    private final int buildTime;
    private final Vector3d camera;


    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext renderContext, int time, Vector3d camera) {
        this.render = render;
        this.renderContext = renderContext;
        this.buildTime = time;
        this.camera = camera;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext jobContext, CancellationToken cancellationToken) {
        ModernChunkBuildContext buildContext = (ModernChunkBuildContext)jobContext;
        ContextBundle<RenderSection> renderData = new ContextBundle<>(RenderSection.class);
        initializeContextBundle(renderData);
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

        boolean voxelizingLight = WorldRenderingSettings.INSTANCE.shouldVoxelizeLightBlocks();

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

                        //? if >=1.17 {
                        if (voxelizingLight && blockState.getBlock() instanceof net.minecraft.world.level.block.LightBlock) {
                            cache.getSpecialBlockRenderer().voxelizeLightBlock(blockPos, blockState, buffers);
                        }
                        //?}

                        if (blockState.getRenderShape() == RenderShape.MODEL) {
                            long seed = blockState.getSeed(blockPos);
                            context.update(GeometryCategory.BLOCK, blockPos, modelOffset, blockState, cache.getBlockModels().getBlockModel(blockState), seed);
                            var model = context.model();

                            //? if forgelike {
                            var modelData = model.getModelData(context.localSlice(), blockPos, blockState, modelDataGetter.getModelData(blockPos));
                            context.setModelData(modelData);

                            context.random().setSeed(seed); // for render layers
                            //?}

                            //? if forgelike && >=1.19 {
                            // We optimize the asList() call to return a cached ImmutableList, so this will not allocate.
                            var renderTypeList = model.getRenderTypes(blockState, context.random(), modelData).asList();
                            //noinspection ForLoopReplaceableByForEach
                            for (int i = 0; i < renderTypeList.size(); i++) {
                                context.setRenderLayer(renderTypeList.get(i));
                                cache.getBlockRenderer().renderModel(context, buffers);
                            }
                            //?} else if forge && <1.19 {
                            /*var renderTypeList = cache.getBlockRenderLayerCache().forState(blockState);
                            //noinspection ForLoopReplaceableByForEach
                            for (int i = 0; i < renderTypeList.size(); i++) {
                                var layer = renderTypeList.get(i);
                                //? if >=1.17 {
                                ForgeHooksClient.setRenderType(layer);
                                //?} else
                                /^ForgeHooksClient.setRenderLayer(layer);^/
                                context.setRenderLayer(layer);
                                cache.getBlockRenderer()
                                        .renderModel(context, buffers);
                            }
                            *///?} else {
                            /*context.setRenderLayer(ItemBlockRenderTypes.getChunkRenderType(blockState));
                            cache.getBlockRenderer()
                                    .renderModel(context, buffers);
                            *///?}
                        }

                        FluidState fluidState = blockState.getFluidState();

                        if (!fluidState.isEmpty()) {
                            context.update(GeometryCategory.FLUID, blockPos, modelOffset, blockState, null, 42L);
                            context.setRenderLayer(ItemBlockRenderTypes.getRenderLayer(fluidState));
                            cache.getFluidRenderer().render(context, buffers);
                        }

                        if (WorldUtil.hasBlockEntity(blockState)) {
                            BlockEntity entity = slice.getBlockEntity(blockPos);

                            if (entity != null) {
                                //? if >=1.17 {
                                BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
                                //?} else
                                /*BlockEntityRenderer<BlockEntity> renderer = net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher.instance.getRenderer(entity);*/

                                if (renderer != null) {
                                    renderData.getContext(renderer.shouldRenderOffScreen(entity) ? ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES : ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES).add(entity);
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

        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buffers,(float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.setContext(ModernRenderSectionBuiltInfo.HAS_BLOCK_GEOMETRY, true);
        }

        encodeVisibilityData(occluder, renderData);

        postSectionDataBuiltEvent(renderData);

        return new ChunkBuildOutput(this.render, renderData, meshes, this.buildTime);
    }

    private static void initializeContextBundle(ContextBundle<RenderSection> renderData) {
        renderData.setContext(ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES, new ArrayList<>());
        renderData.setContext(ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES, new ArrayList<>());
        renderData.setContext(ModernRenderSectionBuiltInfo.ANIMATED_SPRITES, new ObjectOpenHashSet<>());
    }

    private static void encodeVisibilityData(VisGraph occluder, ContextBundle<RenderSection> renderData) {
        var data = occluder.resolve();
        renderData.setContext(RenderSection.VISIBILITY_DATA, VisibilityEncoding.encode((from, to) -> data.visibilityBetween(ModernGraphDirection.toEnum(from), ModernGraphDirection.toEnum(to))));
    }

    private static void postSectionDataBuiltEvent(ContextBundle<RenderSection> renderData) {
        ChunkDataBuiltEvent.BUS.post(new ChunkDataBuiltEvent(new SectionInfoBuilder() {
            @Override
            public void addSprite(TextureAtlasSprite sprite) {
                if (!SpriteUtil.hasAnimation(sprite)) {
                    return;
                }
                renderData.getContext(ModernRenderSectionBuiltInfo.ANIMATED_SPRITES).add(sprite);
            }

            @Override
            public void addBlockEntity(BlockEntity entity, boolean cull) {
                renderData.getContext(cull ? ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES : ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES).add(entity);
            }

            @Override
            public void removeBlockEntitiesIf(Predicate<BlockEntity> filter) {
                renderData.getContext(ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES).removeIf(filter);
                renderData.getContext(ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES).removeIf(filter);
            }
        }));
        renderData.mapContext(ModernRenderSectionBuiltInfo.GLOBAL_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(ModernRenderSectionBuiltInfo.CULLED_BLOCK_ENTITIES, List::copyOf);
        renderData.mapContext(ModernRenderSectionBuiltInfo.ANIMATED_SPRITES, List::copyOf);
    }

    private ReportedException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.addCategory("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.populateBlockDetails(crashReportSection, /*? if >=1.17 {*/ slice, /*?}*/ pos, state);

        crashReportSection.setDetail("Chunk section", this.render);
        if (this.renderContext != null) {
            crashReportSection.setDetail("Render context volume", this.renderContext.getVolume());
        }

        return new ReportedException(report);
    }
}
