package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.FrustumCuller;

import net.minecraft.client.render.FrustumData;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
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
    private double x, y, z;

    @Override
    public Viewport sodium$createViewport() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.set(frustum.modelMatrix);
        modelMatrix.invert();
        Vector3f offset = new Vector3f();
        modelMatrix.transformPosition(offset);
        //? if >=1.3 {
        /*Frustum cullTester = (minX, minY, minZ, maxX, maxY, maxZ) -> this.frustum.contains(minX, minY, minZ, maxX, maxY, maxZ);
        *///?} else
        Frustum cullTester = (minX, minY, minZ, maxX, maxY, maxZ) -> this.frustum.m_9750073(minX, minY, minZ, maxX, maxY, maxZ);
        return new Viewport(cullTester,
                new org.joml.Vector3d(this.x + offset.x, this.y + offset.y, this.z + offset.z));
    }
}

