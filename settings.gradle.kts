import dev.kikugie.stonecutter.data.tree.builder.TreeBuilder

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        exclusiveContent {
            forRepository { maven("https://maven.neoforged.net/releases") }
            filter {
                includeGroup("net.neoforged")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.kikugie.dev/releases") }
            forRepository { maven("https://maven.kikugie.dev/snapshots") }
            filter {
                includeGroup("dev.kikugie")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.fabricmc.net/") }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroup("fabric-loom")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.ornithemc.net/releases") }
            filter {
                includeModule("io.github.gaming32", "signature-changer")
                includeGroupAndSubgroups("net.ornithemc")
                includeGroup("ploceus")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.wagyourtail.xyz/snapshots") }
            forRepository { maven("https://maven.wagyourtail.xyz/releases") }
            filter {
                includeGroupAndSubgroups("xyz.wagyourtail")
            }
        }
        maven("https://maven.taumc.org/releases") {
            content {
                includeGroupAndSubgroups("org.taumc")
            }
        }
        exclusiveContent {
            forRepository { maven("https://nexus.gtnewhorizons.com/repository/public/") }
            filter {
                includeGroupAndSubgroups("com.gtnewhorizons")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            content {
                includeGroupAndSubgroups("net.minecraftforge")
            }
        }
        maven {
            name = "sponge"
            url = uri("https://repo.spongepowered.org/maven-public/")
            content {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
    }

    plugins {
        id("org.taumc.gradle.versioning") version(extra["taugradle_version"].toString())
        id("org.taumc.gradle.publishing") version(extra["taugradle_version"].toString())
        id("net.neoforged.moddev") version "2.0.134"
        id("net.neoforged.moddev.legacyforge") version "2.0.134"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
    id("dev.kikugie.stonecutter") version(extra["stonecutter_version"].toString())
}

rootProject.name = "celeritas"

includeBuild("plugins/celeritas-mdg-plugin")
includeBuild("plugins/celeritas-unimined-plugin")
include("common")

val versionFilter: (String) -> Boolean =
    if (extra.has("celeritas_target_versions")) {
        val versions: List<String> = extra["celeritas_target_versions"].toString().split(",")
        val pred: (String) -> Boolean = { ver -> versions.any { stonecutter.eval(ver, it) } }
        pred
    } else if (extra.has("celeritas_target_versions_pattern")) {
        val regex = Regex(extra["celeritas_target_versions_pattern"].toString())
        val pred: (String) -> Boolean = { ver -> regex.containsMatchIn(ver) }
        pred
    } else {
        { _ -> false }
    }

val subprojectFilter: ((String) -> Boolean)? =
    if (extra.has("celeritas_target_subprojects")) {
        val subprojects: List<String> = extra["celeritas_target_subprojects"].toString().split(",")
        val pred: (String) -> Boolean = { name -> subprojects.contains(name) }
        pred
    } else if (extra.has("celeritas_target_subprojects_pattern")) {
        val regex = Regex(extra["celeritas_target_subprojects_pattern"].toString())
        val pred: (String) -> Boolean = { name -> regex.containsMatchIn(name) }
        pred
    } else {
        null
    }

fun isVersionIncluded(ver: String): Boolean {
    return versionFilter(ver.substringBefore('-'))
}

var includedProjectCount = 0

if(file("forge1710").exists() && isVersionIncluded("1.7.10")) {
    include("forge1710")
    includedProjectCount++
}

if(file("forge122").exists() && isVersionIncluded("1.12.2")) {
    include("forge122")
    includedProjectCount++
}

fun <T> createStonecutterProject(subprojectFolder: String, versions: List<T>, mcVersionGetter: (version: T) -> String = { v -> v.toString() }, action: TreeBuilder.(versions: List<T>) -> Unit) {
    if (subprojectFilter != null && !subprojectFilter(subprojectFolder)) {
        println("Skipping project $subprojectFolder by request")
        return
    }
    if (!file(subprojectFolder).exists()) {
        return
    }
    if (!versions.any { isVersionIncluded(mcVersionGetter.invoke(it)) }) {
        return
    }
    val filteredVersions = versions.filter { versions[0] == it || isVersionIncluded(mcVersionGetter.invoke(it)) }
    val subprojectPath = ":$subprojectFolder"
    include(subprojectPath)
    includedProjectCount++
    stonecutter {
        create(subprojectPath) {
            action.invoke(this, filteredVersions)
        }
    }
}

createStonecutterProject("ornithe", listOf("1.2.5", "1.0.0-beta.7.3", "1.0.0-beta.8.1", "1.7.10", "1.8.9")) { versions ->
    centralScript = "build.gradle.kts"
    versions(versions)
}

data class CeleritasTarget(val friendlyName: String, val loaders: List<String>, val semanticName: String = friendlyName)

createStonecutterProject("modern", listOf(
        CeleritasTarget("1.20.1", listOf("forge", "fabric")),
        CeleritasTarget("1.16.5", listOf("forge")),
        CeleritasTarget("1.18.2", listOf("forge")),
        //CeleritasTarget("1.20.4", listOf("neoforge")),
        CeleritasTarget("1.21.1", listOf("fabric", "neoforge")),
        CeleritasTarget("26.1.1", listOf("neoforge"))
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

if (includedProjectCount == 0) {
    println("WARNING: No projects were selected. Set celeritas_target_versions or celeritas_target_versions_pattern to target specific versions.")
}
