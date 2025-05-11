pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.parchmentmc.org")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.wagyourtail.xyz/snapshots")
        maven("https://maven.taumc.org/releases")
    }

    plugins {
        id("org.taumc.gradle.versioning") version(extra["taugradle_version"].toString())
        id("org.taumc.gradle.publishing") version(extra["taugradle_version"].toString())
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
    id("dev.kikugie.stonecutter") version(extra["stonecutter_version"].toString())
}

rootProject.name = "celeritas"

include("common")
include("common-shaders")

if(file("forge1710").exists()) {
    include("forge1710")
}
if(file("forge122").exists()) {
    include("forge122")
}
if(file("babric").exists()) {
    include("babric")
}

if(file("modern").exists()) {
    include("modern")
    data class CeleritasTarget(val friendlyName: String, val loaders: List<String>, val semanticName: String = friendlyName)
    val targets = listOf(
            CeleritasTarget("1.16.5", listOf("forge")),
            CeleritasTarget("1.18.2", listOf("forge")),
            CeleritasTarget("1.20.1", listOf("forge", "fabric")),
            //CeleritasTarget("1.20.4", listOf("neoforge")),
            CeleritasTarget("1.21.1", listOf("fabric", "neoforge")),
            //CeleritasTarget("1.19.2", listOf("forge", "fabric"))
    )
    stonecutter {
        centralScript = "build.gradle"
        create(":modern") {
            targets.forEach {
                val target = it
                it.loaders.forEach { loader ->
                    vers(target.friendlyName + "-" + loader, target.semanticName)
                }
            }
            vcsVersion = "1.20.1-forge"
        }
    }
}
