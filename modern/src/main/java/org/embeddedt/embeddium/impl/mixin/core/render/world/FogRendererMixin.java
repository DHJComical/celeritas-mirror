package org.embeddedt.embeddium.impl.mixin.core.render.world;

//? if >=1.21.5 {

/*import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin implements FogHelper.FogDataGetter {
    private FogData celeritas$lastFogData = new FogData();
    private Vector4f celeritas$lastFogColor = new Vector4f();

    @Override
    public FogData celeritas$getLastFogData() {
        return celeritas$lastFogData;
    }

    @Override
    public Vector4f celeritas$getLastFogColor() {
        return celeritas$lastFogColor;
    }

    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceStart:F", ordinal = 0))
    private void captureFogData(CallbackInfoReturnable<Vector4f> cir, @Local(ordinal = 0) FogData fogdata) {
        this.celeritas$lastFogData = fogdata;
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void captureFogColor(CallbackInfoReturnable<Vector4f> cir) {
        this.celeritas$lastFogColor = cir.getReturnValue();
    }
}
*///?}
