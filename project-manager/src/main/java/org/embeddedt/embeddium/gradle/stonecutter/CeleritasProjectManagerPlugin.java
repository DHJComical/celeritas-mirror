package org.embeddedt.embeddium.gradle.stonecutter;

import dev.kikugie.stonecutter.settings.StonecutterSettings;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.Version;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.VersionJson;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import java.io.IOException;
import java.util.Objects;

public class CeleritasProjectManagerPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings projectSettings) {
        VersionJson versionData;
        try {
            versionData = VersionJson.read(projectSettings);
        } catch(IOException e) {
            throw new RuntimeException("Failed to read versions.json", e);
        }
        StonecutterSettings scSettings = (StonecutterSettings)projectSettings.getExtensions().getByName("stonecutter");
        scSettings.shared(builder -> {
            versionData.versions.stream().flatMap(Version::streamVersionPermutations).forEach(permutation -> {
                builder.vers(Objects.requireNonNull(permutation.name()), Objects.requireNonNull(permutation.semanticName(), "semantic name is null for " + permutation.name()));
            });
            builder.setVcsVersion(versionData.vcsVersion);
        });
        scSettings.create(projectSettings.getRootProject());
    }
}