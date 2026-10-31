package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumData;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.frustum.IFrustumData;

@Mixin(Frustum.class)
public abstract class FrustumMixin extends FrustumData implements IFrustumData {
    @Unique
    private final FrustumIntersection celeritas$frustum = new FrustumIntersection();

    @Inject(method = "compute", at = @At("RETURN"))
    private void updateJoml(CallbackInfo ci) {
        Matrix4f jomlProjection = new Matrix4f();
        jomlProjection.set(projectionMatrix);
        Matrix4f jomlModelview = new Matrix4f();
        jomlModelview.set(modelMatrix);
        this.celeritas$frustum.set(jomlProjection.mul(jomlModelview), true);
    }

    @Override
    public FrustumIntersection celeritas$getJomlFrustum() {
        return this.celeritas$frustum;
    }
}
