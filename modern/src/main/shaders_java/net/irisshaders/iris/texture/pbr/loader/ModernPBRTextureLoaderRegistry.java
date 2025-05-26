package net.irisshaders.iris.texture.pbr.loader;

import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModernPBRTextureLoaderRegistry implements PBRTextureLoaderRegistry {
    public ModernPBRTextureLoaderRegistry() {
        register(SimpleTexture.class, new SimplePBRLoader());
        register(TextureAtlas.class, new AtlasPBRLoader());

    }

    private final Map<Class<?>, PBRTextureLoader<?>> loaderMap = new HashMap<>();

    @Override
    public <T> void register(Class<? extends T> clazz, PBRTextureLoader<T> loader) {
        loaderMap.put(clazz, loader);
    }

    @Override
    public @Nullable <T> PBRTextureLoader<T> getLoader(Class<? extends T> clazz) {
        return (PBRTextureLoader<T>) loaderMap.get(clazz);
    }
}
