package org.embeddedt.embeddium.impl.mixin.core.gui;

//? if >=1.18 {
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if >=1.21.9 {
/*@Mixin(targets = "net.minecraft.client.multiplayer.LevelLoadTracker$WaitingForPlayerChunk")
*///?} else if >=1.20.3 {
/*@Mixin(net.minecraft.client.multiplayer.LevelLoadStatusManager.class)
*///?} else
@Mixin(net.minecraft.client.gui.screens.ReceivingLevelScreen.class)
public class DownloadingTerrainScreenMixin {
    @Redirect(method = {
            /*? if >=1.21.9 {*/ /*"isReady" *//*?} else {*/ "tick" /*?}*/
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos redirect$getPlayerBlockPosition(LocalPlayer instance) {
        // Ensure the "eye" position (which the chunk rendering code is actually concerned about) is used instead of
        // the "feet" position. This solves a problem where the loading screen can become stuck waiting for the chunk
        // at the player's feet to load, when it is determined to not be visible due to the true location of the
        // player's eyes.
        //? if >=1.20 {
        return BlockPos.containing
        //?} else
        //return new BlockPos
                (instance.getX(), instance.getEyeY(), instance.getZ());
    }
}
//?}