package org.embeddedt.embeddium.compat.iris;

import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;

import java.util.Map;

public interface IBlockEntry {
    Iterable<IBlockEntry> expandEntries();
    NamespacedId id();
    boolean isTag();
    Map<String, String> propertyPredicates();
}
