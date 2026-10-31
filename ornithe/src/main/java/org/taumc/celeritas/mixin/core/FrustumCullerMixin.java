package org.taumc.celeritas.mixin.core;

import net.minecraft.client.render.FrustumCuller;

import net.minecraft.client.render.FrustumData;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.taumc.celeritas.impl.render.frustum.IFrustumData;

@Mixin(FrustumCuller.class)
public class FrustumCullerMixin implements ViewportProvider {
    @Shadow
    @Final
    private FrustumData frustum;

    @Shadow
    private double x, y, z;

    /**
     * @author embeddedt
     * @reason Use the faster JOML frustum test
     */
    @Overwrite
    //? if >=1.3 {
    //public boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    //?} else
    public boolean m_7557245(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (Double.isInfinite(minX) || Double.isInfinite(minY) || Double.isInfinite(minZ) || Double.isInfinite(maxX) || Double.isInfinite(maxY) || Double.isInfinite(maxZ)) {
            return true;
        }
        return ((IFrustumData)frustum).celeritas$getJomlFrustum().testAab((float) (minX - this.x), (float) (minY - this.y), (float) (minZ - this.z), (float) (maxX - this.x), (float) (maxY - this.y), (float) (maxZ - this.z));
    }

    @Override
    public Viewport sodium$createViewport() {
        Matrix4f modelMatrix = new Matrix4f().set(frustum.modelMatrix);
        Vector3f eyeOffset = new Matrix4f(modelMatrix).invert().transformPosition(new Vector3f());
        // The frustum's modelview expects positions relative to the entity's feet, but the viewport passes positions
        // relative to the eye.
        Matrix4f viewProjection = new Matrix4f().set(frustum.projectionMatrix)
                .mul(modelMatrix)
                .translate(eyeOffset);
        FrustumIntersection jomlFrustum = new FrustumIntersection(viewProjection, true);
        return new Viewport(jomlFrustum::testAab, new Vector3d(this.x, this.y, this.z).add(eyeOffset));
    }
}
