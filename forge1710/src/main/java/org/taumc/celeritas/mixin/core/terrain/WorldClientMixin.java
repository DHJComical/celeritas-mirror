package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.multiplayer.WorldClient;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldClient.class)
public class WorldClientMixin implements ChunkTrackerHolder {
    private final ChunkTracker celeritas$tracker = new ChunkTracker();

    @Override
    public ChunkTracker sodium$getTracker() {
        return celeritas$tracker;
    }
}
