package org.embeddedt.embeddium.impl.mixin.features.render.entity.cull;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.embeddedt.embeddium.impl.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0))
    private boolean checkSectionForCullingMain(boolean isWithinFrustum, @Local(ordinal = 0, argsOnly = true) T entity) {
        if(!isWithinFrustum) {
            return false;
        }

        var renderer = SodiumWorldRenderer.instanceNullable();

        return renderer == null || renderer.isEntityVisible(entity);
    }
}
