package org.embeddedt.embeddium.impl.mixin.features.render.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.embeddedt.embeddium.impl.render.vertex.ParticleVertexWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(QuadParticleRenderState.class)
public class QuadParticleRenderStateMixin {
    /**
     * @author embeddedt
     * @reason Rotate the corners in scalars and write each vertex in one call, rather than allocating a quaternion per
     * particle and a vector per vertex and filling elements one at a time
     */
    @Overwrite
    protected void renderRotatedQuad(
            VertexConsumer builder, float x, float y, float z, float xRot, float yRot, float zRot, float wRot, float scale,
            float u0, float u1, float v0, float v1, int color, int lightCoords
    ) {
        // The terms of Quaternionf.transform that remain for a corner with no Z component.
        float xx = xRot * xRot, yy = yRot * yRot, zz = zRot * zRot, ww = wRot * wRot;
        float xy = xRot * yRot, xz = xRot * zRot, yz = yRot * zRot, xw = xRot * wRot, zw = zRot * wRot, yw = yRot * wRot;
        float k = 1.0F / (xx + yy + zz + ww);
        float r00 = (xx - yy - zz + ww) * k, r01 = 2.0F * (xy - zw) * k;
        float r10 = 2.0F * (xy + zw) * k, r11 = (yy - xx - zz + ww) * k;
        float r20 = 2.0F * (xz - yw) * k, r21 = 2.0F * (yz + xw) * k;

        celeritas$putVertex(builder, r00, r01, r10, r11, r20, r21, x, y, z, 1.0F, -1.0F, scale, u1, v1, color, lightCoords);
        celeritas$putVertex(builder, r00, r01, r10, r11, r20, r21, x, y, z, 1.0F, 1.0F, scale, u1, v0, color, lightCoords);
        celeritas$putVertex(builder, r00, r01, r10, r11, r20, r21, x, y, z, -1.0F, 1.0F, scale, u0, v0, color, lightCoords);
        celeritas$putVertex(builder, r00, r01, r10, r11, r20, r21, x, y, z, -1.0F, -1.0F, scale, u0, v1, color, lightCoords);
    }

    @Unique
    private static void celeritas$putVertex(
            VertexConsumer builder, float r00, float r01, float r10, float r11, float r20, float r21,
            float x, float y, float z, float nx, float ny, float scale, float u, float v, int color, int lightCoords
    ) {
        float px = (r00 * nx + r01 * ny) * scale + x;
        float py = (r10 * nx + r11 * ny) * scale + y;
        float pz = (r20 * nx + r21 * ny) * scale + z;

        if (!(builder instanceof ParticleVertexWriter writer) || !writer.celeritas$putParticleVertex(px, py, pz, u, v, color, lightCoords)) {
            builder.addVertex(px, py, pz).setUv(u, v).setColor(color).setLight(lightCoords);
        }
    }
}
