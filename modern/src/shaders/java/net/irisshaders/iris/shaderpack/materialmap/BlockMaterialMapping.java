package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraftforge.client.model.data.ModelData;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlockMaterialMapping {
	public static Object2IntMap<BlockState> createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Object2IntMap<BlockState> blockStateIds = new Object2IntOpenHashMap<>();

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
			ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());

            Block block = BuiltInRegistries.BLOCK.get(resourceLocation);

            if (block != Blocks.AIR) {
                blockTypeIds.put(block, convertBlockToRenderType(blockType));
            }
		});

		return blockTypeIds;
	}

    private static final RandomSource RANDOM = new SingleThreadedRandomSource(42L);

    public static Object2IntMap<TextureAtlasSprite> createFallbackTextureMaterialMap(Object2IntMap<BlockState> blockStateIds) {
        if (blockStateIds == null) {
            return null;
        }

        var modelShaper = Minecraft.getInstance().getModelManager().getBlockModelShaper();

        Object2ObjectMap<TextureAtlasSprite, Int2IntMap> votingMap = new Object2ObjectOpenHashMap<>();
        Object2ObjectMap<Block, List<BlockState>> statesByBlock = new Object2ObjectOpenHashMap<>();

        // Group states by block. This means that all models for the same block will be retrieved at once, which may
        // help dynamic model loading implementations.
        for (var state : blockStateIds.keySet()) {
            statesByBlock.computeIfAbsent(state.getBlock(), b -> new ArrayList<>(2)).add(state);
        }

        for (var stateList : statesByBlock.values()) {
            for (var state : stateList) {
                var model = modelShaper.getBlockModel(state);
                var materialId = blockStateIds.getInt(state);

                try {
                    for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
                        conductVoting(model, state, direction, votingMap, materialId);
                    }

                    conductVoting(model, state, null, votingMap, materialId);
                } catch (Exception ignored) {
                    // No problem, we'll just skip this block.
                    break;
                }
            }
        }

        Object2IntMap<TextureAtlasSprite> finalMap = new Object2IntOpenHashMap<>();

        var entryComparator = Comparator.comparingInt(Int2IntMap.Entry::getIntValue);

        for (var entry : Object2ObjectMaps.fastIterable(votingMap)) {
            Int2IntMap.Entry highestEntry;

            if (entry.getValue().size() == 1) {
                highestEntry = entry.getValue().int2IntEntrySet().iterator().next();
            } else {
                highestEntry = entry.getValue().int2IntEntrySet().stream().max(entryComparator).orElseThrow();
            }

            finalMap.put(entry.getKey(), highestEntry.getIntKey());
        }

        return finalMap;
    }

    private static void conductVoting(BakedModel model, BlockState state, @Nullable Direction direction, Object2ObjectMap<TextureAtlasSprite, Int2IntMap> votingMap, int vote) {
        RANDOM.setSeed(42L);
        List<BakedQuad> quadList = model.getQuads(state, direction, RANDOM, ModelData.EMPTY, null);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < quadList.size(); i++) {
            var quad = quadList.get(i);
            var sprite = quad.getSprite();

            if (sprite != null) {
                var votes = votingMap.get(sprite);
                if (votes == null) {
                    votes = new Int2IntArrayMap();
                    votingMap.put(sprite, votes);
                }

                votes.mergeInt(vote, 1, Integer::sum);
            }
        }
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

	private static void addBlockStates(BlockEntry entry, Object2IntMap<BlockState> idMap, int intId) {
		NamespacedId id = entry.id();
		ResourceLocation resourceLocation;
		try {
			resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to get entry for " + intId, exception);
		}

		Block block = BuiltInRegistries.BLOCK.get(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		if (block == Blocks.AIR) {
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
				Iris.logger.warn("Error while parsing the block ID map entry for \"" + "block." + intId + "\":");
				Iris.logger.warn("- The block " + resourceLocation + " has no property with the name " + key + ", ignoring!");

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
