package org.taumc.celeritas.mixin.core;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldChunk.class)
public interface WorldChunkAccessor {
    @Accessor
    boolean getHasEntities();
}
