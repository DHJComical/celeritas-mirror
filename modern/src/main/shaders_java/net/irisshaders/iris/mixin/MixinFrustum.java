package net.irisshaders.iris.mixin;

import net.irisshaders.iris.shadows.frustum.MCFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Frustum.class)
public abstract class MixinFrustum implements MCFrustum {
}
