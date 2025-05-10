package org.embeddedt.embeddium.gradle.stonecutter;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension;
import kotlin.Unit;
import org.gradle.api.Project;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModDependencyCollector {
    record Dependency(String cursePrefix, List<DependencyCondition> versionConditions) {
    }
    record DependencyCondition(String evalCondition, String version) {}

    private static final Map<String, Dependency> FORGELIKE_DEPENDENCY_MAP = Map.of(
            "immersiveengineering",
            new Dependency("curse.maven:immersiveengineering-231951:", List.of(
                    new DependencyCondition("=1.20.1", "4782978"),
                    new DependencyCondition("=1.19.2", "4193176"),
                    new DependencyCondition("=1.18.2", "4412849")
            )),
            "brandonscore",
            new Dependency("curse.maven:brandonscore-231382:", List.of(
                    new DependencyCondition("=1.20.4", "5981781"),
                    new DependencyCondition("=1.20.1", "5422013"),
                    new DependencyCondition("=1.18.2", "4790968")
            )),
            "codechickenlib",
            new Dependency("curse.maven:codechickenlib-242818:", List.of(
                    new DependencyCondition("=1.21.1", "6061637"),
                    //new DependencyCondition("=1.20.4", "5826640"),
                    new DependencyCondition("=1.20.1", "5753868"),
                    new DependencyCondition("=1.19.2", "4965330"),
                    new DependencyCondition("=1.18.2", "4607274"),
                    new DependencyCondition("=1.16.5", "3681973")
            )),
            "flywheel",
            new Dependency("curse.maven:flywheel-486392:", List.of(
                    new DependencyCondition("=1.19.2", "4341471"),
                    new DependencyCondition("=1.18.2", "4341461"),
                    new DependencyCondition("=1.16.5", "3535459")
            ))
    );

    private static final Map<String, Dependency> FABRIC_DEPENDENCY_MAP = Map.of();

    private static final boolean LOAD_IN_DEV = false;

    private static Map<String, Dependency> dependencyMap(String ver) {
        if (ver.contains("forge")) {
            return FORGELIKE_DEPENDENCY_MAP;
        } else {
            return FABRIC_DEPENDENCY_MAP;
        }
    }

    public static void defineConsts(StonecutterControllerExtension scController) {
        scController.parameters(params -> {
            var mcVersion = params.getMetadata().getVersion();
            var depMap = dependencyMap(params.getMetadata().getProject());
            depMap.forEach((key, dep) -> {
                params.getConstants().put(key, dep.versionConditions.stream().anyMatch(c -> scController.eval(mcVersion, c.evalCondition)));
            });
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(FORGELIKE_DEPENDENCY_MAP.keySet());
            allKeys.addAll(FABRIC_DEPENDENCY_MAP.keySet());
            for (String key : allKeys) {
                if (!depMap.containsKey(key)) {
                   params.getConstants().put(key, false);
                }
            }
            return Unit.INSTANCE;
        });
    }

    public static void addModDependencies(Project project) {
        var scBuild = project.getExtensions().getByType(StonecutterBuildExtension.class);
        var mcVersion = scBuild.getCurrent().getVersion();
        var configurationName = LOAD_IN_DEV ? "modImplementation" : "modCompileOnly";
        dependencyMap(scBuild.getCurrent().getProject()).forEach((key, dep) -> {
            var vers = dep.versionConditions.stream().filter(c -> scBuild.eval(mcVersion, c.evalCondition)).findFirst();
            vers.ifPresent(dependencyCondition ->
                    project.getDependencies().add(configurationName, dep.cursePrefix + dependencyCondition.version));
        });
    }
}
