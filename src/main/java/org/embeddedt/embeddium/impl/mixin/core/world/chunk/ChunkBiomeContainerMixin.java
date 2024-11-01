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
    @Shadow
    @Final
    private IdMap<Biome> biomeRegistry;

    @Shadow
    public abstract int[] writeBiomes();

    @Override
    public ChunkBiomeContainer embeddium$copy() {
        int[] biomeIds = this.writeBiomes();
        return new ChunkBiomeContainer(this.biomeRegistry, biomeIds);
    }
}
*///?}
