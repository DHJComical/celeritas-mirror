package org.taumc.celeritas.mixin.core;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.3 {
/*@Mixin(net.minecraft.client.world.chunk.ClientChunkCache.class)
*///?} else
@Mixin(net.minecraft.client.world.chunk.MultiplayerChunkCache.class)
public class MultiplayerChunkCacheMixin {
    @Shadow
    private World world;

    @Inject(method = "generateChunk", at = @At("RETURN"))
    private void afterLoadChunkFromPacket(int x, int z, CallbackInfoReturnable<WorldChunk> cir) {
        ChunkTrackerHolder.get(this.world).onChunkStatusAdded(x, z, ChunkStatus.FLAG_ALL);
    }

    @Inject(method = "unloadChunk", at = @At("RETURN"))
    private void afterUnloadChunk(int x, int z, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.world).onChunkStatusRemoved(x, z, ChunkStatus.FLAG_ALL);
    }
}
