package org.embeddedt.embeddium.gradle.stonecutter.versionmanagement;

import java.util.List;
import java.util.stream.Stream;

public class Version {
    private String friendlyName;
    private String semanticName;
    private List<String> loaders;

    public record Permutation(String name, String semanticName) {}

    private String getSemanticName() {
        return semanticName != null ? semanticName : friendlyName;
    }

    public Stream<Permutation> streamVersionPermutations() {
        String semanticName = getSemanticName();
        return loaders.stream().map(loader -> new Permutation(friendlyName + "-" + loader, semanticName));
    }
}
