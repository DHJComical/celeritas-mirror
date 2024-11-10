package org.embeddedt.embeddium.impl.mixin.features.world.biome;

import org.embeddedt.embeddium.impl.world.biome.BiomeColorMaps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
//? if >=1.16
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Biome.class, priority = 800)
public abstract class BiomeMixin {
    //? if >=1.16 {
    @Shadow
    @Final
    private BiomeSpecialEffects specialEffects;
    //?}

    //? if >=1.16.2 {
    @Shadow
    @Final
    private Biome.ClimateSettings climateSettings;
    //?} else {
    /*@Shadow
    public abstract float getDownfall();
    @Shadow
    public abstract float getTemperature();
    *///?}

    @Unique
    private int defaultColorIndex;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setupColors(CallbackInfo ci) {
        this.defaultColorIndex = this.getDefaultColorIndex();
    }

    /**
     * @author JellySquid
     * @reason Avoid unnecessary allocations
     */
    @Overwrite
    public int getGrassColor(double x, double z) {
        //? if >=1.16.2 {
        var override = this.specialEffects.getGrassColorOverride().orElse(null);
        int color = override != null ? override.intValue() : BiomeColorMaps.getGrassColor(this.defaultColorIndex);

        var modifier = this.specialEffects.getGrassColorModifier();

        if (modifier != BiomeSpecialEffects.GrassColorModifier.NONE) {
            color = modifier.modifyColor(x, z, color);
        }

        return color;
        //?} else
        /*return BiomeColorMaps.getGrassColor(this.defaultColorIndex);*/
    }

    /**
     * @author JellySquid
     * @reason Avoid allocations
     */
    @Overwrite
    public int getFoliageColor() {
        //? if >=1.16.2 {
        var override = this.specialEffects.getFoliageColorOverride().orElse(null);
        return override != null ? override.intValue() : BiomeColorMaps.getFoliageColor(this.defaultColorIndex);
        //?} else
        /*return BiomeColorMaps.getFoliageColor(this.defaultColorIndex);*/
    }

    @Unique
    private int getDefaultColorIndex() {
        //? if >=1.19 {
        double temperature = Mth.clamp(this.climateSettings.temperature(), 0.0F, 1.0F);
        double humidity = Mth.clamp(this.climateSettings.downfall(), 0.0F, 1.0F);
        //?} else if >=1.16.2 {
        /*double temperature = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double humidity = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        *///?} else {
        /*double temperature = Mth.clamp(this.getTemperature(), 0.0F, 1.0F);
        double humidity = Mth.clamp(this.getDownfall(), 0.0F, 1.0F);
        *///?}

        return BiomeColorMaps.getIndex(temperature, humidity);
    }
}
