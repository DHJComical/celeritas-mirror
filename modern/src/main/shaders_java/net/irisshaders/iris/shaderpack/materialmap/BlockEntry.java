package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.collect.Iterators;
import net.irisshaders.iris.Iris;
//? if <1.19.3 {
/*import net.minecraft.core.Registry;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.tags.TagKey;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record BlockEntry(NamespacedId id, Map<String, String> propertyPredicates, boolean isTag) {

	/**
	 * Parses a block ID entry.
	 *
	 * @param entry The string representation of the entry. Must not be empty.
	 */
	@NotNull
	public static BlockEntry parse(@NotNull String entry) {
		if (entry.isEmpty()) {
			throw new IllegalArgumentException("Called BlockEntry::parse with an empty string");
		}

        boolean isTag;

        if (entry.startsWith("%")) {
            isTag = true;
            entry = entry.substring(1);
        } else {
            isTag = false;
        }

		// We can assume that this array is of at least array length because the input string is non-empty.
		String[] splitStates = entry.split(":");

		// Trivial case: no states, no namespace
		if (splitStates.length == 1) {
			return new BlockEntry(new NamespacedId("minecraft", entry), Map.of(), isTag);
		}

		// Less trivial case: no states involved, just a namespace
		//
		// The first term MUST be a valid ResourceLocation component without an equals sign
		// The second term, if it does not contain an equals sign, must be a valid ResourceLocation component.
		if (splitStates.length == 2 && !splitStates[1].contains("=")) {
			return new BlockEntry(new NamespacedId(splitStates[0], splitStates[1]), Map.of(), isTag);
		}

		// Complex case: One or more states involved...
		int statesStart;
		NamespacedId id;

		if (splitStates[1].contains("=")) {
			// We have an entry of the form "tall_grass:half=upper"
			statesStart = 1;
			id = new NamespacedId("minecraft", splitStates[0]);
		} else {
			// We have an entry of the form "minecraft:tall_grass:half=upper"
			statesStart = 2;
			id = new NamespacedId(splitStates[0], splitStates[1]);
		}

		// We must parse each property key=value pair from the state entry.
		//
		// These pairs act as a filter on the block states. Thus, the shader pack does not have to specify all the
		// individual block properties itself; rather, it only specifies the parts of the block state that it wishes
		// to filter in/out.
		//
		// For example, some shader packs may make it so that hanging lantern blocks wave. They will put something of
		// the form "lantern:hanging=false" in the ID map as a result. Note, however, that there are also waterlogged
		// hanging lanterns, which would have "lantern:hanging=false:waterlogged=true". We must make sure that when the
		// shader pack author writes "lantern:hanging=false", that we do not just match that individual state, but that
		// we also match the waterlogged variant too.
		Map<String, String> map = new HashMap<>();

		for (int index = statesStart; index < splitStates.length; index++) {
			// Split "key=value" into the key and value
			String[] propertyParts = splitStates[index].split("=");

			if (propertyParts.length != 2) {
				Iris.logger.warn("Warning: the block ID map entry \"" + entry + "\" could not be fully parsed:");
				Iris.logger.warn("- Block state property filters must be of the form \"key=value\", but "
					+ splitStates[index] + " is not of that form!");

				// Continue and ignore the invalid entry.
				continue;
			}

			map.put(propertyParts[0], propertyParts[1]);
		}

		return new BlockEntry(id, Map.copyOf(map), isTag);
	}

    public Iterable<BlockEntry> expandEntries() {
        if (!this.isTag) {
            return Collections.singletonList(this);
        } else {
            //? if >=1.19.3 {
            var tag = TagKey.create(Registries.BLOCK, ResourceLocationUtil.make(id.getNamespace().toLowerCase(Locale.ROOT), id.getName().toLowerCase(Locale.ROOT)));
            var tagOpt = BuiltInRegistries.BLOCK.getTag(tag);
            //?} else {
            /*var tag = TagKey.create(Registry.BLOCK_REGISTRY, ResourceLocationUtil.make(id.getNamespace().toLowerCase(Locale.ROOT), id.getName().toLowerCase(Locale.ROOT)));
            var tagOpt = Registry.BLOCK.getTag(tag);
            *///?}

            if (!tagOpt.isPresent()) {
                Iris.logger.warn("Failed to find the block tag {}", tag.location());
                return Collections.emptyList();
            }

            var holderSet = tagOpt.orElseThrow();
            return () -> Iterators.transform(holderSet.iterator(), holder -> {
                var location = holder.unwrapKey().orElseThrow().location();
                return new BlockEntry(new NamespacedId(location.getNamespace(), location.getPath()), propertyPredicates, false);
            });
        }
    }
}
