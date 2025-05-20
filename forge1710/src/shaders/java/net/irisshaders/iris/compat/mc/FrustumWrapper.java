package net.irisshaders.iris.compat.mc;

import net.irisshaders.iris.shadows.frustum.MCAABB;
import net.irisshaders.iris.shadows.frustum.MCFrustum;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.util.AxisAlignedBB;

public class FrustumWrapper extends Frustrum implements MCFrustum {
    @Override
    public void prepare(double camX, double camY, double camZ) {
        setPosition(camX, camY, camZ);
    }

    @Override
    public boolean isVisible(MCAABB aabb) {
        return isBoundingBoxInFrustum((AxisAlignedBB) aabb);
    }
}