package org.embeddedt.embeddium.impl.gl.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import org.embeddedt.embeddium.impl.gl.state.GlStateService;
import org.embeddedt.embeddium.impl.mixin.core.GlStateManagerAccessor;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;

public final class ModernGlStateService implements GlStateService {
    public static final ModernGlStateService INSTANCE = new ModernGlStateService();

    // GlStateManager.TEXTURES only covers 2D textures on these units. Iris enlarges it.
    private static final int CACHED_TEXTURE_UNITS = GlStateManagerAccessor.celeritas$getTextures().length;

    private ModernGlStateService() {
    }

    @Override
    public int activateTextureUnit(int unit) {
        int previous = GL13C.GL_TEXTURE0 + GlStateManagerAccessor.celeritas$getActiveTexture();
        GlStateManager._activeTexture(unit);
        return previous;
    }

    @Override
    public void bindTexture(int unit, int target, int texture) {
        int previous = this.activateTextureUnit(GL13C.GL_TEXTURE0 + unit);

        if (target == GL11C.GL_TEXTURE_2D && unit < CACHED_TEXTURE_UNITS) {
            GlStateManager._bindTexture(texture);
        } else {
            GL11C.glBindTexture(target, texture);
        }

        this.activateTextureUnit(previous);
    }
}
