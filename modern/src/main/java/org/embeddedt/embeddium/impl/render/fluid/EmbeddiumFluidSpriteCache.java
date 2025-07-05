package org.embeddedt.embeddium.impl.render.fluid;

//? if forge && >=1.19
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
//? if neoforge
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;*/

//? if forgelike {
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;


public class EmbeddiumFluidSpriteCache {
    // Cache the sprites array to avoid reallocating it on every call
    private final TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
    private final Object2ObjectOpenHashMap<ResourceLocation, TextureAtlasSprite> spriteCache = new Object2ObjectOpenHashMap<>();

    private TextureAtlasSprite getTexture(ResourceLocation identifier) {
        TextureAtlasSprite sprite = spriteCache.get(identifier);

        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(identifier);
            spriteCache.put(identifier, sprite);
        }

        return sprite;
    }

    public TextureAtlasSprite[] getSprites(BlockAndTintGetter world, BlockPos pos, FluidState fluidState) {
        //? if >=1.19 {
        IClientFluidTypeExtensions fluidExt = IClientFluidTypeExtensions.of(fluidState);
        sprites[0] = getTexture(fluidExt.getStillTexture(fluidState, world, pos));
        sprites[1] = getTexture(fluidExt.getFlowingTexture(fluidState, world, pos));
        ResourceLocation overlay = fluidExt.getOverlayTexture(fluidState, world, pos);
        //?} else {
        /*var attrs = fluidState.getType().getAttributes();
        sprites[0] = getTexture(attrs.getStillTexture(world, pos));
        sprites[1] = getTexture(attrs.getFlowingTexture(world, pos));
        ResourceLocation overlay = attrs.getOverlayTexture();
        *///?}
        sprites[2] = overlay != null ? getTexture(overlay) : null;
        return sprites;
    }
}
//?}
