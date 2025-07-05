package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustrum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.render.terrain.CameraHelper;

@Mixin(Frustrum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    @Final
    private ClippingHelper clippingHelper;

    @Shadow
    private double xPosition, yPosition, zPosition;

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(((minX, minY, minZ, maxX, maxY, maxZ) -> this.clippingHelper.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)),
                new org.joml.Vector3d(this.xPosition, this.yPosition, this.zPosition).add(CameraHelper.getThirdPersonOffset()));
    }
}
