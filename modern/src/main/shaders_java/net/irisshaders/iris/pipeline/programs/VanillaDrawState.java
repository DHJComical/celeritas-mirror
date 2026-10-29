package net.irisshaders.iris.pipeline.programs;

//? if <26.1 {
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
//?} else {
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlProgram;

import java.util.Map;
*///?}

/**
 * The values vanilla provides for a draw. Gbuffer programs receive them as {@code iris_}-prefixed uniforms, such as
 * {@code iris_ModelViewMat}.
 */
public final class VanillaDrawState {
	//? if <26.1 {
	public final Matrix4f modelView = new Matrix4f();
	public final Matrix4f projection = new Matrix4f();
	public final Matrix4f textureMatrix = new Matrix4f();
	public final Vector3f chunkOffset = new Vector3f();
	public final Vector4f colorModulator = new Vector4f(1.0F);
	public final Vector4f fogColor = new Vector4f();
	public final Vector3f light0Direction = new Vector3f();
	public final Vector3f light1Direction = new Vector3f();
	public float glintAlpha = 1.0F;
	public float fogStart;
	public float fogEnd = 1.0F;
	//?} else {
	/*// The program vanilla set up the draw for, whose uniform block bindings hold vanilla's values.
	public GlProgram vanillaProgram;
	// The uniform blocks the render pass provides, by vanilla's block name.
	public Map<String, GpuBufferSlice> uniforms;
	public int albedoSampler;
	public int overlaySampler;
	public int lightmapSampler;
	// Zero unless the albedo texture is a texture atlas.
	public int atlasWidth;
	public int atlasHeight;
	// The buffer texture of cloud faces clouds programs generate their vertices from, or 0.
	public int cloudFacesTexture;
	*///?}
	public int albedoTexture;
	public int overlayTexture;
	public int lightmapTexture;
}
