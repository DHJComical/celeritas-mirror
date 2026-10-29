package net.irisshaders.iris.pipeline.programs;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The values vanilla provides for a draw. Gbuffer programs receive them as {@code iris_}-prefixed uniforms, such as
 * {@code iris_ModelViewMat}.
 */
public final class VanillaDrawState {
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
	public int albedoTexture;
	public int overlayTexture;
	public int lightmapTexture;
}
