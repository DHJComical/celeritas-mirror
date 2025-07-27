package org.embeddedt.embeddium.gradle.unimined;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import kotlin.Unit;
import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask;
//import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask;

import java.util.List;
import java.util.Map;

public class ProductionJarHelper {
    private static void configureDefaultJarManifest(Project project, Map<String, ?> attributesToAdd) {
        project.getTasks().named("jar").configure(task -> {
            if (task instanceof Jar jarTask) {
                jarTask.manifest(manifest -> {
                    manifest.attributes(attributesToAdd);
                });
            }
        });
    }

    public static void setupLegacyFMLManifest(Project project) {
        configureDefaultJarManifest(project, Map.of(
                "FMLCorePlugin", "org.taumc.celeritas.core.CeleritasLoadingPlugin",
                "FMLCorePluginContainsFMLMod", "true",
                "FMLAT", "celeritas_at.cfg",
                "ForceLoadAsMod", "true",
                "Lwjgl3ify-Aware", "true"
        ));
    }

    public static void createShadowRemapJar(Project project) {
        project.getTasks().named("shadowJar").configure(task -> {
            if (task instanceof ShadowJar shadowJar) {
                shadowJar.setConfigurations(List.of());
            }
        });
        project.getTasks().register("shadowRemapJar", ShadowJar.class).configure(shadowJar -> {
            shadowJar.getArchiveClassifier().set("");
            shadowJar.setConfigurations(List.of(project.getConfigurations().getByName("shadow")));
            shadowJar.from(project.getTasks().named("remapJar"));
            shadowJar.getManifest().inheritFrom(((Jar)project.getTasks().getByName("jar")).getManifest());
            shadowJar.relocate("org.joml", "org.embeddedt.embeddium.impl.shadow.joml");
            shadowJar.mergeServiceFiles();

            shadowJar.from("COPYING", "COPYING.LESSER", "README.md");
        });
        var remapJarTask = project.getTasks().named("remapJar");
        remapJarTask.configure(task -> {
            if (task instanceof RemapJarTask remapJar) {
                remapJar.mixinRemap(m -> {
                    m.enableBaseMixin();
                    m.enableMixinExtra();
                    m.disableRefmap();
                    return Unit.INSTANCE;
                });
                remapJar.getAsJar().getArchiveClassifier().set("remapped-thin");
            }
        });
        project.getTasks().named("assemble").configure(task -> task.dependsOn(remapJarTask));
    }

    private static Map<String, String> getProcessedProperties(Project project) {
        return Map.of("version", project.getVersion().toString());
    }

    public static void configureProcessedResources(Project project) {
        var props = getProcessedProperties(project);
        var isModernProject = project.getBuildTreePath().startsWith(":modern");
        project.getTasks().named("processResources").configure(task -> {
            if (task instanceof ProcessResources processResources) {
                props.forEach((key, val) -> processResources.getInputs().property(key, val));
                processResources.filesMatching("mcmod.info", details -> details.expand(props));
                processResources.filesMatching("fabric.mod.json", details -> details.expand(props));
                if (!isModernProject) {
                    processResources.from(project.getRootProject().file("modern/src/main/resources/icon.png"));
                }
            }
        });
    }
}
