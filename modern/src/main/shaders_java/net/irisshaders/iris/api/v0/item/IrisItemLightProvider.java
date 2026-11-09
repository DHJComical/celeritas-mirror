package net.irisshaders.iris.api.v0.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
//?}

public interface IrisItemLightProvider {

	Vector3f DEFAULT_LIGHT_COLOR = new Vector3f(1, 1, 1);

	default int getLightEmission(Player player, ItemStack stack) {
		if (stack.getItem() instanceof BlockItem item) {
			BlockState blockState = item.getBlock().defaultBlockState();
			//? if >=1.20.5 {
			/*BlockItemStateProperties itemBlockState = stack.getComponents().get(DataComponents.BLOCK_STATE);
			if (itemBlockState != null) {
				blockState = itemBlockState.apply(blockState);
			}
			*///?} else {
			// Parsed as BlockItem does when placing the block, skipping unknown properties and values.
			CompoundTag itemBlockState = stack.getTagElement("BlockStateTag");
			if (itemBlockState != null) {
				StateDefinition<Block, BlockState> definition = blockState.getBlock().getStateDefinition();

				for (String name : itemBlockState.getAllKeys()) {
					Property<?> property = definition.getProperty(name);

					if (property != null) {
						blockState = withValue(blockState, property, itemBlockState.get(name).getAsString());
					}
				}
			}
			//?}
			return blockState.getLightEmission();
		}

		return 0;
	}

	default Vector3f getLightColor(Player player, ItemStack stack) {
		return DEFAULT_LIGHT_COLOR;
	}
	//? if <1.20.5 {

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}
	//?}
}
