package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustrum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.taumc.celeritas.impl.render.frustum.IClippingHelper;
import org.taumc.celeritas.impl.render.terrain.CameraHelper;

@Mixin(Frustrum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    private ClippingHelper clippingHelper;

    @Shadow
    private double xPosition, yPosition, zPosition;

    /**
     * @author embeddedt
     * @reason Use the faster JOML frustum test
     */
    @Overwrite
    public boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(minZ) || Double.isInfinite(maxX) || Double.isInfinite(maxY) || Double.isInfinite(maxZ)) {
            return true;
        }
        return ((IClippingHelper)clippingHelper).celeritas$getJomlFrustum().testAab((float) (minX - this.xPosition), (float) (minY - this.yPosition), (float) (minZ - this.zPosition), (float) (maxX - this.xPosition), (float) (maxY - this.yPosition), (float) (maxZ - this.zPosition));
    }

    @Override
    public Viewport sodium$createViewport() {
        Vector3f eyeOffset = CameraHelper.getThirdPersonOffset();
        // The clipping helper's modelview expects positions relative to the entity's feet, but the viewport
        // passes positions relative to the eye.
        Matrix4f viewProjection = new Matrix4f().set(clippingHelper.projectionMatrix)
                .mul(new Matrix4f().set(clippingHelper.modelviewMatrix))
                .translate(eyeOffset);
        FrustumIntersection frustum = new FrustumIntersection(viewProjection, true);
        return new Viewport(frustum::testAab, new Vector3d(this.xPosition, this.yPosition, this.zPosition).add(eyeOffset));
    }
}
