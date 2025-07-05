package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.terrain.CameraHelper;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    @Final
    private ClippingHelper clippingHelper;

    @Shadow
    private double x, y, z;

    @Unique
    private FrustumIntersection celeritas$frustum;

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/culling/ClippingHelper;)V", at = @At("RETURN"))
    private void onFrustumInit(CallbackInfo ci) {
        Matrix4f frustumMatrix = new Matrix4f();
        frustumMatrix.set(clippingHelper.projectionMatrix);
        Matrix4f modelviewMatrix = new Matrix4f();
        modelviewMatrix.set(clippingHelper.modelviewMatrix);
        this.celeritas$frustum = new FrustumIntersection(frustumMatrix.mul(modelviewMatrix));
    }

    @Overwrite
    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(minZ) || Double.isInfinite(maxX) || Double.isInfinite(maxY) || Double.isInfinite(maxZ)) {
            return true;
        }
        return this.celeritas$frustum.testAab((float) (minX - this.x), (float) (minY - this.y), (float) (minZ - this.z), (float) (maxX - this.x), (float) (maxY - this.y), (float) (maxZ - this.z));
    }

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(this.celeritas$frustum::testAab, new org.joml.Vector3d(this.x, this.y, this.z).add(CameraHelper.getThirdPersonOffset()));
    }
}
