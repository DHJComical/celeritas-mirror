package org.taumc.celeritas.mixin.features.render.tileentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRenderDispatcherMixin {
    /**
     * @author embeddedt
     * @reason Allow some invalid TEs to still be rendered. Modern vanilla
     * versions do this for all TEs, but we cannot do that here as mods may rely on their TESR not being invoked
     * when the TE is invalid.
     */
    @WrapOperation(method = "getRenderer(Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntity;isInvalid()Z"))
    private boolean allowSomeInvalidTESRs(TileEntity te, Operation<Boolean> original) {
        if (te instanceof TileEntityPiston) {
            // Pistons invalidate their TE before the chunk renderer has finished the chunk update
            return false;
        }
        return original.call(te);
    }
}
