package org.embeddedt.embeddium.impl.mixin.core.render.frustum;

//? if <1.20
/*import com.mojang.math.Matrix4f;*/
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.embeddedt.embeddium.impl.render.viewport.frustum.SimpleFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    //?} else {
    /*@Unique
    private FrustumIntersection intersection;

    @Inject(method = "<init>(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;)V", at = @At("RETURN"))
    private void initFrustum(Matrix4f pProjection, Matrix4f pFrustum, CallbackInfo ci) {
        this.intersection = new FrustumIntersection(JomlHelper.copy(pFrustum).mul(JomlHelper.copy(pProjection)), false);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("RETURN"))
    private void copyFrustum(Frustum pOther, CallbackInfo ci) {
        this.intersection = ((FrustumMixin)(Object)pOther).intersection;
    }
    *///?}

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new SimpleFrustum(this.intersection), new Vector3d(this.camX, this.camY, this.camZ));
    }
}
