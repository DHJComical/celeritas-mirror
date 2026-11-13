import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import java.net.URI

plugins {
    id("celeritas.platform-conventions")
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus") version "1.17.7"
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter {
            includeGroup("net.fabricmc")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.cloverclient.com/releases") }
        filter {
            includeGroup("pl.tomgirl")
        }
    }
}

// Pylon bundles LWJGL3 and replaces the LWJGL2 API, so no LWJGL artifact from the version manifest may stay on the
// classpath.
configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")
    exclude(group = "org.lwjgl")
}

group = "org.embeddedt"
version = rootProject.version

evaluationDependsOn(":common")

data class VersionData(val uniminedVersion: String, val metadataURL: URI? = null)

val versionDataMap = mapOf(
        "1.0.0-beta.7.3" to VersionData("b1.7.3"),
        "1.0.0-beta.8.1" to VersionData( "b1.8.1"),
        "1.2-alpha.125a" to VersionData("12w05a-1442", file("12w05a.json").toURI())
)

val isClientServerSplit = stonecutter.eval(project.name, "<1.3")

val featherVersion = if(isClientServerSplit) 23 else 28
val versionData = versionDataMap.getOrDefault(project.name, VersionData(project.name))

base.archivesName = "celeritas-fabriclike-mc${versionData.uniminedVersion.replace(Regex("^1\\."), "")}"

loom {
    if (isClientServerSplit) {
        clientOnlyMinecraftJar()
    }
    mixin {
        useLegacyMixinAp = false
    }
    if (stonecutter.eval(project.name, "<1.6")) {
        runs.configureEach {
            // Required by LegacyFix
            vmArg("--add-opens=java.base/java.lang=ALL-UNNAMED")
        }
    }
}

ploceus {
    if (isClientServerSplit) {
        clientOnlyMappings()
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${versionData.uniminedVersion}")
    mappings(ploceus.featherMappings(featherVersion.toString()))

    implementation(project(":common")) {
        isTransitive = false
    }
    shadow(project(":common")) {
        isTransitive = false
    }

    implementation("org.joml:joml:1.10.5")
    shadow("org.joml:joml:1.10.5")
    implementation("it.unimi.dsi:fastutil:8.5.15")

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")

    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")
    modImplementation("pl.tomgirl:pylon:${rootProject.property("pylon_version")}")
    if (stonecutter.eval(project.name, "<1.6")) {
        // Pylon needs a deAWT mod before 1.6
        modLocalRuntime("maven.modrinth:legacyfix:26.10")
    }
}

tasks.named("validateAccessWidener") {
    enabled = false
}

val remapJarTask = tasks.named<RemapJarTask>("remapJar") {
    archiveClassifier.set("thin")
}

val shadowJar = tasks.register<ShadowJar>("shadowRemapJar") {
    archiveBaseName = base.archivesName
    archiveClassifier = ""
    configurations = listOf(project.configurations.shadow.get())
    from(zipTree(remapJarTask.get().archiveFile))
    manifest.from(tasks.named<Jar>("jar").get().manifest)
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

/*
val remapJar = tasks.named<AbstractRemapJarTask>("remapJar") {
    manifest {
        attributes(mapOf("Calamus-Generation" to "1"))
    }
}

ShadowHelper.createShadowRemapJar(project)
ProductionJarHelper.configureRemapJar(project)
LWJGLHelper.convertLwjgl2To3(project)
ProductionJarHelper.configureProcessedResources(project)

tasks.register("packageJar", Copy::class) {
    from(tasks.named<ShadowJar>("shadowRemapJar").get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}

tasks.named("genIntellijRuns") {
    enabled = false
}

 */