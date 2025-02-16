package org.embeddedt.embeddium.impl.mixin.core.model.quad;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
//? if >=1.20
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Check if quad's UVs are contained within the sprite's boundaries; if so, mark it as having a trusted sprite
 * (meaning the particle sprite matches the encoded UVs)
 */
@Mixin(FaceBakery.class)
public class BakedQuadFactoryMixin {
    //? if <1.21.4-alpha.24.45.a {
    @ModifyReturnValue(method = "bakeQuad", at = @At("RETURN"))
    private BakedQuad setMaterialClassification(BakedQuad quad, @Local(ordinal = 0, argsOnly = true) BlockElementFace face, @Local(ordinal = 0, argsOnly = true) TextureAtlasSprite sprite) {
        handleMaterialClassifications(quad, sprite, face);
        return quad;
    }
    //?} else {
    /*@ModifyReturnValue(method = "bakeQuad", at = @At("RETURN"))
    private static BakedQuad setMaterialClassification(BakedQuad quad, @Local(ordinal = 0, argsOnly = true) BlockElementFace face, @Local(ordinal = 0, argsOnly = true) TextureAtlasSprite sprite) {
        handleMaterialClassifications(quad, sprite, face);
        return quad;
    }
    *///?}

    private static void handleMaterialClassifications(BakedQuad quad, TextureAtlasSprite sprite, BlockElementFace face) {
        if (sprite.getClass() == TextureAtlasSprite.class /*? if >=1.20 {*/ && sprite.contents().getClass() == SpriteContents.class /*?}*/) {

            float minUV = Float.MAX_VALUE, maxUV = Float.MIN_VALUE;
            //? if <1.21
            float[] uvs = face.uv.uvs;
            //? if >=1.21 <1.21.5-alpha.25.7.a
            /*float[] uvs = face.uv().uvs;*/

            //? if <1.21.5-alpha.25.7.a {
            for (float uv : uvs) {
                minUV = Math.min(minUV, uv);
                maxUV = Math.max(maxUV, uv);
            }
            //?} else {
            /*var uvs = face.uvs();
            if (uvs != null) {
                minUV = Math.min(uvs.minU(), Math.min(uvs.minV(), Math.min(uvs.maxU(), uvs.maxV())));
                maxUV = Math.max(uvs.minU(), Math.max(uvs.minV(), Math.max(uvs.maxU(), uvs.maxV())));
            } else {
                // assume default bounds are always fine
                minUV = 0;
                maxUV = 16;
            }
            *///?}

            if (minUV >= 0 && maxUV <= 16) {
                // Quad UVs do not extend outside texture boundary, we can trust the given sprite
                BakedQuadView view = (BakedQuadView)(Object)quad;
                view.addFlags(ModelQuadFlags.IS_TRUSTED_SPRITE);
            }

        }
    }

    //? if forgelike && <1.20.2 {
    /**
     * Backport of NeoForge PR <a href="https://github.com/neoforged/NeoForge/pull/207">#207</a>. The Forge patch
     * here reduces UV precision for no reason and has not been needed since at least 1.14. Vanilla already
     * adjusts the UV offsets itself.
     */
    @Inject(method =
            //? if >=1.20 {
            "fillVertex([IILorg/joml/Vector3f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/block/model/BlockFaceUV;)V"
            //?} else
            /*"fillVertex"*/
            , at = @At("RETURN"))
    private void undoForgeUVExpansion(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) int[] vertices, @Local(ordinal = 0, argsOnly = true) int cornerIndex, @Local(ordinal = 0, argsOnly = true) TextureAtlasSprite sprite, @Local(ordinal = 0, argsOnly = true) net.minecraft.client.renderer.block.model.BlockFaceUV element) {
        int i = cornerIndex * 8;
        vertices[i + 4] = Float.floatToRawIntBits(sprite.getU(element.getU(cornerIndex)));
        vertices[i + 4 + 1] = Float.floatToRawIntBits(sprite.getV(element.getV(cornerIndex)));
    }
    //?}
}
