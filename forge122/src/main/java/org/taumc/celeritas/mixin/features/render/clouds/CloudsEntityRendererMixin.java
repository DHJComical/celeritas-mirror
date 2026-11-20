package org.taumc.celeritas.mixin.features.render.clouds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.taumc.celeritas.impl.render.clouds.VintageCloudRenderer;

/**
 * Pushes the far plane of the world projection out to the cloud range, as vanilla does since 1.21.6, so that clouds
 * past the terrain render distance aren't clipped. These are the calls that set up the terrain projection, which
 * {@link net.minecraft.client.renderer.ActiveRenderInfo} captures and clouds are drawn with.
 */
@Mixin(EntityRenderer.class)
public class CloudsEntityRendererMixin {
    @Shadow
    @Final
    private Minecraft mc;

    @ModifyArg(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 0, remap = false), index = 3)
    private float celeritas$extendFarPlaneForClouds(float zFar) {
        return this.celeritas$getFarPlane(zFar);
    }

    // Restores the terrain projection after the sky
    @ModifyArg(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 1, remap = false), index = 3)
    private float celeritas$extendFarPlaneForCloudsAfterSky(float zFar) {
        return this.celeritas$getFarPlane(zFar);
    }

    // Restores the terrain projection after clouds
    @ModifyArg(method = "renderCloudsCheck", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", ordinal = 1, remap = false), index = 3)
    private float celeritas$extendFarPlaneForCloudsAfterClouds(float zFar) {
        return this.celeritas$getFarPlane(zFar);
    }

    @Unique
    private float celeritas$getFarPlane(float zFar) {
        if (!VintageCloudRenderer.isEnabled() || this.mc.gameSettings.shouldRenderClouds() == 0) {
            return zFar;
        }

        return Math.max(zFar, VintageCloudRenderer.getCloudRange());
    }
}
