package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.embeddedt.embeddium.impl.render.vertex.FastVertexWriter;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumer, FastVertexWriter {
    @Shadow
    @Final
    private VertexFormat format;

    @Shadow
    private long beginVertex() {
        throw new AssertionError();
    }

    @Shadow
    private static void putRgba(long pointer, int argb) {
        throw new AssertionError();
    }

    @Shadow
    private static void putPackedUv(long pointer, int packedUv) {
        throw new AssertionError();
    }

    @Override
    public boolean celeritas$putParticleVertex(float x, float y, float z, float u, float v, int color, int light) {
        if (this.format != DefaultVertexFormat.PARTICLE) {
            return false;
        }

        long pointer = this.beginVertex();
        MemoryUtil.memPutFloat(pointer + 0L, x);
        MemoryUtil.memPutFloat(pointer + 4L, y);
        MemoryUtil.memPutFloat(pointer + 8L, z);
        MemoryUtil.memPutFloat(pointer + 12L, u);
        MemoryUtil.memPutFloat(pointer + 16L, v);
        putRgba(pointer + 20L, color);
        putPackedUv(pointer + 24L, light);
        return true;
    }

    @Override
    public boolean celeritas$putTextVertex(Matrix4fc pose, float x, float y, float z, int color, float u, float v, int light) {
        if (this.format != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
            return false;
        }

        int prop = pose.properties();
        long pointer = this.beginVertex();
        MemoryUtil.memPutFloat(pointer + 0L, celeritas$transformX(pose, prop, x, y, z));
        MemoryUtil.memPutFloat(pointer + 4L, celeritas$transformY(pose, prop, x, y, z));
        MemoryUtil.memPutFloat(pointer + 8L, celeritas$transformZ(pose, prop, x, y, z));
        putRgba(pointer + 12L, color);
        MemoryUtil.memPutFloat(pointer + 16L, u);
        MemoryUtil.memPutFloat(pointer + 20L, v);
        putPackedUv(pointer + 24L, light);
        return true;
    }

    /**
     * Same as the default implementation, but transforms the position in scalars rather than allocating a vector.
     */
    @Override
    public VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        int prop = pose.properties();
        return this.addVertex(celeritas$transformX(pose, prop, x, y, z), celeritas$transformY(pose, prop, x, y, z), celeritas$transformZ(pose, prop, x, y, z));
    }

    // These match Matrix4fc.transformPosition, including its shortcuts for identity and translation-only matrices.
    @Unique
    private static float celeritas$transformX(Matrix4fc mat, int prop, float x, float y, float z) {
        if ((prop & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            return x;
        } else if ((prop & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            return x + mat.m30();
        }
        return mat.m00() * x + (mat.m10() * y + (mat.m20() * z + mat.m30()));
    }

    @Unique
    private static float celeritas$transformY(Matrix4fc mat, int prop, float x, float y, float z) {
        if ((prop & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            return y;
        } else if ((prop & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            return y + mat.m31();
        }
        return mat.m01() * x + (mat.m11() * y + (mat.m21() * z + mat.m31()));
    }

    @Unique
    private static float celeritas$transformZ(Matrix4fc mat, int prop, float x, float y, float z) {
        if ((prop & Matrix4fc.PROPERTY_IDENTITY) != 0) {
            return z;
        } else if ((prop & Matrix4fc.PROPERTY_TRANSLATION) != 0) {
            return z + mat.m32();
        }
        return mat.m02() * x + (mat.m12() * y + (mat.m22() * z + mat.m32()));
    }

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
