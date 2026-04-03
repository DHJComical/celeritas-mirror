import bs.ModLoader
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

plugins {
    id("com.gradleup.shadow")
    id("java-library")
}

evaluationDependsOn(":common")

repositories {
    mavenCentral()
    maven {
        name = "wagyourtail releases"
        url = uri("https://maven.wagyourtail.xyz/releases")
        content {
            includeGroupAndSubgroups("xyz.wagyourtail")
        }
    }
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven")
        content {
            includeGroupAndSubgroups("org.spongepowered")
        }
    }
    maven {
        url = uri("https://maven.cleanroommc.com")
        content {
            includeGroup("zone.rong")
        }
    }
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
        content {
            includeGroupAndSubgroups("com.gtnewhorizons")
        }
    }
    maven {
        name = "Su5eD Maven"
        url = uri("https://maven.su5ed.dev/releases")
        content {
            includeGroupAndSubgroups("dev.su5ed.sinytra")
            includeGroupAndSubgroups("org.sinytra")
        }
    }
    maven {
        name = "Fabric Maven"
        url = uri("https://maven.fabricmc.net/")
        content {
            includeGroupAndSubgroups("net.fabricmc")
        }
    }
    /*
    maven {
        name = "GTCEu Maven"
        url = uri("https://maven.gtceu.com/")
    }
    maven {
        url = uri("https://files.prismlauncher.org/maven")
        metadataSources { artifact() }
    }
     */
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    /*
    maven {
        // location of the maven that hosts JEI files
        name = "Progwml6 maven"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
    maven {
        name = "Glass-Launcher"
        url = uri("https://maven.glass-launcher.net/releases/")
    }
     */
    maven {
        name = "taumc releases"
        url = uri("https://maven.taumc.org/releases")
        content {
            includeGroupAndSubgroups("org.taumc")
        }
    }
}

dependencies {
    val lombokVersion = rootProject.properties["lombok_version"].toString()
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val modLoader: ModLoader? = ModLoader.fromProject(project)
val minecraftVersion = ModLoader.getMinecraftVersion(project)

val stonecutterExt = project.extensions.findByType<StonecutterBuildExtension>()

val modMixinConfigs = mutableListOf("embeddium.mixins.json")
project.extra.set("celeritasMixinConfigs", modMixinConfigs)

if (generateSequence(project) { it.parent }.any { it.name == "modern" }) {
    if (stonecutterExt?.constants?.getOrDefault("settings_gui", false) ?: false) {
        sourceSets {
            main {
                java.srcDir("src/main/gui_java")
            }
        }
    }
    val platformVersionSourceDir = if((stonecutterExt?.compare(stonecutterExt.current.version, "1.21.11") ?: -1) >= 0) {
        "postmodern"
    } else {
        "modern"
    }
    sourceSets {
        main {
            java.srcDir("src/main/${platformVersionSourceDir}_java")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val inputProps = mutableMapOf(
            "forgeid" to if (modLoader == ModLoader.NEOFORGE) "neoforge" else "forge",
            "minecraft" to (versionedProperty("minecraft_dependency") ?: ""),
            "mod_version" to version,
            "mod_id" to rootProject.property("mod_id"),
            "mod_display_name" to rootProject.property("mod_display_name"),
            "mod_description" to rootProject.property("mod_description"),
            "homepage_url" to rootProject.property("homepage_url"),
            "sources_url" to rootProject.property("sources_url"),
            "issue_tracker_url" to rootProject.property("issue_tracker_url"),
            "license" to rootProject.property("license"),
            "fabricloader" to rootProject.property("fabricloader"),
            "contributors" to rootProject.property("mod_contributors").toString(),
            "authors" to rootProject.property("mod_authors").toString(),
            "fabric_api_modules" to rootProject.property("fabric_api_modules")
    )

    stonecutterExt?.let {
        val mixinCompatLevel = if (it.eval(minecraftVersion, "<1.17")) {
            "JAVA_8"
        } else if (it.eval(minecraftVersion, "<1.18")) {
            "JAVA_16"
        } else {
            "JAVA_17"
        }
        inputProps.put("mixinCompatLevel", mixinCompatLevel)
    }

    inputProps.forEach { (key, value) ->
        inputs.property(key, value)
    }

    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "quilt.mod.json", "embeddium.mixins.json")) {
        expand(inputProps)
    }

    if (modLoader == ModLoader.FORGE || modLoader == ModLoader.NEOFORGE) {
        exclude("fabric.mod.json")
    } else if (modLoader == ModLoader.FABRIC) {
        exclude("META-INF/mods.toml", "META-INF/accesstransformer.cfg", "pack.mcmeta")
    }

    doLast {
        if (modLoader == ModLoader.NEOFORGE) {
            val modsTOMLFiles = fileTree(outputs.files.asPath) {
                include("META-INF/mods.toml")
            }

            modsTOMLFiles.forEach { file ->
                file.appendText("\n\n[[mixins]]\nconfig = \"${rootProject.properties["mod_id"]}.mixins.json\"")

                val outputPath = Path.of(outputs.files.asPath)
                        .resolve("META-INF/neoforge.mods.toml")

                Files.copy(file.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        if (modLoader == ModLoader.FABRIC) {
            fileTree(outputs.files.asPath) {
                include("fabric.mod.json")
            }.forEach { file ->
                val slurper = JsonSlurper()
                val parse = slurper.parse(file) as MutableMap<String, Any>

                parse["mixins"] = modMixinConfigs.toList()

                file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(parse)))
            }
        }
    }
}