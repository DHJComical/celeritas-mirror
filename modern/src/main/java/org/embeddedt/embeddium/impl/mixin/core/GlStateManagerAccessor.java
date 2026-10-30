package org.embeddedt.embeddium.impl.mixin.core;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor {
    /**
     * {@return the cached active texture unit, as an index rather than {@code GL_TEXTURE0 + n}}
     */
    @Accessor("activeTexture")
    static int celeritas$getActiveTexture() {
        throw new AssertionError();
    }
}
