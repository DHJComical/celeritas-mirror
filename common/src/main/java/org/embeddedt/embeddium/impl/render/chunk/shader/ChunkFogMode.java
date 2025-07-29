package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.lwjgl.opengl.GL20;

import java.util.List;
import java.util.function.Function;

public enum ChunkFogMode implements ChunkShaderComponent.Factory<ChunkShaderFogComponent> {
    NONE(ChunkShaderFogComponent.None::new, List.of()),
    EXP2(ChunkShaderFogComponent.Exp2::new, List.of("USE_FOG", "USE_FOG_EXP2")),
    SMOOTH(ChunkShaderFogComponent.Smooth::new, List.of("USE_FOG", "USE_FOG_SMOOTH"));

    private final Function<ShaderBindingContext, ChunkShaderFogComponent> factory;
    private final List<String> defines;

    ChunkFogMode(Function<ShaderBindingContext, ChunkShaderFogComponent> factory, List<String> defines) {
        this.factory = factory;
        this.defines = defines;
    }

    @Override
    public ChunkShaderFogComponent create(ShaderBindingContext context) {
        return factory.apply(context);
    }

    public List<String> getDefines() {
        return this.defines;
    }

    public static ChunkFogMode fromGLMode(int mode) {
        switch (mode) {
            case 0:
                return ChunkFogMode.NONE;
            case GL20.GL_EXP2:
            case GL20.GL_EXP:
                return ChunkFogMode.EXP2;
            case GL20.GL_LINEAR:
                return ChunkFogMode.SMOOTH;
            default:
                throw new UnsupportedOperationException("Unknown fog mode: " + mode);
        }
    }
}
