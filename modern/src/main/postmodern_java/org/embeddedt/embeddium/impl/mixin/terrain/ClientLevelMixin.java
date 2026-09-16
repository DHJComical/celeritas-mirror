package org.embeddedt.embeddium.impl.mixin.terrain;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
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
                //pChunkPos.x(), pChunkPos.z(),
                ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }

    @Inject(method = "unload", at = @At("RETURN"))
    private void markUnloaded(LevelChunk chunk, CallbackInfo ci) {
        var pos = chunk.getPos();
        this.tracker.onChunkStatusRemoved(
                //? if <26.1 {
                pos.x, pos.z,
                //?} else
                //pos.x(), pos.z(),
                ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }
}
