package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.renderer.RenderType;

import java.util.Collection;

public interface RenderBuffersExt {
	void beginLevelRendering();

	void endLevelRendering();

	/**
	 * {@return the render types that the vanilla buffer source gives a fixed buffer}
	 */
	Collection<RenderType> getFixedRenderTypes();
}
