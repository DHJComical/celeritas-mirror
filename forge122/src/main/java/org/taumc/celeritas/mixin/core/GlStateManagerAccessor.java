package org.taumc.celeritas.mixin.core;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor {
    /**
     * {@return the cached active texture unit, as an index rather than {@code GL_TEXTURE0 + n}}
     */
    @Accessor("activeTextureUnit")
    static int celeritas$getActiveTextureUnit() {
        throw new AssertionError();
    }
}
