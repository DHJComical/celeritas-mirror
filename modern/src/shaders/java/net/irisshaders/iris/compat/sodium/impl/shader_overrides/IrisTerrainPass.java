package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

public enum IrisTerrainPass {
	SHADOW("shadow"),
	SHADOW_CUTOUT("shadow"),
	GBUFFER_SOLID("gbuffers_terrain"),
	GBUFFER_CUTOUT("gbuffers_terrain_cutout"),
	GBUFFER_TRANSLUCENT("gbuffers_water");

	private final String name;

	IrisTerrainPass(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isShadow() {
		return this == SHADOW || this == SHADOW_CUTOUT;
	}

	public TerrainRenderPass toTerrainPass(RenderPassConfiguration configuration) {
		switch (this) {
			case SHADOW, GBUFFER_SOLID:
				return configuration.defaultSolidMaterial().pass;
			case SHADOW_CUTOUT, GBUFFER_CUTOUT:
				return configuration.defaultCutoutMippedMaterial().pass;
			case GBUFFER_TRANSLUCENT:
				return configuration.defaultTranslucentMaterial().pass;
			default:
				return null;
		}
	}
}
