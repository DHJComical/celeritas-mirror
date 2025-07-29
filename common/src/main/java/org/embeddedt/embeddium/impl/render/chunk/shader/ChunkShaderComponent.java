package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;

import java.util.Collection;
import java.util.List;

public interface ChunkShaderComponent {
    void setup();

    interface Factory<T extends ChunkShaderComponent> {
        T create(ShaderBindingContext context);

        default Collection<String> getDefines() {
            return List.of();
        }
    }
}
