package net.irisshaders.iris.compat.flywheel.compile.transform;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

// Extends Parameters so that ShaderTransformer.commonPatch can be applied; the patch type is only a placeholder.
public class ClrwlTransformParameters extends Parameters
{
	private final boolean isCrumbling;
	private final boolean customOutputs;

	public ClrwlTransformParameters(ShaderType type, boolean isCrumbling, boolean customOutputs, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap)
	{
		super(Patch.VANILLA, textureMap);
		super.type = type;
		this.isCrumbling = isCrumbling;
		this.customOutputs = customOutputs;
	}

	public boolean usesCustomOutputs()
	{
		return customOutputs;
	}

	public boolean isCrumbling()
	{
		return isCrumbling;
	}

	@Override
	public TextureStage getTextureStage() { return TextureStage.GBUFFERS_AND_SHADOW; }
}
