package org.embeddedt.embeddium.impl.mixin.features.render.entity.cull;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//? if >=1.21 {
/*import net.minecraft.world.entity.Leashable;
*///?}
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    //? if >=1.21.5 {
    /*@Shadow
    protected abstract AABB getBoundingBoxForCulling(Entity par1);
    *///?} else {
    private AABB getBoundingBoxForCulling(Entity entity) {
        return entity.getBoundingBoxForCulling();
    }
    //?}

    @ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0))
    private boolean checkSectionForCullingMain(boolean isWithinFrustum, @Local(ordinal = 0, argsOnly = true) T entity) {
        if(!isWithinFrustum) {
            return false;
        }

        var renderer = CeleritasWorldRenderer.instanceNullable();

        return renderer == null || renderer.isEntityVisible(entity, this.getBoundingBoxForCulling(entity));
    }

    //? if >=1.21 {
    /*@ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 1))
    private boolean checkSectionForCullingMain(boolean isWithinFrustum, @Local(ordinal = 0) Leashable leashable) {
        if(!isWithinFrustum) {
            return false;
        }

        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer == null) {
            return false;
        }

        var leashHolder = leashable.getLeashHolder();

        var boundingBox = ((EntityRendererMixin<?>)(Object)this.entityRenderDispatcher.getRenderer(leashHolder)).getBoundingBoxForCulling(leashHolder);

        return renderer.isEntityVisible(leashable.getLeashHolder(), boundingBox);
    }
    *///?}
}
