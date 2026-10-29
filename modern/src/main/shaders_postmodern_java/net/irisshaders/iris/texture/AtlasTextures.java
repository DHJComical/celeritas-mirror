package net.irisshaders.iris.texture;

import com.mojang.blaze3d.textures.GpuTexture;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The GPU textures that hold texture atlases, which shader packs see the size of through {@code atlasSize}.
 */
public final class AtlasTextures {
	private static final Set<GpuTexture> ATLASES = Collections.newSetFromMap(new WeakHashMap<>());

	private AtlasTextures() {
	}

	public static void add(GpuTexture texture) {
		ATLASES.add(texture);
	}

	public static boolean isAtlas(GpuTexture texture) {
		return ATLASES.contains(texture);
	}
}
