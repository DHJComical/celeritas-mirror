package org.embeddedt.embeddium.gradle.stonecutter;

import dev.kikugie.stonecutter.settings.StonecutterSettings;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.Version;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.VersionJson;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.VersionLocalJson;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

public class CeleritasProjectManagerPlugin implements Plugin<Settings> {

    private VersionLocalJson readOverrideData(Settings projectSettings) {
        try {
            return VersionLocalJson.read(projectSettings);
        } catch(IOException e) {
            return new VersionLocalJson();
        }
    }

    @Override
    public void apply(Settings projectSettings) {
        VersionJson versionData;
        VersionLocalJson overrideData = readOverrideData(projectSettings);
        try {
            versionData = VersionJson.read(projectSettings);
        } catch(IOException e) {
            throw new RuntimeException("Failed to read versions.json", e);
        }
        StonecutterSettings scSettings = (StonecutterSettings)projectSettings.getExtensions().getByName("stonecutter");
        scSettings.setKotlinController(true);
        scSettings.shared(builder -> {
            Predicate<String> overrideFilter = overrideData.getVersionFilterPredicate();
            versionData.versions.stream().flatMap(Version::streamVersionPermutations).filter(v -> v.name().equals(versionData.vcsVersion) || overrideFilter.test(v.name())).forEach(permutation -> {
                builder.vers(Objects.requireNonNull(permutation.name()), Objects.requireNonNull(permutation.semanticName(), "semantic name is null for " + permutation.name()));
            });
            builder.setVcsVersion(versionData.vcsVersion);
        });
        projectSettings.include("common");
        // Create the modern subproject
        projectSettings.include("modern");
        // Create the versioned subprojects under modern
        scSettings.create(projectSettings.project(":modern"));
    }
}