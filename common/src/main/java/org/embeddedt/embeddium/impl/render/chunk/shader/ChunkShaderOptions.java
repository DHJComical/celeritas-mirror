package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.terrain.sampling.TerrainSampling;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.VertexFetchMode;

import java.util.List;

public record ChunkShaderOptions(List<ChunkShaderComponent.Factory<?>> components, TerrainRenderPass pass, TerrainSampling sampling) {

    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        for (var component : components) {
            constants.addAll(component.getDefines());
        }

        if (this.pass.supportsFragmentDiscard()) {
            constants.add("USE_FRAGMENT_DISCARD");
        }

        if (this.pass.hasNoLightmap()) {
            constants.add("CELERITAS_NO_LIGHTMAP");
        }

        if (this.pass.vertexFetchMode() == VertexFetchMode.PULLED) {
            constants.add("USE_VERTEX_PULLING");
        }

        constants.addAll(pass.extraDefines());

        this.sampling.addDefines(constants);

        var vertexType = pass.vertexType();
        var primitiveType = pass.primitiveType();

        vertexType.getDefines().forEach(constants::add);
        constants.addAll(primitiveType.getDefines());

        return constants.build();
    }
}
