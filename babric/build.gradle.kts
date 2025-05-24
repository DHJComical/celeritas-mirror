import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.embeddedt.embeddium.gradle.build.conventions.LWJGLHelper
import org.embeddedt.embeddium.gradle.build.conventions.ProductionJarHelper
import xyz.wagyourtail.unimined.api.minecraft.EnvType
import xyz.wagyourtail.unimined.api.minecraft.task.AbstractRemapJarTask

plugins {
    id("celeritas.platform-conventions")
    id("celeritas.unimined-platform-conventions")
}

repositories {
    maven("https://maven.fabricmc.net")
}

group = "org.embeddedt"
version = rootProject.version

evaluationDependsOn(":common")

data class VersionData(val uniminedVersion: String)

val versionDataMap = mapOf(
        "1.0.0-beta.7.3" to VersionData("b1.7.3"),
        "1.0.0-beta.8.1" to VersionData("b1.8.1"),
        "1.2.5" to VersionData("1.2.5")
)

val versionData = versionDataMap.getValue(project.name)

base.archivesName = "celeritas-fabriclike-mc${versionData.uniminedVersion.replace(Regex("^1\\."), "")}"

unimined.minecraft {
    combineWith(project(":common"), project(":common").sourceSets.getByName("main"))

    version = versionData.uniminedVersion
    side = EnvType.CLIENT

    mappings {
        calamus()
        feather(23)
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