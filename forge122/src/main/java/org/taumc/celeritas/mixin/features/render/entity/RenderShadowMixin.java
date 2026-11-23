package org.taumc.celeritas.mixin.features.render.entity;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

@Mixin(Render.class)
public abstract class RenderShadowMixin {
    @Shadow
    @Final
    protected RenderManager renderManager;

    @Shadow
    protected float shadowSize;

    @Inject(method = "renderShadow(Lnet/minecraft/entity/Entity;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void celeritas$batchShadow(Entity entity, double x, double y, double z, float shadowAlpha, float partialTicks, CallbackInfo ci) {
        CeleritasWorldRenderer renderer = CeleritasWorldRenderer.instanceNullable();
        if (renderer != null && renderer.getEntityShadowBatch().record(this.renderManager.world, entity, x, y, z, shadowAlpha, partialTicks, this.shadowSize)) {
            // Leave the state vanilla's renderShadow leaves behind, since following code may depend on it
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            ci.cancel();
        }
    }
}
