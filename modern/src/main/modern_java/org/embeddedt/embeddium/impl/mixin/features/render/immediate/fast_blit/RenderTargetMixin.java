package org.embeddedt.embeddium.impl.mixin.features.render.immediate.fast_blit;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public class RenderTargetMixin {
    @Shadow
    public int frameBufferId;

    @Shadow
    public int width;

    @Shadow
    public int height;

    /**
     * @author embeddedt
     * @reason For non-blends, do a straight blit using glBlitNamedFramebuffer instead of invoking the shader
     * (backport of 24w34a change)
     */
    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
    private void blitUsingGlBlit(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (disableBlend) {
            ci.cancel();

            // Apply important side effects to the FFP state that vanilla would have applied.
            // This fixes the panorama not showing on the title screen in 1.16.5.
            GlStateManager._disableBlend();
            GlStateManager._disableDepthTest();

            GlStateManager._glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, this.width, this.height, 0, 0, width, height, GL32C.GL_COLOR_BUFFER_BIT, GL32C.GL_NEAREST);
            GlStateManager._glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
        }
    }
}
