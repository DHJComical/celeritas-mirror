package org.embeddedt.embeddium.impl.mixin.features.render.model.block;

import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.render.vertex.VertexConsumerUtils;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.18
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
//? if forge && >=1.19
import net.minecraftforge.client.model.data.ModelData;
//? if forge && <1.19
/*import net.minecraftforge.client.model.data.IModelData;*/
//? if neoforge
/*import net.neoforged.neoforge.client.model.data.ModelData;*/
import org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {
    @Unique
    //? if >=1.19 {
    private final RandomSource random = new SingleThreadedRandomSource(42L);
     //?} else
    /*private final Random random = new XoRoShiRoRandom(42L);*/

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Inject(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;" +
            /*? if <1.21.5-alpha.25.7.a {*/ "Lnet/minecraft/client/resources/model/BakedModel;" + /*?}*/
            /*? if >=1.21.5-alpha.25.7.a {*/ /*"Lnet/minecraft/client/renderer/block/model/BlockStateModel;" + *//*?}*/
            "FFFII" +
            /*? if forge && >=1.19 {*/ "Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;" +  /*?}*/
            /*? if forge && <1.19 {*/ /*"Lnet/minecraftforge/client/model/data/IModelData;" +  *//*?}*/
            /*? if neoforge {*/ /*"Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;" +  *//*?}*/
    ")V", at = @At("HEAD"), cancellable = true/*? if forgelike && >=1.17 {*/, remap = false/*?}*/)
    private void renderFast(PoseStack.Pose entry, VertexConsumer vertexConsumer, BlockState blockState,
                            //? if <1.21.5-alpha.25.7.a {
                            net.minecraft.client.resources.model.BakedModel bakedModel,
                            //?} else
                            /*net.minecraft.client.renderer.block.model.BlockStateModel bakedModel,*/
                            float red, float green, float blue, int light, int overlay,
                            /*? if forgelike && >=1.19 {*/ModelData modelData, RenderType renderType,/*?}*//*? if forge && <1.19 {*//*IModelData modelData,*//*?}*/ CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);
        if(writer == null) {
            return;
        }

        ci.cancel();

        /*$ rng >>*/ RandomSource random = this.random;

        // Clamp color ranges
        red = Mth.clamp(red, 0.0F, 1.0F);
        green = Mth.clamp(green, 0.0F, 1.0F);
        blue = Mth.clamp(blue, 0.0F, 1.0F);

        int defaultColor = ColorABGR.pack(red, green, blue, 1.0F);

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, random/*? if forgelike && >=1.19 {*/, modelData, renderType /*?}*//*? if forge && <1.19 {*//*, modelData*//*?}*/);

            if (!quads.isEmpty()) {
                renderQuads(entry, writer, defaultColor, quads, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, random/*? if forgelike && >=1.19 {*/, modelData, renderType /*?}*//*? if forge && <1.19 {*//*, modelData*//*?}*/);

        if (!quads.isEmpty()) {
            renderQuads(entry, writer, defaultColor, quads, light, overlay);
        }
    }

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void renderQuads(PoseStack.Pose matrices, VertexBufferWriter writer, int defaultColor, List<BakedQuad> quads, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);

            BakedQuadView quad = (BakedQuadView)(Object)bakedQuad;

            if (quad.getVerticesCount() < 4) {
                continue;
            }

            int color = quad.hasColor() ? defaultColor : 0xFFFFFFFF;

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, true);

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
