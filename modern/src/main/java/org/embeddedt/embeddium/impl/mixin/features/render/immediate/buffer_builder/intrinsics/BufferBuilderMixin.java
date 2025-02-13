package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.BufferBuilder;
//? if <1.21
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({ "SameParameterValue" })
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin /*? if <1.21 {*/ extends DefaultedVertexConsumer /*?} else {*/ /*implements VertexConsumer *//*?}*/ {
    @Shadow
    private boolean fastFormat;

    //? if >=1.21 {
    /*@Override
    public void putBulkData(PoseStack.Pose matrices, BakedQuad bakedQuad, float r, float g, float b, float a, int light, int overlay) {
        this.putBulkData(matrices, bakedQuad, r, g, b, a, light, overlay, false);
    }

    public void putBulkData(PoseStack.Pose matrices, BakedQuad bakedQuad, float r, float g, float b, float a, int light, int overlay, boolean colorize) {
    *///?} else {
    @Override
    public void putBulkData(PoseStack.Pose matrices, BakedQuad bakedQuad, float r, float g, float b, /*? if >=1.20.6 {*/ /*float a, *//*?}*/ int light, int overlay) {
        boolean colorize = true;
        //? if <1.20.6
        float a = 1.0f;
    //?}

        BakedQuadView quad = BakedQuadView.of(bakedQuad);

        if (!this.fastFormat) {
            //? if <1.20.6
            super.putBulkData(matrices, bakedQuad, r, g, b, light, overlay);
            //? if >=1.20.6 <1.21
            /*super.putBulkData(matrices, bakedQuad, r, g, b, a, light, overlay);*/
            //? if >=1.21 && neoforge
            /*VertexConsumer.super.putBulkData(matrices, bakedQuad, r, g, b, a, light, overlay, colorize);*/
            //? if >=1.21 && !neoforge
            /*VertexConsumer.super.putBulkData(matrices, bakedQuad, r, g, b, a, light, overlay);*/

            SpriteUtil.markSpriteActive(quad.getSprite());

            return;
        }

        //? if <1.21 {
        if (this.defaultColorSet) {
            throw new IllegalStateException();
        }
        //?}

        if (quad.getVerticesCount() < 4) {
            return; // we do not accept quads with less than 4 properly sized vertices
        }

        VertexBufferWriter writer = VertexBufferWriter.of(this);


        int color = ColorABGR.pack(r, g, b, a);
        BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, colorize);

        SpriteUtil.markSpriteActive(quad.getSprite());
    }

    @Override
    public void putBulkData(PoseStack.Pose matrices, BakedQuad bakedQuad, float[] brightnessTable, float r, float g, float b, /*? if >=1.20.6 {*/ /*float a, *//*?}*/ int[] light, int overlay, boolean colorize) {
        //? if <1.20.6
        float a = 1.0f;

        BakedQuadView quad = BakedQuadView.of(bakedQuad);

        if (!this.fastFormat) {
            //? if >=1.21
            /*VertexConsumer.*/
            super.putBulkData(matrices, bakedQuad, brightnessTable, r, g, b, /*? if >=1.20.6 {*/ /*a, *//*?}*/ light, overlay, colorize);

            SpriteUtil.markSpriteActive(quad.getSprite());

            return;
        }

        //? if <1.21 {
        if (this.defaultColorSet) {
            throw new IllegalStateException();
        }
        //?}

        if (quad.getVerticesCount() < 4) {
            return; // we do not accept quads with less than 4 properly sized vertices
        }

        VertexBufferWriter writer = VertexBufferWriter.of(this);

        BakedModelEncoder.writeQuadVertices(writer, matrices, quad, r, g, b, a, brightnessTable, colorize, light, overlay);

        SpriteUtil.markSpriteActive(quad.getSprite());
    }
}
