package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.FrustumCuller;

import net.minecraft.client.render.FrustumData;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FrustumCuller.class)
public class FrustumCullerMixin implements ViewportProvider {
    @Shadow
    @Final
    private FrustumData frustum;

    @Shadow
    private double offsetX, offsetY, offsetZ;

    @Override
    public Viewport sodium$createViewport() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.set(frustum.modelMatrix);
        modelMatrix.invert();
        Vector3f offset = new Vector3f();
        modelMatrix.transformPosition(offset);
        return new Viewport(((minX, minY, minZ, maxX, maxY, maxZ) -> this.frustum.intersects(minX, minY, minZ, maxX, maxY, maxZ)),
                new org.joml.Vector3d(this.offsetX + offset.x, this.offsetY + offset.y, this.offsetZ + offset.z));
    }
}

