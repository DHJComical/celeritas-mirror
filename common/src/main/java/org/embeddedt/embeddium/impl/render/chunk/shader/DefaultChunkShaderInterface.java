package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.joml.Matrix4fc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class DefaultChunkShaderInterface implements ChunkShaderInterface {
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;

    // The additional shader components used by this program in order to setup the appropriate GL state
    private final List<? extends ChunkShaderComponent> components;

    private GlPrimitiveType primitiveType;

    public DefaultChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);

        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        if (!options.pass().hasNoLightmap()) {
            this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));
        }

        this.components = options.components().stream().map(c -> c.create(context)).toList();
    }

    @Deprecated // the shader interface should not modify pipeline state
    public void setupState(TerrainRenderPass pass) {
        if (pass.primitiveType() == QuadPrimitiveType.DIRECT) {
            primitiveType = GlPrimitiveType.QUADS;
        } else if (pass.primitiveType() == QuadPrimitiveType.TRIANGULATED) {
            primitiveType = GlPrimitiveType.TRIANGLES;
        } else {
            throw new IllegalArgumentException("Unknown primitive type");
        }

        for (var c : this.components) {
            c.setup();
        }
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }

    public void setTextureSlot(ChunkShaderTextureSlot slot, int val) {
        var uniform = this.uniformTextures.get(slot);

        if (uniform != null) {
            uniform.setInt(val);
        }
    }
}
