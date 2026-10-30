package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumer {
    /**
     * Same as the default implementation, but transforms positions and normals in scalars rather than allocating
     * vectors for every quad and vertex.
     */
    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        var mat = pose.pose();
        var normalMat = pose.normal();

        var direction = quad.direction();
        float dx = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
        float nx = normalMat.m00() * dx + (normalMat.m10() * dy + normalMat.m20() * dz);
        float ny = normalMat.m01() * dx + (normalMat.m11() * dy + normalMat.m21() * dz);
        float nz = normalMat.m02() * dx + (normalMat.m12() * dy + normalMat.m22() * dz);
        if (!pose.trustedNormals) {
            float invLength = 1.0F / (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx *= invLength;
            ny *= invLength;
            nz *= invLength;
        }

        int lightEmission = quad.materialInfo().lightEmission();
        var bakedColors = quad.bakedColors();
        var bakedNormals = quad.bakedNormals();

        for (int vertex = 0; vertex < 4; vertex++) {
            var position = quad.position(vertex);
            float x = position.x(), y = position.y(), z = position.z();
            float xt = mat.m00() * x + (mat.m10() * y + (mat.m20() * z + mat.m30()));
            float yt = mat.m01() * x + (mat.m11() * y + (mat.m21() * z + mat.m31()));
            float zt = mat.m02() * x + (mat.m12() * y + (mat.m22() * z + mat.m32()));

            // Like NeoForge's applyBakedNormals, a baked normal replaces the face normal for this and later vertices.
            int packedNormal = bakedNormals.normal(vertex);
            if (!BakedNormals.isUnspecified(packedNormal)) {
                float bx = BakedNormals.unpackX(packedNormal), by = BakedNormals.unpackY(packedNormal), bz = BakedNormals.unpackZ(packedNormal);
                nx = normalMat.m00() * bx + (normalMat.m10() * by + normalMat.m20() * bz);
                ny = normalMat.m01() * bx + (normalMat.m11() * by + normalMat.m21() * bz);
                nz = normalMat.m02() * bx + (normalMat.m12() * by + normalMat.m22() * bz);
            }

            long packedUv = quad.packedUV(vertex);
            int color = ARGB.multiply(instance.getColor(vertex), bakedColors.color(vertex));
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
            this.addVertex(xt, yt, zt, color, UVPair.unpackU(packedUv), UVPair.unpackV(packedUv), instance.overlayCoords(), light, nx, ny, nz);
        }
    }
}
