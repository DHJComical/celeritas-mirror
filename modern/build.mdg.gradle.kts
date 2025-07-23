import bs.ModLoader
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import org.embeddedt.embeddium.gradle.fabric.remapper.GenerateATFromAWTask
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector

plugins {
    // Apply the plugin. You can find the latest version at https://projects.neoforged.net/neoforged/ModDevGradle
    id("net.neoforged.moddev") version "2.0.103" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.103" apply false
    id("celeritas.platform-conventions")
    id("celeritas.shader-conventions") apply false
    id("embeddium-fabric-module-finder")
}

group = "org.embeddedt"
version = rootProject.version

val modLoader = ModLoader.fromProject(project)!!
val minecraftVersion = ModLoader.getMinecraftVersion(project)!!

val generatedATPath = layout.buildDirectory.file("generated/accesstransformer.cfg")

val generateAccessTransformer = tasks.register<GenerateATFromAWTask>("generateAccessTransformer") {
    accessTransformerPath.set(generatedATPath)
    accessWidenerPath.set(rootProject.file("modern/src/main/resources/embeddium.accesswidener"))
}

val modDevExtension: ModDevExtension = if (modLoader == ModLoader.NEOFORGE) {
    apply(plugin = "net.neoforged.moddev")
    project.extensions.getByName("neoForge") as ModDevExtension
} else {
    apply(plugin = "net.neoforged.moddev.legacyforge")
    val legacyForge = project.extensions.getByName("legacyForge") as LegacyForgeExtension
    legacyForge.version = "1.20.1-47.3.0"
    generateAccessTransformer.configure {
        val obfuscation = project.extensions.getByType<ObfuscationExtension>()
        tsrgMappings = obfuscation.namedToSrgMappings
    }
    legacyForge
}

val parchmentVersion = versionedProperty("parchment_version")

if (parchmentVersion != null) {
    val parchmentData = parchmentVersion.split(":")
    modDevExtension.parchment {
        minecraftVersion = parchmentData[0]
        mappingsVersion = parchmentData[1]
    }
}

modDevExtension.mods {
    create("embeddium") {
        sourceSet(sourceSets.main.get())
        sourceSet(project(":common").sourceSets.main.get())
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(generateAccessTransformer)
}

modDevExtension.accessTransformers.from(generatedATPath)

val modMixinConfigs = mutableListOf("embeddium.mixins.json")

if (stonecutter.constants.getOrDefault("shaders", false)) {
    sourceSets {
        main {
            arrayOf("shaders", "batching").forEach { it ->
                java.srcDir("src/main/${it}_java")
                resources.srcDir("src/main/${it}_resources")
            }
        }
    }

    modMixinConfigs.add("oculus-batched-entity-rendering.mixins.json")
    modMixinConfigs.addAll(listOf(
            "mixins.oculus.json",
            "mixins.oculus.compat.sodium.json",
            "mixins.oculus.compat.indigo.json",
            "mixins.oculus.compat.indium.json",
            "mixins.oculus.compat.dh.json",
            "mixins.oculus.compat.pixelmon.json"
    ))

    apply(plugin = "celeritas.shader-conventions")
}

modDevExtension.runs {
    create("client") {
        client()
        for (config in modMixinConfigs) {
            programArgument("--mixin.config")
            programArgument(config)
        }
    }
}

dependencies {
    shadow(project(":common")) {
        isTransitive = false
    }
    val ffapiVersion = versionedProperty("ffapi")
    if (ffapiVersion != null) {
        for (module in rootProject.property("fabric_api_modules").toString().split(",")) {
            compileOnly(fabricApiModuleFinder.module(modLoader, module,ffapiVersion))
        }

        compileOnly("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")
    }
    if (modLoader != ModLoader.NEOFORGE) {
        val mixinExtrasVersion = rootProject.property("mixinextras").toString()
        compileOnly("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")

        implementation("io.github.llamalad7:mixinextras-${modLoader.friendlyName}:$mixinExtrasVersion")
        "jarJar"("io.github.llamalad7:mixinextras-${modLoader.friendlyName}:$mixinExtrasVersion")
    }

    ModDependencyCollector.obtainDeps(project) { cfg, dep ->
        dependencies.add(cfg, dep)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes.put("MixinConfigs", modMixinConfigs.joinToString(","))
    }
}