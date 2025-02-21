package org.embeddedt.embeddium.impl.mixin.core.render.frustum;

//? if <1.20
/*import com.mojang.math.Matrix4f;*/
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.embeddedt.embeddium.impl.loader.common.LoaderServices;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    //? if >=1.20 {
    @Shadow
    @Final
    private FrustumIntersection intersection;

    @Shadow
    @Final
    private org.joml.Matrix4f matrix;
    //?} else {
    /*@Unique
    private FrustumIntersection intersection;

    @Inject(method = "<init>(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;)V", at = @At("RETURN"))
    private void initFrustum(Matrix4f pProjection, Matrix4f pFrustum, CallbackInfo ci) {
        this.intersection = new FrustumIntersection(JomlHelper.copy(pFrustum).mul(JomlHelper.copy(pProjection)), false);
    }

    *///?}

    //? if >=1.18 <1.20 {
    /*@Inject(method = "<init>(Lnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("RETURN"))
    private void copyFrustum(Frustum pOther, CallbackInfo ci) {
        this.intersection = ((FrustumMixin)(Object)pOther).intersection;
    }
    *///?}

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new SimpleFrustum(this.intersection), new Vector3d(this.camX, this.camY, this.camZ));
    }

    //? if <1.20 {
    /*@Shadow public boolean cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) { throw new AssertionError(); }

    @Overwrite
    public boolean isVisible(AABB box) {
        if(!LoaderServices.INSTANCE.isCullableAABB(box))
            return true;
        return this.cubeInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    @Overwrite
    private boolean cubeInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.intersection.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    *///?}

    //? if forge && >=1.19 {
    /**
     * @author embeddedt
     * @reason Avoid AABB#equals in hot path, turns out Double.compare can be slow
     */
    @Redirect(method = "isVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;equals(Ljava/lang/Object;)Z"))
    private boolean checkInfinite(AABB aabb, Object infinite) {
            return aabb == net.minecraftforge.common.extensions.IForgeBlockEntity.INFINITE_EXTENT_AABB
                    || (Double.isInfinite(aabb.minX) && Double.isInfinite(aabb.minY) && Double.isInfinite(aabb.minZ) && Double.isInfinite(aabb.maxX) && Double.isInfinite(aabb.maxY) && Double.isInfinite(aabb.maxZ));
    }
    //?}

}
