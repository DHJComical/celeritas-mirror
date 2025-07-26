import bs.ModLoader
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector
import org.gradle.kotlin.dsl.register

plugins {
    id("fabric-loom") version "1.11.4"
    id("celeritas.platform-conventions")
    id("celeritas.shader-conventions") apply false
}

group = "org.embeddedt"
version = rootProject.version

val modLoader = ModLoader.fromProject(project)!!
val minecraftVersion = ModLoader.getMinecraftVersion(project)!!

val defaultArchiveBaseName = "celeritas-${modLoader.friendlyName}-mc${minecraftVersion.replace("^1\\.".toRegex(), "")}"

repositories {
    maven("https://maven.parchmentmc.org/") {
        content {
            includeGroup("org.parchmentmc.data")
        }
    }
}

loom {
    accessWidenerPath = rootProject.file("modern/src/main/resources/embeddium.accesswidener")
    mixin {
        useLegacyMixinAp = false
    }
}


dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
        val parchmentVersion = versionedProperty("parchment_version")
        if (parchmentVersion != null) {
            val parchmentData = parchmentVersion.split(":")
            parchment("org.parchmentmc.data:parchment-${parchmentData[0]}:${parchmentData[1]}@zip")
        }
    })

    implementation(project(":common")) {
        isTransitive = false
    }
    shadow(project(":common")) {
        isTransitive = false
    }

    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")

    val ffapiVersion = versionedProperty("fabric_api_version")
    if (ffapiVersion != null) {
        val excludedRuntimeFabricApiModules = listOf("fabric-renderer-api-v1", "fabric-renderer-indigo")
        for (module in rootProject.property("fabric_api_modules").toString().split(",")) {
            val configType = if (excludedRuntimeFabricApiModules.contains(module)) {
                "modCompileOnly"
            } else {
                "modImplementation"
            }
            add(configType, fabricApi.module(module,ffapiVersion))
        }
    }

    if (stonecutter.eval(minecraftVersion, "<1.19.3")) {
        val jomlDep = "org.joml:joml:${rootProject.property("joml_version")}"
        implementation(jomlDep)
        "include"(jomlDep)
    }

    ModDependencyCollector.obtainDeps(project) { cfg, dep ->
        dependencies.add(cfg, dep)
    }
}

if (stonecutter.constants.getOrDefault("shaders", false)) {
    apply(plugin = "celeritas.shader-conventions")
}

tasks.named("validateAccessWidener") {
    enabled = false
}

tasks.named<RemapJarTask>("remapJar") {
    archiveClassifier.set("thin")
}

val shadowJar = tasks.register<ShadowJar>("shadowRemapJar") {
    archiveBaseName = defaultArchiveBaseName
    archiveClassifier = ""
    configurations = listOf(project.configurations.shadow.get())
    from(tasks.named("remapJar"))
    manifest.inheritFrom(tasks.named<Jar>("jar").get().manifest)
    mergeServiceFiles()

    from("COPYING", "COPYING.LESSER", "README.md")
}

val packageJar = tasks.register("packageJar", Copy::class) {
    from(shadowJar.get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}

tasks.named("assemble") {
    dependsOn(packageJar)
}