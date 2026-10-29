package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

public enum IrisTerrainPass {
	SHADOW("shadow"),
	SHADOW_CUTOUT("shadow"),
	SHADOW_TRANSLUCENT("shadow"),
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
		return this == SHADOW || this == SHADOW_CUTOUT || this == SHADOW_TRANSLUCENT;
	}

	public TerrainRenderPass toTerrainPass(RenderPassConfiguration<?> configuration) {
        return switch (this) {
            case SHADOW, GBUFFER_SOLID -> configuration.defaultSolidMaterial().pass;
            case SHADOW_CUTOUT, GBUFFER_CUTOUT -> configuration.defaultCutoutMippedMaterial().pass;
            case SHADOW_TRANSLUCENT, GBUFFER_TRANSLUCENT -> configuration.defaultTranslucentMaterial().pass;
        };
	}
}
