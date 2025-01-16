package org.embeddedt.embeddium.gradle.stonecutter;

import dev.kikugie.stonecutter.settings.StonecutterSettings;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.Version;
import org.embeddedt.embeddium.gradle.stonecutter.versionmanagement.VersionJson;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.invocation.DefaultGradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CeleritasProjectManagerPlugin implements Plugin<Settings> {

    private static final Pattern STONECUTTER_ACTIVE = Pattern.compile("^stonecutter active \"(.*)\" /\\* \\[SC] DO NOT EDIT \\*/$");
    private static final Pattern SET_ACTIVE = Pattern.compile("Set active project to ([A-Za-z0-9.-]+)$");

    private static String readActiveVersion(Settings projectSettings) throws IOException {
        String activeVersion = null;
        Path stonecutterKts = projectSettings.getRootDir().toPath().resolve("modern").resolve("stonecutter.gradle.kts");
        for (var line : Files.readAllLines(stonecutterKts)) {
            var matcher = STONECUTTER_ACTIVE.matcher(line);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public void apply(Settings projectSettings) {
        var allTasks = ((DefaultGradle)projectSettings.getGradle()).getStartParameter().getTaskNames();
        boolean includeAll = allTasks.stream().anyMatch(t -> t.contains("chiseled"));

        Set<String> extraProjects = allTasks.stream().map(SET_ACTIVE::matcher).filter(Matcher::find).map(m -> m.group(1)).collect(Collectors.toSet());

        VersionJson versionData;
        String activeVersion;
        try {
            versionData = VersionJson.read(projectSettings);
            activeVersion = readActiveVersion(projectSettings);
            if (activeVersion == null) {
                throw new IllegalStateException("Failed to read active version");
            }
        } catch(IOException e) {
            throw new RuntimeException("Failed to read version data", e);
        }
        StonecutterSettings scSettings = (StonecutterSettings)projectSettings.getExtensions().getByName("stonecutter");
        scSettings.setKotlinController(true);
        scSettings.shared(builder -> {
            Predicate<Version.Permutation> versionFilter = includeAll ? v -> true : v -> extraProjects.contains(v.name()) || v.name().equals(versionData.vcsVersion) || v.name().equals(activeVersion);
            versionData.versions.stream().flatMap(Version::streamVersionPermutations)
                .filter(versionFilter)
                .forEach(permutation -> {
                    builder.vers(Objects.requireNonNull(permutation.name()), Objects.requireNonNull(permutation.semanticName(), "semantic name is null for " + permutation.name()));
                });
            builder.setVcsVersion(versionData.vcsVersion);
        });
        projectSettings.include("glsl-relocated");
        projectSettings.include("common");
        // Create the modern subproject
        projectSettings.include("modern");
        // Create the versioned subprojects under modern
        scSettings.create(projectSettings.project(":modern"));
        // Create the 1.12.2 subproject
        projectSettings.include("forge122");
    }
}