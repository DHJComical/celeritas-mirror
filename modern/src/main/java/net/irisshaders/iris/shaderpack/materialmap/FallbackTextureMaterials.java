package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public record FallbackTextureMaterials(Object2ObjectMap<TextureAtlasSprite, Object2IntFunction<BlockState>> fallbackMaterialMap, Set<Block> blacklistedBlocks) {
    public int getFallbackId(TextureAtlasSprite sprite, BlockState state) {
        if (blacklistedBlocks.contains(state.getBlock())) {
            return -1;
        }

        var byPropertyMap = fallbackMaterialMap.get(sprite);

        if (byPropertyMap == null) {
            return -1;
        }

        return byPropertyMap.applyAsInt(state);
    }

    @Override
    public String toString() {
        return "texture materials for " + fallbackMaterialMap.size() + " sprites with " + blacklistedBlocks.size() + " blocks blacklisted";
    }
}
