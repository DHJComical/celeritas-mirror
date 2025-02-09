package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;

@Mixin(ActiveRenderInfo.class)
public interface ActiveRenderInfoAccessor {
    @Accessor("projection")
    static FloatBuffer getProjectionMatrix() {
        throw new AssertionError();
    }

    @Accessor("modelview")
    static FloatBuffer getModelViewMatrix() {
        throw new AssertionError();
    }
}
