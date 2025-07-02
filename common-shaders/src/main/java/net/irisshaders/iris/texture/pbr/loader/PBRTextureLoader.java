package net.irisshaders.iris.texture.pbr.loader;

import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.embeddedt.embeddium.compat.mc.MCResourceManager;
import org.jetbrains.annotations.NotNull;

public interface PBRTextureLoader<T> {
	/**
	 * This method must not modify global GL state except the texture binding for {@code GL_TEXTURE_2D}.
	 *
	 * @param texture            The base texture.
	 * @param resourceManager    The resource manager.
	 * @param pbrTextureConsumer The consumer that accepts resulting PBR textures.
	 */
	void load(T texture, MCResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer);

	interface PBRTextureConsumer {
		void acceptNormalTexture(@NotNull MCAbstractTexture texture);

		void acceptSpecularTexture(@NotNull MCAbstractTexture texture);
	}
}
