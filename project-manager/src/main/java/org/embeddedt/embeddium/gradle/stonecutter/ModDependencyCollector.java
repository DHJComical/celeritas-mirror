package org.embeddedt.embeddium.gradle.stonecutter;

import dev.kikugie.stonecutter.build.StonecutterBuild;
import dev.kikugie.stonecutter.controller.StonecutterController;
import org.gradle.api.Project;

import java.util.List;
import java.util.Map;

public class ModDependencyCollector {
    record Dependency(String cursePrefix, List<DependencyCondition> versionConditions) {
    }
    record DependencyCondition(String evalCondition, String version) {}

    private static final Map<String, Dependency> DEPENDENCY_MAP = Map.of(
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
                    new DependencyCondition("=1.20.4", "5826640"),
                    new DependencyCondition("=1.20.1", "5753868"),
                    new DependencyCondition("=1.19.2", "4965330"),
                    new DependencyCondition("=1.18.2", "4607274"),
                    new DependencyCondition("=1.16.5", "3681973")
            ))
    );

    private static final boolean LOAD_IN_DEV = true;

    public static void defineConsts(StonecutterController scController) {
        scController.parameters(params -> {
            var mcVersion = params.getMetadata().getVersion();
            DEPENDENCY_MAP.forEach((key, dep) -> {
                params.getConsts().set(key, dep.versionConditions.stream().anyMatch(c -> scController.eval(mcVersion, c.evalCondition)));
            });
        });
    }

    public static void addModDependencies(Project project) {
        var scBuild = project.getExtensions().getByType(StonecutterBuild.class);
        var mcVersion = scBuild.getCurrent().getVersion();
        var configurationName = LOAD_IN_DEV ? "modImplementation" : "modCompileOnly";
        DEPENDENCY_MAP.forEach((key, dep) -> {
            var vers = dep.versionConditions.stream().filter(c -> scBuild.eval(mcVersion, c.evalCondition)).findFirst();
            vers.ifPresent(dependencyCondition ->
                    project.getDependencies().add(configurationName, dep.cursePrefix + dependencyCondition.version));
        });
    }
}
