package net.irisshaders.iris.pipeline.transform.parameter;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.EqualsAndHashCode;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

@EqualsAndHashCode(callSuper = true)
public class SodiumParameters extends Parameters {
	public final ShaderAttributeInputs inputs;
	public final AlphaTest alpha;
    public final ChunkVertexType vertexType;

	public SodiumParameters(Patch patch,
                            Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap,
                            AlphaTest alpha,
                            ShaderAttributeInputs inputs,
                            ChunkVertexType vertexType) {
		super(patch, textureMap);
		this.inputs = inputs;

		this.alpha = alpha;
        this.vertexType = vertexType;
	}

	@Override
	public AlphaTest getAlphaTest() {
		return alpha;
	}

	@Override
	public TextureStage getTextureStage() {
		return TextureStage.GBUFFERS_AND_SHADOW;
	}
}
