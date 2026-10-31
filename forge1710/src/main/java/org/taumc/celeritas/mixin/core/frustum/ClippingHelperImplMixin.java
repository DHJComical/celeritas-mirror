package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.frustum.IClippingHelper;

@Mixin(ClippingHelperImpl.class)
public abstract class ClippingHelperImplMixin extends ClippingHelper implements IClippingHelper {
    @Unique
    private final FrustumIntersection celeritas$frustum = new FrustumIntersection();

    @Inject(method = "init", at = @At("RETURN"))
    private void updateJoml(CallbackInfo ci) {
        Matrix4f jomlProjection = new Matrix4f();
        jomlProjection.set(projectionMatrix);
        Matrix4f jomlModelview = new Matrix4f();
        jomlModelview.set(modelviewMatrix);
        this.celeritas$frustum.set(jomlProjection.mul(jomlModelview), true);
    }

    @Override
    public FrustumIntersection celeritas$getJomlFrustum() {
        return this.celeritas$frustum;
    }
}
