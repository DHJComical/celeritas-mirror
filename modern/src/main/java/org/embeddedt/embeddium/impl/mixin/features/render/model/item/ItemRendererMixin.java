package org.embeddedt.embeddium.impl.mixin.features.render.model.item;

import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.render.vertex.VertexConsumerUtils;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
//? if <1.21.4-alpha.24.45.a {
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import org.embeddedt.embeddium.impl.model.color.interop.ItemColorsExtended;
//?}
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
//$ rng_import
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
//? if >=1.18
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.embeddedt.embeddium.impl.util.rand.XoRoShiRoRandom;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Random;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique
    //? if >=1.19 {
    private final RandomSource random = new SingleThreadedRandomSource(42L);
     //?} else
    /*private final Random random = new XoRoShiRoRandom(42L);*/

    //? if <1.21.4-alpha.24.45.a {
    @Shadow
    @Final
    private ItemColors itemColors;
    //?}

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    //? if <1.21.5-alpha.25.7.a {
    @Inject(method = "renderModelLists", at = @At("HEAD"), cancellable = true)
    private
    //? if >=1.21.4-alpha.24.45.a
    /*static*/
    void renderModelFast(net.minecraft.client.resources.model.BakedModel model,
                                 //? if <1.21.4-alpha.24.45.a {
                                 ItemStack itemStack,
                                 //?} else
                                 /*int[] colorProvider,*/
                                 int light, int overlay, PoseStack matrixStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        //? if >=1.19 {
        RandomSource random = new SingleThreadedRandomSource(42L);
        //?} else
        /*Random random = new XoRoShiRoRandom(42L);*/

        PoseStack.Pose matrices = matrixStack.last();

        //? if <1.21.4-alpha.24.45.a {
        ItemColor colorProvider = null;

        if (!itemStack.isEmpty()) {
            colorProvider = ((ItemColorsExtended) this.itemColors).sodium$getColorProvider(itemStack);
        }
        //?}

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);

            if (!quads.isEmpty()) {
                renderBakedItemQuads(matrices, writer, quads, /*? if <1.21.4-alpha.24.45.a {*/ itemStack,/*?}*/ colorProvider, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);

        if (!quads.isEmpty()) {
            renderBakedItemQuads(matrices, writer, quads, /*? if <1.21.4-alpha.24.45.a {*/ itemStack,/*?}*/ colorProvider, light, overlay);
        }
    }
    //?} else {
    /*@Inject(method = "renderQuadList", at = @At("HEAD"), cancellable = true)
    private static void renderQuadListFast(PoseStack stack, VertexConsumer vertexConsumer, List<BakedQuad> list, int[] colorProvider, int light, int overlay, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        renderBakedItemQuads(stack.last(), writer, list, colorProvider, light, overlay);
    }
    *///?}

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void renderBakedItemQuads(PoseStack.Pose matrices, VertexBufferWriter writer, List<BakedQuad> quads,
                                      //? if <1.21.4-alpha.24.45.a {
                                      ItemStack itemStack, ItemColor colorProvider,
                                      //?} else
                                      /*int[] colorProvider,*/
                                      int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuadView quad = BakedQuadView.of(quads.get(i));

            if (quad.getVerticesCount() < 4) {
                continue; // ignore bad quads
            }

            int color = 0xFFFFFFFF;

            if (colorProvider != null && quad.hasColor()) {
                int quadColorIndex = quad.getColorIndex();
                //? if <1.21.4-alpha.24.45.a {
                int colorProviderColor = colorProvider.getColor(itemStack, quadColorIndex);
                //?} else
                /*int colorProviderColor = quadColorIndex < colorProvider.length ? colorProvider[quadColorIndex] : -1;*/
                color = ColorARGB.toABGR(colorProviderColor/*? if <1.20.5 {*/, 255/*?}*/);
            }

            boolean shouldReadExistingColor;

            //? if forgelike {
            shouldReadExistingColor = true;
            //?} else
            /*shouldReadExistingColor = false;*/

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, shouldReadExistingColor);

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
