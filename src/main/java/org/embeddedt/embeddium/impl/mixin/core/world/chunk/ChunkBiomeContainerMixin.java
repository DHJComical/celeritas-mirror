package org.embeddedt.embeddium.impl.mixin.core.world.chunk;

//? if <1.18 {

/*import org.embeddedt.embeddium.impl.world.ChunkBiomeContainerExtended;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBiomeContainer.class)
public abstract class ChunkBiomeContainerMixin implements ChunkBiomeContainerExtended {
    //? if >=1.16.2 {
    @Shadow
    @Final
    private IdMap<Biome> biomeRegistry;

    @Shadow
    public abstract int[] writeBiomes();
    //?} else {
    /^@Shadow
    @Final
    private Biome[] biomes;
    ^///?}

    @Override
    public ChunkBiomeContainer embeddium$copy() {
        //? if >=1.16.2 {
        int[] biomeIds = this.writeBiomes();
        return new ChunkBiomeContainer(this.biomeRegistry, biomeIds);
        //?} else {
        /^return new ChunkBiomeContainer(this.biomes.clone());
        ^///?}
    }
}
*///?}
