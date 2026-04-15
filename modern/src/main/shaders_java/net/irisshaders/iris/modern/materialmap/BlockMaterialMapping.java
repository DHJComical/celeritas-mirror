package net.irisshaders.iris.modern.materialmap;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.jetbrains.annotations.NotNull;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.lang.reflect.Method;
import java.util.*;

public class BlockMaterialMapping {
	public static Object2IntMap<BlockState> createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Object2IntMap<BlockState> blockStateIds = new Object2IntOpenHashMap<>();

        blockStateIds.defaultReturnValue(-1);

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				addBlockStates(entry, blockStateIds, intId);
			}
		});

		return blockStateIds;
	}

	public static Map<Block, RenderType> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
		Map<Block, RenderType> blockTypeIds = new Object2ObjectOpenHashMap<>();

		blockPropertiesMap.forEach((id, blockType) -> {
			ResourceLocation resourceLocation = ResourceLocationUtil.make(id.getNamespace(), id.getName());

            Block block = BuiltInRegistries.BLOCK.get(resourceLocation);

            if (block != Blocks.AIR) {
                blockTypeIds.put(block, convertBlockToRenderType(blockType));
            }
		});

		return blockTypeIds;
	}

	private static RenderType convertBlockToRenderType(BlockRenderType type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case SOLID -> RenderType.solid();
			case CUTOUT -> RenderType.cutout();
			case CUTOUT_MIPPED -> RenderType.cutoutMipped();
			case TRANSLUCENT -> RenderType.translucent();
		};
	}

    private static final ClassValue<Boolean> IS_APPEARANCE_CHANGING = new ClassValue<>() {
        @Override
        protected Boolean computeValue(@NotNull Class<?> clz) {
            Method m;
            try {
                m = clz.getMethod("getAppearance", BlockState.class, BlockAndTintGetter.class, BlockPos.class, Direction.class, BlockState.class, BlockPos.class);
            } catch(ReflectiveOperationException e) {
                return false;
            }
            //? if forge {
            return m.getDeclaringClass() != net.minecraftforge.common.extensions.IForgeBlock.class;
            //?} else if neoforge {
            /*return m.getDeclaringClass() != net.neoforged.neoforge.common.extensions.IBlockExtension.class;
             *///?} else
            /*return false;*/
        }
    };

    private static boolean isAppearanceChangingBlock(Block block) {
        return IS_APPEARANCE_CHANGING.get(block.getClass());
    }

    private static Iterable<BlockEntry> getTagEntryBlocks(BlockEntry tagEntry) {
        //? if >=1.19.3 {
        var tag = TagKey.create(Registries.BLOCK, ResourceLocationUtil.make(tagEntry.id().getNamespace().toLowerCase(Locale.ROOT), tagEntry.id().getName().toLowerCase(Locale.ROOT)));
        var tagOpt = BuiltInRegistries.BLOCK.getTag(tag);
        //?} else {
        /*var tag = TagKey.create(Registry.BLOCK_REGISTRY, ResourceLocationUtil.make(tagEntry.id().getNamespace().toLowerCase(Locale.ROOT), tagEntry.id().getName().toLowerCase(Locale.ROOT)));
        var tagOpt = Registry.BLOCK.getTag(tag);
        *///?}

        if (!tagOpt.isPresent()) {
            CeleritasShaders.logger().debug("Failed to find the block tag {}", tag.location());
            return Collections.emptyList();
        }

        var holderSet = tagOpt.orElseThrow();
        return () -> Iterators.transform(holderSet.iterator(), holder -> {
            var location = holder.unwrapKey().orElseThrow().location();
            return new BlockEntry(new NamespacedId(location.getNamespace(), location.getPath()), tagEntry.propertyPredicates(), false);
        });
    }

	private static void addBlockStates(BlockEntry entry, Object2IntMap<BlockState> idMap, int intId) {
        if (entry.isTag()) {
            getTagEntryBlocks(entry).forEach(nested -> addBlockStates(nested, idMap, intId));
            return;
        }

		NamespacedId id = entry.id();
		ResourceLocation resourceLocation;
		try {
			resourceLocation = ResourceLocationUtil.make(id.getNamespace(), id.getName());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to get entry for " + intId, exception);
		}

		Block block = BuiltInRegistries.BLOCK.get(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		if (block == Blocks.AIR) {
			return;
		}

        if (isAppearanceChangingBlock(block)) {
            CeleritasShaders.logger().warn("Warning while parsing the block ID map entry for \"" + "block." + intId + "\":");
            CeleritasShaders.logger().warn("- The block {} can change appearance, skipping!", resourceLocation);
            return;
        }

		Map<String, String> propertyPredicates = entry.propertyPredicates();

		if (propertyPredicates.isEmpty()) {
			// Just add all the states if there aren't any predicates
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				// NB: Using putIfAbsent means that the first successful mapping takes precedence
				//     Needed for OptiFine parity:
				//     https://github.com/IrisShaders/Iris/issues/1327
				idMap.putIfAbsent(state, intId);
			}

			return;
		}

		// As a result, we first collect each key=value pair in order to determine what properties we need to filter on.
		// We already get this from BlockEntry, but we convert the keys to `Property`s to ensure they exist and to avoid
		// string comparisons later.
		Map<Property<?>, String> properties = new HashMap<>();
		StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();

		propertyPredicates.forEach((key, value) -> {
			Property<?> property = stateManager.getProperty(key);

			if (property == null) {
				CeleritasShaders.logger().warn("Error while parsing the block ID map entry for \"" + "block." + intId + "\":");
				CeleritasShaders.logger().warn("- The block " + resourceLocation + " has no property with the name " + key + ", ignoring!");

				return;
			}

			properties.put(property, value);
		});

		// Once we have a list of properties and their expected values, we iterate over every possible state of this
		// block and check for ones that match the filters. This isn't particularly efficient, but it works!
		for (BlockState state : stateManager.getPossibleStates()) {
			if (checkState(state, properties)) {
				// NB: Using putIfAbsent means that the first successful mapping takes precedence
				//     Needed for OptiFine parity:
				//     https://github.com/IrisShaders/Iris/issues/1327
				idMap.putIfAbsent(state, intId);
			}
		}
	}

	// We ignore generics here, the actual types don't matter because we just convert
	// them to strings anyways, and the compiler checks just get in the way.
	//
	// If you're able to rewrite this function without SuppressWarnings, feel free.
	// But otherwise it works fine.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean checkState(BlockState state, Map<Property<?>, String> expectedValues) {
		for (Map.Entry<Property<?>, String> condition : expectedValues.entrySet()) {
			Property property = condition.getKey();
			String expectedValue = condition.getValue();

			String actualValue = property.getName(state.getValue(property));

			if (!expectedValue.equals(actualValue)) {
				return false;
			}
		}

		return true;
	}
}
