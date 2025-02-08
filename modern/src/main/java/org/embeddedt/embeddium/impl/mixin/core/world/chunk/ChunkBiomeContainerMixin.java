package org.embeddedt.embeddium.impl.mixin.core.world.chunk;

//? if <1.18 {

/*//? if >=1.17 {
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
//?}
import org.embeddedt.embeddium.impl.world.ChunkBiomeContainerExtended;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBiomeContainer.class)
public abstract class ChunkBiomeContainerMixin implements ChunkBiomeContainerExtended /^? if >=1.17 {^/, LevelHeightAccessor /^?}^/ {
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

    //? if >=1.17 {

    @Shadow
    @Final
    private int quartHeight;

    @Shadow
    @Final
    private int quartMinY;

    @Override
    public int getHeight() {
        return (this.quartHeight + 1) << 2;
    }

    @Override
    public int getMinBuildHeight() {
        return this.quartMinY << 2;
    }

    //?}

    @Override
    public ChunkBiomeContainer embeddium$copy() {
        //? if >=1.17 {
        if ((Object)this instanceof EmptyLevelChunk.EmptyChunkBiomeContainer emptyContainer) {
            return emptyContainer;
        }
        //?}
        //? if >=1.16.2 {
        int[] biomeIds = this.writeBiomes();
        return new ChunkBiomeContainer(this.biomeRegistry, /^? if >=1.17 {^/ this, /^?}^/ biomeIds);
        //?} else {
        /^return new ChunkBiomeContainer(this.biomes.clone());
        ^///?}
    }
}
*///?}
