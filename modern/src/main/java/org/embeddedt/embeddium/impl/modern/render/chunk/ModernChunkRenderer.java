package org.embeddedt.embeddium.impl.modern.render.chunk;

import com.mojang.blaze3d.platform.GlStateManager;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.mixin.core.GlStateManagerAccessor;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.lwjgl.opengl.GL13C;

public class ModernChunkRenderer extends DefaultChunkRenderer {
    public static final int ATLAS_EDGE_MASK_TEXTURE_UNIT = 3;

    public ModernChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        super(device, renderPassConfiguration, FogHelper.INSTANCE);
    }

    /*
    @Override
    protected GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        if (options.vertexType() == ChunkMeshFormats.VANILLA_LIKE) {
            return CoreShaderSupport.createCeleritasCoreShader(options);
        } else {
            return super.createShader(path, options);
        }
    }
     */

    @Override
    protected boolean useBlockFaceCulling() {
        return Celeritas.options().performance.useBlockFaceCulling /*? if shaders {*/ && !net.irisshaders.iris.shadows.ShadowRenderingState.areShadowsCurrentlyBeingRendered() /*?}*/;
    }

    @Override
    protected int activateTextureUnit(int unit) {
        int previous = GL13C.GL_TEXTURE0 + GlStateManagerAccessor.celeritas$getActiveTexture();
        GlStateManager._activeTexture(unit);
        return previous;
    }

    @Override
    protected void configureShaderInterface(ChunkShaderInterface shader) {
        shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
        shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 2);
        shader.setTextureSlot(ChunkShaderTextureSlot.ATLAS_EDGE_MASK, ATLAS_EDGE_MASK_TEXTURE_UNIT);
    }
}
