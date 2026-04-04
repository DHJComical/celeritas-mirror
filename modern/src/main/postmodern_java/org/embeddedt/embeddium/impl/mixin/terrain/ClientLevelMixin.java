package org.embeddedt.embeddium.impl.mixin.terrain;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTracker;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements ChunkTrackerHolder {
    @Unique
    private final ChunkTracker tracker = new ChunkTracker();

    @Override
    public ChunkTracker sodium$getTracker() {
        return tracker;
    }

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void markLoaded(ChunkPos pChunkPos, CallbackInfo ci) {
        this.tracker.onChunkStatusAdded(
                //? if <26.1 {
                pChunkPos.x, pChunkPos.z,
                //?} else
                /*pChunkPos.x(), pChunkPos.z(),*/
                ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }
}
