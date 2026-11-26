package org.taumc.celeritas.impl.render.terrain;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.embeddedt.embeddium.impl.gl.state.GlStateService;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.mixin.core.GlStateManagerAccessor;

public final class GLStateManagerGlStateService implements GlStateService {
    public static final GLStateManagerGlStateService INSTANCE = new GLStateManagerGlStateService();

    // GlStateManager.textureState only covers 2D textures on these units
    private static final int CACHED_TEXTURE_UNITS = 8;

    private GLStateManagerGlStateService() {
    }

    @Override
    public int activateTextureUnit(int unit) {
        int previous = OpenGlHelper.defaultTexUnit + GlStateManagerAccessor.celeritas$getActiveTextureUnit();
        GlStateManager.setActiveTexture(unit);
        return previous;
    }

    @Override
    public void bindTexture(int unit, int target, int texture) {
        int previous = this.activateTextureUnit(OpenGlHelper.defaultTexUnit + unit);

        if (target == GL11.GL_TEXTURE_2D && unit < CACHED_TEXTURE_UNITS) {
            GlStateManager.bindTexture(texture);
        } else {
            GL11.glBindTexture(target, texture);
        }

        this.activateTextureUnit(previous);
    }

    @Override
    public void deleteTexture(int texture) {
        GlStateManager.deleteTexture(texture);
    }
}
