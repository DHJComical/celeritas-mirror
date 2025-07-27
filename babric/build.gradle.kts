import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.embeddedt.embeddium.gradle.build.conventions.LWJGLHelper
import org.embeddedt.embeddium.gradle.unimined.ProductionJarHelper
import xyz.wagyourtail.unimined.api.minecraft.EnvType
import xyz.wagyourtail.unimined.api.minecraft.task.AbstractRemapJarTask
import java.net.URI

plugins {
    id("celeritas.platform-conventions")
    id("celeritas-unimined-plugin")
    id("xyz.wagyourtail.unimined") version "1.3.15-SNAPSHOT"
}

repositories {
    maven("https://maven.fabricmc.net")
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

unimined.minecraft {
    combineWith(project(":common"), project(":common").sourceSets.getByName("main"))

    version = versionData.uniminedVersion
    side = if(isClientServerSplit) EnvType.CLIENT else EnvType.COMBINED

    if (versionData.metadataURL != null) {
        minecraftData.metadataURL = versionData.metadataURL
    }

    mappings {
        calamus()
        feather(featherVersion)
    }

    fabric {
        loader("0.16.14")
    }

    minecraftRemapper.config {
        ignoreConflicts(true)
    }

    runs.config("client") {
        javaVersion = JavaVersion.VERSION_21
    }

    if (!isClientServerSplit) {
        runs.config("server") {
            enabled = false
        }
    }
}

dependencies {
    implementation("org.joml:joml:1.10.5")
    shadow("org.joml:joml:1.10.5")
    implementation("it.unimi.dsi:fastutil:8.5.15")

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
}

val remapJar = tasks.named<AbstractRemapJarTask>("remapJar") {
    manifest {
        attributes(mapOf("Calamus-Generation" to "1"))
    }
}

ProductionJarHelper.createShadowRemapJar(project)
LWJGLHelper.convertLwjgl2To3(project)
ProductionJarHelper.configureProcessedResources(project)

tasks.register("packageJar", Copy::class) {
    from(tasks.named<ShadowJar>("shadowRemapJar").get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}