package org.embeddedt.embeddium.impl.world.biome;

import net.minecraft.core.Holder;
//? if >=1.15 {
import net.minecraft.world.level.ColorResolver;
//?} else
/*import net.minecraft.client.renderer.BiomeColors.ColorResolver;*/
import net.minecraft.world.level.biome.Biome;

public class BiomeColorCache extends org.embeddedt.embeddium.impl.biome.BiomeColorCache<Holder<Biome>, ColorResolver> {
    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        super(biomeData::getBiome, blendRadius);
    }

    @Override
    protected int resolveColor(ColorResolver colorResolver, Holder<Biome> biomeHolder, int relativeX, int relativeY, int relativeZ) {
        return colorResolver.getColor(biomeHolder.value(), relativeX, relativeZ);
    }
}
