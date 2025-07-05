package org.embeddedt.embeddium.impl.mixin.features.render.entity.shadows;

import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.embeddedt.embeddium.api.util.ColorABGR;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);

    /**
     * @author JellySquid
     * @reason Reduce vertex assembly overhead for shadow rendering
     */
    @Inject(method = "renderBlockShadow", at = @At("HEAD"), cancellable = true)
    private static void renderShadowPartFast(PoseStack.Pose entry, VertexConsumer vertices, /*? if >=1.20 {*/ ChunkAccess chunk, /*?}*/ LevelReader world, BlockPos pos, double x, double y, double z, float radius, float opacity, CallbackInfo ci) {
        var writer = VertexBufferWriter.tryOf(vertices);

        if (writer == null)
            return;

        ci.cancel();

        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);

        if (blockState.getRenderShape() == RenderShape.INVISIBLE || !blockState.isCollisionShapeFullBlock(world, blockPos)) {
            return;
        }

        var light = world.getMaxLocalRawBrightness(pos);

        if (light <= 3) {
            return;
        }

        VoxelShape voxelShape = blockState.getShape(world, blockPos);

        if (voxelShape.isEmpty()) {
            return;
        }

        //? if >=1.19 {
        float brightness = LightTexture.getBrightness(world.dimensionType(), light);
        //?} else if >=1.16 {
        /*float brightness = world.dimensionType().brightness(light);
        *///?} else
        /*float brightness = world.getDimension().getBrightness(light);*/
        float alpha = (float) (((double) opacity - ((y - (double) pos.getY()) / 2.0)) * 0.5 * (double) brightness);

        if (alpha >= 0.0F) {
            if (alpha > 1.0F) {
                alpha = 1.0F;
            }

            AABB box = voxelShape.bounds();

            float minX = (float) ((pos.getX() + box.minX) - x);
            float maxX = (float) ((pos.getX() + box.maxX) - x);

            // Need to apply an epsilon pre-1.16 to prevent z-fighting
            float minY = (float) ((pos.getY() + box.minY) - y /*? if <1.16 {*/ /*+ 0.015625 *//*?}*/);

            float minZ = (float) ((pos.getZ() + box.minZ) - z);
            float maxZ = (float) ((pos.getZ() + box.maxZ) - z);

            renderShadowPart(entry, writer, radius, alpha, minX, maxX, minY, minZ, maxZ);
        }
    }

    /**
     * @deprecated don't call, but just in case...
     */
    @Deprecated
    private static void renderShadowPart(PoseStack.Pose matrices, VertexConsumer consumer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        renderShadowPart(matrices, VertexBufferWriter.of(consumer), radius, alpha, minX, maxX, minY, minZ, maxZ);
    }

    @Unique
    private static void renderShadowPart(PoseStack.Pose matrices, VertexBufferWriter writer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        float size = 0.5F * (1.0F / radius);

        float u1 = (-minX * size) + 0.5F;
        float u2 = (-maxX * size) + 0.5F;

        float v1 = (-minZ * size) + 0.5F;
        float v2 = (-maxZ * size) + 0.5F;

        var matNormal = matrices.normal();
        //? if >=1.20 {
        var matPosition = matrices.pose();
        //?} else
        /*var matPosition = Matrix4fExtended.get(matrices.pose());*/

        var color = ColorABGR.withAlpha(SHADOW_COLOR, alpha);
        var normal = MatrixHelper.transformNormal(matNormal, /*? if >=1.20.6 {*/ /*matrices.trustedNormals, *//*?}*/  Direction.UP);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            writeShadowVertex(ptr, matPosition, minX, minY, minZ, u1, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, minX, minY, maxZ, u1, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, maxZ, u2, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, minZ, u2, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            writer
                    .push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    //? if >=1.20 {
    @Unique
    private static void writeShadowVertex(long ptr, Matrix4f matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
        float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
    //?} else {
    /*@Unique
    private static void writeShadowVertex(long ptr, Matrix4fExtended matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = matPosition.transformVecX(x, y, z);
        float yt = matPosition.transformVecY(x, y, z);
        float zt = matPosition.transformVecZ(x, y, z);
    *///?}

        ModelVertex.write(ptr, xt, yt, zt, color, u, v, LightDataAccess.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, normal);
    }
}
