package net.irisshaders.iris.shaderpack.materialmap;

import org.embeddedt.embeddium.compat.iris.IBlockEntry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record BlockEntry(NamespacedId id, Set<Integer> ids) implements IBlockEntry {


    @Override
    public Iterable<IBlockEntry> expandEntries() {
        return Collections.singletonList(this);
    }

    @Override
    public boolean isTag() {
        return false;
    }

    @Override
    public Map<String, String> propertyPredicates() {
        return Map.of();
    }
}
