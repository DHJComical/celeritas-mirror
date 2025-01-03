package net.irisshaders.iris.mixin;

public class InjectionPoints {
    public static final String PARTICLE_ENGINE_RENDER =
            "Lnet/minecraft/client/particle/ParticleEngine;render(" +
                    "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;" +
                    "Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F" +
                    //? if forgelike
                    "Lnet/minecraft/client/renderer/culling/Frustum;" +
                    ")V";
}
