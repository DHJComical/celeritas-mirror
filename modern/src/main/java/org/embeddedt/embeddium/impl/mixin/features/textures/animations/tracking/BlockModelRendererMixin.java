package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {
    /**
     * @reason Ensure sprites rendered through renderSmooth/renderFlat in immediate-mode are marked as active.
     * This doesn't affect vanilla to my knowledge, but mods can trigger it.
     * @author embeddedt
     */
    @Inject(method = "putQuadData", at = @At("HEAD"))
    private void preRenderQuad(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) BakedQuad quad) {
        SpriteUtil.markSpriteActive(BakedQuadView.of(quad).getSprite());
    }
}
