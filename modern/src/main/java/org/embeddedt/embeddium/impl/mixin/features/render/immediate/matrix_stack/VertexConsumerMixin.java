package org.embeddedt.embeddium.impl.mixin.features.render.immediate.matrix_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <1.20 {
/*import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
*///?} else {
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
//?}
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.math.Matrix3fExtended;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    //? if >=1.20 <1.21 {
    @Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    @Overwrite
    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
        float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);

        return this.vertex(xt, yt, zt);
    }

    @Overwrite
    default VertexConsumer normal(
            //? if <1.20.6 {
            Matrix3f matrix,
            //?} else
            /*PoseStack.Pose pose,*/
            float x, float y, float z) {
        //? if >=1.20.6
        /*var matrix = pose.normal();*/
        float xt = MatrixHelper.transformNormalX(matrix, x, y, z);
        float yt = MatrixHelper.transformNormalY(matrix, x, y, z);
        float zt = MatrixHelper.transformNormalZ(matrix, x, y, z);

        //? if >=1.20.6 {
        /*if (!pose.trustedNormals) {
            float scalar = Math.invsqrt(Math.fma(xt, xt, Math.fma(yt, yt, zt * zt)));

            xt *= scalar;
            yt *= scalar;
            zt *= scalar;
        }
        *///?}

        return this.normal(xt, yt, zt);
    }
    //?} else if >=1.21 {

    /*@Shadow
    VertexConsumer setNormal(float x, float y, float z);

    @Shadow
    VertexConsumer addVertex(float x, float y, float z);

    @Overwrite
    default VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
        float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);

        return this.addVertex(xt, yt, zt);
    }

    @Overwrite
    default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        final Matrix3f matrix = pose.normal();
        float xt = MatrixHelper.transformNormalX(matrix, x, y, z);
        float yt = MatrixHelper.transformNormalY(matrix, x, y, z);
        float zt = MatrixHelper.transformNormalZ(matrix, x, y, z);

        if (!pose.trustedNormals) {
            float scalar = Math.invsqrt(Math.fma(xt, xt, Math.fma(yt, yt, zt * zt)));

            xt *= scalar;
            yt *= scalar;
            zt *= scalar;
        }

        return this.setNormal(xt, yt, zt);
    }
    *///?} else {
    /*@Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    @Overwrite
    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        float xt = ((Matrix4fExtended)(Object)matrix).transformVecX(x, y, z);
        float yt = ((Matrix4fExtended)(Object)matrix).transformVecY(x, y, z);
        float zt = ((Matrix4fExtended)(Object)matrix).transformVecZ(x, y, z);

        return this.vertex(xt, yt, zt);
    }

    @Overwrite
    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        var matrixExt = Matrix3fExtended.get(matrix);
        float xt = matrixExt.transformVecX(x, y, z);
        float yt = matrixExt.transformVecY(x, y, z);
        float zt = matrixExt.transformVecZ(x, y, z);

        return this.normal(xt, yt, zt);
    }
    *///?}
}