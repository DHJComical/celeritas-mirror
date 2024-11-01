package org.embeddedt.embeddium.impl.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ChunkRenderMatrices(Matrix4fc projection, Matrix4fc modelView) {
    //? if <1.18 {
    /*private static com.mojang.math.Matrix4f getProjectionMatrix() {
        return GameRendererContext.PROJECTION_MATRIX;
    }
    *///?} else if <1.20 {
    /*private static com.mojang.math.Matrix4f getProjectionMatrix() {
        return RenderSystem.getProjectionMatrix();
    }
    *///?}

    public static ChunkRenderMatrices from(PoseStack stack) {
        PoseStack.Pose entry = stack.last();
        //? if >=1.20 {
        return new ChunkRenderMatrices(new Matrix4f(RenderSystem.getProjectionMatrix()), new Matrix4f(entry.pose()));
        //?} else
        /*return new ChunkRenderMatrices(JomlHelper.copy(getProjectionMatrix()), JomlHelper.copy(entry.pose()));*/
    }

    public static ChunkRenderMatrices from(Matrix4f pose) {
        //? if >=1.20 {
        return new ChunkRenderMatrices(new Matrix4f(RenderSystem.getProjectionMatrix()), pose);
        //?} else
        /*return new ChunkRenderMatrices(JomlHelper.copy(getProjectionMatrix()), pose);*/
    }
}
