package org.taumc.celeritas.mixin.core.terrain;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class BakedQuadFactoryMixin {
    @ModifyReturnValue(method = "makeBakedQuad(Lorg/lwjgl/util/vector/Vector3f;Lorg/lwjgl/util/vector/Vector3f;Lnet/minecraft/client/renderer/block/model/BlockPartFace;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/util/EnumFacing;Lnet/minecraftforge/common/model/ITransformation;Lnet/minecraft/client/renderer/block/model/BlockPartRotation;ZZ)Lnet/minecraft/client/renderer/block/model/BakedQuad;", at = @At("RETURN"))
    private BakedQuad setMaterialClassification(BakedQuad quad, @Local(ordinal = 0, argsOnly = true) BlockPartFace face, @Local(ordinal = 0, argsOnly = true) TextureAtlasSprite sprite) {
        handleMaterialClassifications(quad, sprite, face);
        return quad;
    }

    private static void handleMaterialClassifications(BakedQuad quad, TextureAtlasSprite sprite, BlockPartFace face) {
        if (sprite.getClass() == TextureAtlasSprite.class) {
            float minUV = Float.MAX_VALUE, maxUV = Float.MIN_VALUE;
            float[] uvs = face.blockFaceUV.uvs;

            for (float uv : uvs) {
                minUV = Math.min(minUV, uv);
                maxUV = Math.max(maxUV, uv);
            }

            if (minUV >= 0 && maxUV <= 16) {
                // Quad UVs do not extend outside texture boundary, we can trust the given sprite
                BakedQuadView view = (BakedQuadView)(Object)quad;
                view.addFlags(ModelQuadFlags.IS_TRUSTED_SPRITE);
            }
        }
    }
}
