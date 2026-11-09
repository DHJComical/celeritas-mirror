package net.irisshaders.iris.shadows;

import net.irisshaders.iris.api.v0.IrisShadowRenderCallback;
import org.joml.Matrix4f;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ShadowRenderCallbacks {
	private static final List<IrisShadowRenderCallback> CALLBACKS = new CopyOnWriteArrayList<>();

	private ShadowRenderCallbacks() {
	}

	public static void register(IrisShadowRenderCallback callback) {
		CALLBACKS.add(callback);
	}

	public static boolean isEmpty() {
		return CALLBACKS.isEmpty();
	}

	public static void invoke(Matrix4f modelView, Matrix4f projection, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		for (IrisShadowRenderCallback callback : CALLBACKS) {
			try {
				// Copies, so one callback can't change the matrices the next one or the rest of the shadow pass sees.
				callback.renderShadow(new Matrix4f(modelView), new Matrix4f(projection), cameraX, cameraY, cameraZ, tickDelta);
			} catch (Throwable t) {
				CeleritasShaders.logger().error("Error while running shadow render callback {}", callback, t);
			}
		}
	}
}
