package org.taumc.celeritas.mixin.shaders;

import net.irisshaders.iris.shadows.frustum.MCAABB;
import net.irisshaders.iris.shadows.frustum.MCFrustum;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.util.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Frustrum.class)
public class MixinFrustrum_Shaders implements MCFrustum {
    @Shadow public void setPosition(double camX, double camY, double camZ) {
        throw new IllegalStateException("Mixin shadow method should not be called");
    }

    @Shadow public boolean isBoundingBoxInFrustum(AxisAlignedBB aabb) {
        throw new IllegalStateException("Mixin shadow method should not be called");
    }

    @Override
    public void prepare(double camX, double camY, double camZ) {
        setPosition(camX, camY, camZ);
    }

    @Override
    public boolean isVisible(MCAABB aabb) {
        return isBoundingBoxInFrustum((AxisAlignedBB) aabb);
    }
}
