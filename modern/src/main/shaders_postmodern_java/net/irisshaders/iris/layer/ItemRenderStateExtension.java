package net.irisshaders.iris.layer;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Records which item an item render state was resolved for, so that its ID can be set when it's submitted.
 */
public interface ItemRenderStateExtension {
	void iris$setItem(@Nullable Item item, @Nullable Identifier modelId);
}
