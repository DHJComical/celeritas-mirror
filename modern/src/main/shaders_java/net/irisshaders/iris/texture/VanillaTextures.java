package net.irisshaders.iris.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
//? if <26.1 {
import net.irisshaders.iris.mixin.LightTextureAccessor;
import org.embeddedt.embeddium.impl.util.TextureUtil;
//?} else {
/*import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
*///?}

/**
 * The GL names of vanilla's textures.
 */
public final class VanillaTextures {
	private VanillaTextures() {
	}

	public static int glId(AbstractTexture texture) {
		//? if <26.1 {
		return texture.getId();
		//?} else
		//return ((GlTexture) texture.getTexture()).glId();
	}

	public static int missingTexture() {
		//? if <26.1 {
		return MissingTextureAtlasSprite.getTexture().getId();
		//?} else
		//return glId(Minecraft.getInstance().getTextureManager().getTexture(MissingTextureAtlasSprite.getLocation()));
	}

	/**
	 * Returns the atlas terrain is drawn with.
	 */
	public static int blockAtlas() {
		//? if <26.1 {
		return TextureUtil.getBlockTextureId();
		//?} else
		//return glId(Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS));
	}

	/**
	 * Returns the lightmap the game is currently drawing the world with.
	 */
	public static int lightmap() {
		//? if <26.1 {
		return ((LightTextureAccessor) Minecraft.getInstance().gameRenderer.lightTexture()).getLightTexture().getId();
		//?} else
		//return ((GlTexture) Minecraft.getInstance().gameRenderer.lightmap().texture()).glId();
	}
}
