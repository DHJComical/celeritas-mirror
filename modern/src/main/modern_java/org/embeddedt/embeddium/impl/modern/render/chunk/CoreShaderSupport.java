package org.embeddedt.embeddium.impl.modern.render.chunk;

//? if >=1.17 {
import com.mojang.blaze3d.systems.RenderSystem;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;

public class CoreShaderSupport {
    public static GlProgram<ChunkShaderInterface> createCeleritasCoreShader(ChunkShaderOptions options) {
        String shaderName = "rendertype_" + options.pass().name();

        throw new UnsupportedOperationException("incomplete");
    }
}
//?}