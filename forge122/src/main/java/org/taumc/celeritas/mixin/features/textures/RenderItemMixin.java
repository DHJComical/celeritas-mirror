package org.taumc.celeritas.mixin.features.textures;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.sprite.SpriteUtil;

import java.util.List;

@Mixin(RenderItem.class)
public class RenderItemMixin {
    @Inject(method = "renderQuads", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/pipeline/LightUtil;renderQuadColor(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/block/model/BakedQuad;I)V"))
    private void markSpriteActive(CallbackInfo ci, @Local(ordinal = 0) BakedQuad quad) {
        var sprite = quad.getSprite();
        if (sprite != null) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }
}
