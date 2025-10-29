package org.embeddedt.embeddium.gradle.build.conventions;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;

import java.util.List;

public class ShadowHelper {
    public static void createShadowRemapJar(Project project) {
        createShadowRemapJar(project, "remapJar");
    }

    public static void createShadowRemapJar(Project project, String remapJarTaskName) {
        project.getTasks().named("shadowJar").configure(task -> {
            if (task instanceof ShadowJar shadowJar) {
                shadowJar.setConfigurations(List.of());
            }
        });
        project.getTasks().register("shadowRemapJar", ShadowJar.class).configure(shadowJar -> {
            shadowJar.getArchiveClassifier().set("");
            shadowJar.setConfigurations(List.of(project.getConfigurations().getByName("shadow")));
            shadowJar.from(project.getTasks().named(remapJarTaskName));
            shadowJar.getManifest().inheritFrom(((Jar)project.getTasks().getByName("jar")).getManifest());
            shadowJar.relocate("org.joml", "org.embeddedt.embeddium.impl.shadow.joml");
            shadowJar.mergeServiceFiles();

            shadowJar.from("COPYING", "COPYING.LESSER", "README.md");
        });
    }
}
