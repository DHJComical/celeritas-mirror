package org.embeddedt.embeddium.impl.mixin.features.world.biome;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Biome.class, priority = 800)
public abstract class BiomeMixin {
    @Shadow
    @Final
    private BiomeSpecialEffects specialEffects;

    @Shadow
    private int getFoliageColorFromTexture() {
        throw new AssertionError();
    }

    @Shadow
    private int getDryFoliageColorFromTexture() {
        throw new AssertionError();
    }

    /**
     * @author embeddedt
     * @reason Avoid allocating a method reference for orElseGet on every call
     */
    @Overwrite
    public int getFoliageColor() {
        var override = this.specialEffects.foliageColorOverride();
        return override.isPresent() ? override.get() : this.getFoliageColorFromTexture();
    }

    /**
     * @author embeddedt
     * @reason Avoid allocating a method reference for orElseGet on every call
     */
    @Overwrite
    public int getDryFoliageColor() {
        var override = this.specialEffects.dryFoliageColorOverride();
        return override.isPresent() ? override.get() : this.getDryFoliageColorFromTexture();
    }
}
