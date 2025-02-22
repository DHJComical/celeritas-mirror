package org.taumc.celeritas.mixin.core.terrain;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /**
     * @author embeddedt
     * @reason fix the frustum being positioned at the player's feet
     */
    @ModifyVariable(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;lastTickPosZ:D", ordinal = 0), ordinal = 1)
    private double adjustYForEyeHeight(double old, @Local(ordinal = 0) Entity entity) {
        return old + entity.getEyeHeight();
    }
}
