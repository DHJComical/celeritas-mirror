package org.embeddedt.embeddium.impl.mixin.terrain;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"))
    private void celeritas$captureProjectionMatrix(DeltaTracker deltaTracker, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projectionMatrix) {
        CeleritasWorldRenderer.instance().setCurrentChunkRenderProjection(projectionMatrix);
    }
}
