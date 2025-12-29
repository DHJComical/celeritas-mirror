package org.taumc.celeritas.render.terrain.task;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import lombok.AllArgsConstructor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildOutput;
import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderTask;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.SectionVisibilityBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.task.CancellationToken;
import org.joml.Vector3d;
import org.taumc.celeritas.render.terrain.PostmodernChunkBuildContext;

import java.util.function.Function;

@AllArgsConstructor
public class MeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final BlockAndTintGetter world;
    private final Vector3d cameraPosition;
    private final int frame;

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext jobContext, CancellationToken cancellationToken) {
        PostmodernChunkBuildContext buildContext = (PostmodernChunkBuildContext)jobContext;
        MinecraftBuiltRenderSectionData<TextureAtlasSprite, BlockEntity> renderData = new MinecraftBuiltRenderSectionData<>();
        SectionVisibilityBuilder occluder = new SectionVisibilityBuilder();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);
        BlockPos.MutableBlockPos modelOffset = new BlockPos.MutableBlockPos();

        var slice = this.world;

        ObjectArrayList<BlockModelPart> renderedParts = new ObjectArrayList<>(10);
        PoseStack stack = buildContext.getPoseStack();
        Function<ChunkSectionLayer, VertexConsumer> bufferLookup = buildContext::getVertexConsumer;

        buildContext.buffers.init(renderData, this.render.getSectionIndex());

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        blockPos.set(x, y, z);

                        BlockState blockState = slice.getBlockState(blockPos);

                        // Fast path - skip blocks that are air and don't have any custom logic
                        if (blockState.isAir() && blockState.getRenderShape() == RenderShape.INVISIBLE && !blockState.hasBlockEntity()) {
                            continue;
                        }

                        modelOffset.set(x & 15, y & 15, z & 15);

                        if (blockState.getRenderShape() == RenderShape.MODEL) {
                            buildContext.getRandomSource().setSeed(blockState.getSeed(blockPos));

                            var model = buildContext.getBlockModels().getBlockModel(blockState);
                            model.collectParts(slice, blockPos, blockState, buildContext.getRandomSource(), renderedParts);

                            stack.pushPose();
                            stack.translate(x & 15, y & 15, z & 15);
                            Minecraft.getInstance().getBlockRenderer().renderBatched(blockState, blockPos, slice, stack,
                                    bufferLookup, true, renderedParts);
                            buildContext.flushVertexConsumers();
                            stack.popPose();
                            renderedParts.clear();
                        }

                        if (blockState.isSolidRender()) {
                            occluder.markOpaque(x, y, z);
                        }
                    }
                }
            }
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, blockPos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.forThrowable(ex, "Encountered exception while building chunk meshes"), world, blockPos);
        }

        Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes = BuiltSectionMeshParts.groupFromBuildBuffers(buildContext.buffers,(float)cameraPosition.x - minX, (float)cameraPosition.y - minY, (float)cameraPosition.z - minZ);

        if (!meshes.isEmpty()) {
            renderData.hasBlockGeometry = true;
        }

        renderData.visibilityData = occluder.computeVisibilityEncoding();

        return new ChunkBuildOutput(this.render, renderData, meshes, this.frame);
    }

    private ReportedException fillCrashInfo(CrashReport report, BlockAndTintGetter slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.addCategory("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.populateBlockDetails(crashReportSection, slice, pos, state);

        crashReportSection.setDetail("Chunk section", this.render);

        return new ReportedException(report);
    }
}
