package org.taumc.celeritas.mixin.features.textures;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.taumc.celeritas.impl.render.terrain.sprite.SpriteUtil;

@Mixin(RenderItem.class)
public class RenderItemMixin {
    @ModifyExpressionValue(method = { "func_175032_a", "renderQuads" }, at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object markSpriteActive(Object o) {
        if (o instanceof BakedQuad quad) {
            var sprite = quad.getSprite();
            if (sprite != null) {
                SpriteUtil.markSpriteActive(sprite);
            }
        }
        return o;
    }
}
