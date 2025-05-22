import org.embeddedt.embeddium.gradle.build.conventions.LWJGLHelper
import org.embeddedt.embeddium.gradle.build.conventions.ProductionJarHelper
import xyz.wagyourtail.unimined.api.minecraft.EnvType

plugins {
    id("celeritas.platform-conventions")
    id("celeritas.unimined-platform-conventions")
}

repositories {
    maven("https://api.modrinth.com/maven")
}

group = "org.embeddedt"
version = rootProject.version

evaluationDependsOn(":common")

unimined.minecraft {
    combineWith(project(":common"), project(":common").sourceSets.getByName("main"))

    version = "b1.7.3"
    side = EnvType.CLIENT

    mappings {
        babricIntermediary()
        biny("2f404bc")
    }

    babric {
        loader("0.15.6-babric.2")
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
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
}

LWJGLHelper.convertLwjgl2To3(project)

ProductionJarHelper.configureProcessedResources(project)