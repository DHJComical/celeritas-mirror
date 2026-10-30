package org.embeddedt.embeddium.impl.mixin.features.render.entity.cull;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @WrapOperation(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"), require = 3)
    private boolean celeritas$checkSectionVisibility(Frustum frustum, AABB box, Operation<Boolean> original, @Local(argsOnly = true) T entity) {
        if (!original.call(frustum, box)) {
            return false;
        }

        var renderer = CeleritasWorldRenderer.instanceNullable();

        return renderer == null || renderer.isEntityVisible(entity, box);
    }
}
