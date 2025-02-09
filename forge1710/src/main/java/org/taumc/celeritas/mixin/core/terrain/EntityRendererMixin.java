package org.taumc.celeritas.mixin.core.terrain;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /**
     * @author embeddedt
     * @reason fix the frustum being positioned at the player's feet
     */
    @ModifyVariable(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;lastTickPosZ:D", ordinal = 0), ordinal = 1)
    private double adjustYForEyeHeight(double old, @Local(ordinal = 0) EntityLivingBase entity) {
        return old + entity.getEyeHeight();
    }
}
