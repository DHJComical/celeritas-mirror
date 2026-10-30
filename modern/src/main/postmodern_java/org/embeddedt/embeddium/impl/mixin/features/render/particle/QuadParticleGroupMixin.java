package org.embeddedt.embeddium.impl.mixin.features.render.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
    private boolean celeritas$checkSectionVisibility(Frustum frustum, double x, double y, double z, Operation<Boolean> original) {
        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer != null && !renderer.isPointVisible(x, y, z)) {
            return false;
        }

        return original.call(frustum, x, y, z);
    }
}
