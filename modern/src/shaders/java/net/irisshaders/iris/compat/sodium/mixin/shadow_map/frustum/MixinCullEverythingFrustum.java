package net.irisshaders.iris.compat.sodium.mixin.shadow_map.frustum;

import org.embeddedt.embeddium.impl.render.viewport.Viewport;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.render.viewport.frustum.Frustum;
import net.irisshaders.iris.shadows.frustum.CullEverythingFrustum;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CullEverythingFrustum.class)
public class MixinCullEverythingFrustum implements Frustum, ViewportProvider {
	private static final Vector3d EMPTY = new Vector3d();

	@Override
	public Viewport sodium$createViewport() {
		return new Viewport(this, EMPTY);
	}

	@Override
	public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		return false;
	}
}
