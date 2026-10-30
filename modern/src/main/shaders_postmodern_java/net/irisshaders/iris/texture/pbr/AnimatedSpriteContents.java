package net.irisshaders.iris.texture.pbr;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;

public interface AnimatedSpriteContents {
	/**
	 * The state of the sprite's animation in the atlas it was last uploaded to.
	 */
	@Nullable
	SpriteContents.AnimationState iris$getAnimationState();
}
