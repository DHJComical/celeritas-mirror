package net.irisshaders.iris.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.util.EnumMap;

public abstract class CommonCustomTextureManager {
    public abstract TextureAccess getNoiseTexture();
    public abstract EnumMap<TextureStage, Object2ObjectMap<String, TextureAccess>> getCustomTextureIdMap();
    public abstract Object2ObjectMap<String, TextureAccess> getCustomTextureIdMap(TextureStage stage);
    public abstract Object2ObjectMap<String, TextureAccess> getIrisCustomTextures();
    public abstract void destroy();
}
