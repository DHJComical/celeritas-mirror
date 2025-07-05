package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Chunk.class)
public interface ChunkAccessor {
    @Accessor("hasEntities")
    boolean celeritas$getHasEntities();
}
