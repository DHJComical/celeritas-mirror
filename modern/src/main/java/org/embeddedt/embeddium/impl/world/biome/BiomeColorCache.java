package org.embeddedt.embeddium.impl.world.biome;

//? if >=1.18
import net.minecraft.core.Holder;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

//? if >=1.18 {
public class BiomeColorCache extends org.embeddedt.embeddium.impl.biome.BiomeColorCache<Holder<Biome>, ColorResolver> {
    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        super(biomeData::getBiome, blendRadius);
    }

    @Override
    protected int resolveColor(ColorResolver colorResolver, Holder<Biome> biomeHolder, int relativeX, int relativeY, int relativeZ) {
        return colorResolver.getColor(biomeHolder.value(), relativeX, relativeZ);
    }
}
//?} else {
/*public class BiomeColorCache extends org.embeddedt.embeddium.impl.biome.BiomeColorCache<Biome, ColorResolver> {
    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        super(biomeData::getBiome, blendRadius);
    }

    @Override
    protected int resolveColor(ColorResolver colorResolver, Biome biomeHolder, int relativeX, int relativeY, int relativeZ) {
        return colorResolver.getColor(biomeHolder, relativeX, relativeZ);
    }
}
*///?}
