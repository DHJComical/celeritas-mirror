package org.embeddedt.embeddium.impl.world;

//? if <1.18 {
/*import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public interface ChunkBiomeContainerExtended {
    static ChunkBiomeContainer clone(ChunkBiomeContainer container) {
        return container != null ? ((ChunkBiomeContainerExtended)container).embeddium$copy() : null;
    }

    ChunkBiomeContainer embeddium$copy();
}
*///?} else {
public interface ChunkBiomeContainerExtended {}
//?}
