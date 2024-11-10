package org.embeddedt.embeddium.impl.mixin.core.world.biome;

import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.world.BiomeSeedProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientWorldMixin implements BiomeSeedProvider {
    @Unique
    private long biomeSeed;

    //? if >=1.16 {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) long seed) {
        this.biomeSeed = seed;
    }
    //?} else {
    /*@Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(CallbackInfo ci) {
        this.biomeSeed = ((BiomeManagerAccessor)((ClientLevel)(Object)this).getBiomeManager()).getBiomeZoomSeed();
    }
    *///?}

    @Override
    public long sodium$getBiomeSeed() {
        return this.biomeSeed;
    }
}