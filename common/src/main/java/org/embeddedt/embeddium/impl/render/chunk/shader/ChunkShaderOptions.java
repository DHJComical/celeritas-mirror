package org.embeddedt.embeddium.impl.render.chunk.shader;

import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;

public record ChunkShaderOptions(ChunkFogMode fog, TerrainRenderPass pass) {

    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.supportsFragmentDiscard()) {
            constants.add("USE_FRAGMENT_DISCARD");
        }

        if (this.pass.hasNoLightmap()) {
            constants.add("CELERITAS_NO_LIGHTMAP");
        }

        var vertexType = pass.vertexType();
        var primitiveType = pass.primitiveType();

        constants.addAll(vertexType.getDefines());
        constants.addAll(primitiveType.getDefines());

        constants.add("VERT_POS_SCALE", String.valueOf(vertexType.getPositionScale()));
        constants.add("VERT_POS_OFFSET", String.valueOf(vertexType.getPositionOffset()));
        constants.add("VERT_TEX_SCALE", String.valueOf(vertexType.getTextureScale()));

        if(!ShaderModBridge.emulateLegacyColorBrightnessFormat()) {
            constants.add("USE_VANILLA_COLOR_FORMAT");
        }

        return constants.build();
    }
}
