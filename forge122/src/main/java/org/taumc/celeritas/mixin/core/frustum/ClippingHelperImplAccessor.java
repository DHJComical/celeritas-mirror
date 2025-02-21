package org.taumc.celeritas.mixin.core.frustum;

import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

@Mixin(ClippingHelperImpl.class)
public interface ClippingHelperImplAccessor {
    @Accessor
    FloatBuffer getProjectionMatrixBuffer();
    @Accessor
    FloatBuffer getModelviewMatrixBuffer();
}
