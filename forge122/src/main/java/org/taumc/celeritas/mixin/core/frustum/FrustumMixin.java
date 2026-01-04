package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.spongepowered.asm.mixin.*;
import org.taumc.celeritas.impl.render.frustum.IClippingHelper;
import org.taumc.celeritas.impl.render.terrain.CameraHelper;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    @Final
    private ClippingHelper clippingHelper;

    @Shadow
    private double x, y, z;

    @Overwrite
    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(minZ) || Double.isInfinite(maxX) || Double.isInfinite(maxY) || Double.isInfinite(maxZ)) {
            return true;
        }
        return ((IClippingHelper)clippingHelper).celeritas$getJomlFrustum().testAab((float) (minX - this.x), (float) (minY - this.y), (float) (minZ - this.z), (float) (maxX - this.x), (float) (maxY - this.y), (float) (maxZ - this.z));
    }

    @Override
    public Viewport sodium$createViewport() {
        var frustum = ((IClippingHelper)clippingHelper).celeritas$getJomlFrustum();
        return new Viewport(frustum::testAab, new org.joml.Vector3d(this.x, this.y, this.z).add(CameraHelper.getThirdPersonOffset()));
    }
}
