package org.embeddedt.embeddium.impl.mixin.modcompat.fabric_renderer_indigo;

//? if ffapi && >=1.20 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemRenderContext.class, remap = false)
public class ItemRenderContextMixin {
    @Unique
    private final TextureAtlas celeritas$blocksAtlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);

    /**
     * @author embeddedt
     * @reason There is currently no efficient & minimalistic API approach to capture textures rendered on FRAPI models.
     * To make matters worse, the MutableQuadView doesn't provide TextureAtlasSprite context. Therefore, we have to take
     * the worst possible approach of computing the sprite from UV coordinates... every frame the item is rendered.
     * Fortunately, the FRAPI sprite finder appears to be quite fast in practice.
     */
    @Inject(method = "renderQuad", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;material()Lnet/fabricmc/fabric/impl/client/indigo/renderer/material/RenderMaterialImpl;"), require = 0)
    private void captureTexture(@Coerce MutableQuadView quad, CallbackInfo ci) {
        float midU = 0, midV = 0;
        for(int i = 0; i < 4; i++) {
            midU += quad.u(i);
            midV += quad.v(i);
        }

        // Detect sprite
        TextureAtlasSprite sprite = ((TextureAtlasExtended)celeritas$blocksAtlas).celeritas$findFromUV(midU / 4, midV / 4);
        if (sprite != null) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }
}
//?}
