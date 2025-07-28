import dev.kikugie.stonecutter.data.tree.TreeBuilder

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "sponge"
            url = uri("https://repo.spongepowered.org/maven-public/")
            content {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
        maven {
            name = "Fabric Maven"
            url = uri("https://maven.fabricmc.net/")
            content {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            content {
                includeGroupAndSubgroups("net.minecraftforge")
            }
        }
        maven("https://maven.neoforged.net/releases") {
            content {
                includeGroupAndSubgroups("net.neoforged")
            }
        }
        maven("https://maven.kikugie.dev/releases") {
            content {
                includeGroupAndSubgroups("dev.kikugie")
            }
        }
        maven("https://maven.kikugie.dev/snapshots") {
            content {
                includeGroupAndSubgroups("dev.kikugie")
            }
        }
        maven {
            name = "wagyourtail releases"
            url = uri("https://maven.wagyourtail.xyz/releases")
            content {
                includeGroupAndSubgroups("xyz.wagyourtail")
            }
        }
        maven {
            name = "wagyourtail snapshots"
            url = uri("https://maven.wagyourtail.xyz/snapshots")
            content {
                includeGroupAndSubgroups("xyz.wagyourtail")
            }
        }
        maven("https://maven.taumc.org/releases") {
            content {
                includeGroupAndSubgroups("org.taumc")
            }
        }
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

includeBuild("plugins/celeritas-mdg-plugin")
includeBuild("plugins/celeritas-unimined-plugin")
include("common")
include("common-shaders")

val includedVersionsProp = if(extra.has("target_versions")) extra["target_versions"].toString().split(",") else null
val includedSubprojectsProp = if(extra.has("target_subprojects")) extra["target_subprojects"].toString().split(",") else null

fun isVersionIncluded(ver: String): Boolean {
    if (includedVersionsProp == null) {
        return true
    }

    val testVer = ver.substringBefore('-')

    return includedVersionsProp.any { stonecutter.eval(testVer, it) }
}

if(file("forge1710").exists() && isVersionIncluded("1.7.10")) {
    include("forge1710")
}

fun <T> createStonecutterProject(subprojectFolder: String, versions: List<T>, mcVersionGetter: (version: T) -> String = { v -> v.toString() }, action: TreeBuilder.(versions: List<T>) -> Unit) {
    if (includedSubprojectsProp != null && !includedSubprojectsProp.contains(subprojectFolder)) {
        println("Skipping project $subprojectFolder by request")
        return
    }
    if (!file(subprojectFolder).exists()) {
        return
    }
    if (!versions.any { isVersionIncluded(mcVersionGetter.invoke(it)) }) {
        println("Skipping project $subprojectFolder as it does not contain any desired versions")
        return
    }
    val filteredVersions = versions.filter { versions[0] == it || isVersionIncluded(mcVersionGetter.invoke(it)) }
    val subprojectPath = ":$subprojectFolder"
    include(subprojectPath)
    stonecutter {
        create(subprojectPath) {
            action.invoke(this, filteredVersions)
        }
    }
}

createStonecutterProject("forge122", listOf("1.12.2", "1.10.2")) { versions ->
    centralScript = "build.gradle.kts"
    versions(versions)
}

createStonecutterProject("babric", listOf("1.2.5", "1.0.0-beta.7.3", "1.0.0-beta.8.1", "1.7.10")) { versions ->
    centralScript = "build.gradle.kts"
    versions(versions)
}

data class CeleritasTarget(val friendlyName: String, val loaders: List<String>, val semanticName: String = friendlyName)

createStonecutterProject("modern", listOf(
        CeleritasTarget("1.20.1", listOf("forge", "fabric")),
        //CeleritasTarget("1.16.5", listOf("forge")),
        //CeleritasTarget("1.18.2", listOf("forge")),
        //CeleritasTarget("1.20.4", listOf("neoforge")),
        CeleritasTarget("1.21.1", listOf("fabric", "neoforge")),
        CeleritasTarget("1.21.8", listOf("neoforge")),
        //CeleritasTarget("1.19.2", listOf("forge", "fabric"))
), { it.friendlyName }) { targets ->
    targets.forEach {
        val target = it
        it.loaders.forEach { loader ->
            val versionConfig = vers(target.friendlyName + "-" + loader, target.semanticName)
            val buildscriptType = if (loader == "neoforge" || (loader == "forge" && stonecutter.eval(target.semanticName, ">=1.17"))) {
                "mdg"
            } else if (loader == "forge") {
                "unimined"
            } else if (loader == "fabric") {
                "loom"
            } else {
                throw IllegalArgumentException("Unhandled loader/version combo: ${target.friendlyName}-${loader}")
            }
            versionConfig.buildscript = "build.${buildscriptType}.gradle.kts"
        }
    }
}