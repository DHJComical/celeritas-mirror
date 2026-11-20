package org.embeddedt.embeddium.impl.mixin.features.render.world.clouds;

//? if >=1.17 && <1.21.6 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.immediate.ModernCloudRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Pushes the far plane out to the cloud range, as vanilla does since 1.21.6, so that clouds past the terrain
 * render distance aren't clipped.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyReturnValue(method = "getDepthFar", at = @At("RETURN"))
    private float celeritas$extendDepthFarForClouds(float depthFar) {
        if (!Celeritas.options().performance.useFasterClouds || this.minecraft.options.getCloudsType() == CloudStatus.OFF) {
            return depthFar;
        }

        return Math.max(depthFar, ModernCloudRenderer.getCloudRange());
    }
}
//?}
