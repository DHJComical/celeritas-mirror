package org.embeddedt.embeddium.impl.mixin.core.gui;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.multiplayer.LevelLoadTracker$WaitingForPlayerChunk")
public class DownloadingTerrainScreenMixin {
    @Redirect(method = "isReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos redirect$getPlayerBlockPosition(LocalPlayer instance) {
        // Wait on the section containing the camera, which is always visible and therefore scheduled for building.
        // The section at the player's feet may not be visible, and so would never be built.
        return BlockPos.containing(instance.getX(), instance.getEyeY(), instance.getZ());
    }
}
