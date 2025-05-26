package net.irisshaders.iris.texture.pbr.loader;

import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

public interface PBRTextureLoaderRegistry {
    PBRTextureLoaderRegistry INSTANCE = ServiceLoader.load(PBRTextureLoaderRegistry.class).findFirst().orElseThrow();


	<T> void register(Class<? extends T> clazz, PBRTextureLoader<T> loader);

	@SuppressWarnings("unchecked")
	@Nullable
	<T> PBRTextureLoader<T> getLoader(Class<? extends T> clazz);
}
