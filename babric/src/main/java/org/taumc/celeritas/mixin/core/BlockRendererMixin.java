package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.BlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockRenderer.class)
public class BlockRendererMixin {
    @Redirect(method = { "tessellateWithMaxAmbientOcclusion", "tessellateWithoutAmbientOcclusion" }, at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/BlockRenderer;fancyGraphics:Z"))
    private boolean applyFancyGraphicsUnconditionally() {
        return true;
    }
}
