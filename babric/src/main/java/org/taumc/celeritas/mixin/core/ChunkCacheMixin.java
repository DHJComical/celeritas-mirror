package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkCache.class)
public class ChunkCacheMixin {
    @Shadow
    private World world;

    @Inject(method = "loadChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkCache;loadChunkFromStorage(II)Lnet/minecraft/world/chunk/Chunk;"))
    private void markLoaded(int x, int z, CallbackInfoReturnable<Chunk> cir, @Share("loaded") LocalBooleanRef didLoad) {
        didLoad.set(true);
    }

    @Inject(method = "loadChunk", at = @At("RETURN"))
    private void sendLoadEventIfLoaded(int x, int z, CallbackInfoReturnable<Chunk> cir, @Share("loaded") LocalBooleanRef didLoad) {
        if (didLoad.get()) {
            ChunkTrackerHolder.get(this.world).onChunkStatusAdded(x, z, ChunkStatus.FLAG_ALL);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;unload()V", shift = At.Shift.AFTER))
    private void sendUnloadEvent(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) Chunk chunk) {
        ChunkTrackerHolder.get(this.world).onChunkStatusRemoved(chunk.x, chunk.z, ChunkStatus.FLAG_ALL);
    }
}
