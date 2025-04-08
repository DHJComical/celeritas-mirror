package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.collect.Iterators;
//? if <1.19.3 {
/*import net.minecraft.core.Registry;
*///?} else {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.tags.TagKey;
import org.embeddedt.embeddium.compat.iris.IBlockEntry;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public record BlockEntry(NamespacedId id, Map<String, String> propertyPredicates, boolean isTag) implements IBlockEntry {
    public Iterable<IBlockEntry> expandEntries() {
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
                IRIS_LOGGER.warn("Failed to find the block tag {}", tag.location());
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
