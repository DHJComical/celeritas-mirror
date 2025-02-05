package org.embeddedt.embeddium.impl.mixin.features.render.particle;

//? if forgelike {
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.impl.render.CeleritasWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    /**
     * @author embeddedt
     * @reason use cached section visibility information instead of going through frustum
     */
    @Redirect(method = {
            //? if forge && >=1.17
            "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"
            //? if forge && <1.17
            /*"renderParticles"*/
            //? if neoforge
            /*"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"*/
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean celeritas$useSectionVisibility(Frustum instance, AABB aabb) {
        var renderer = CeleritasWorldRenderer.instanceNullable();

        if (renderer == null) {
            return instance.isVisible(aabb);
        }

        return renderer.isBoxVisible(aabb);
    }
}
//?}