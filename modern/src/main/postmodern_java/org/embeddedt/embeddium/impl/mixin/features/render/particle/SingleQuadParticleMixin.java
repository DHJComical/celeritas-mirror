package org.embeddedt.embeddium.impl.mixin.features.render.particle;

import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SingleQuadParticle.class)
public class SingleQuadParticleMixin {
    // Particles are only extracted on the main thread, and the rotation is copied into the render state right away.
    @Unique
    private static final Quaternionf celeritas$scratchRotation = new Quaternionf();

    @Redirect(method = "extract", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;", remap = false))
    private Quaternionf celeritas$useScratchRotation() {
        return celeritas$scratchRotation.identity();
    }
}
