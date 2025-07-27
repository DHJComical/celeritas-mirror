import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.embeddedt.embeddium.gradle.build.conventions.LWJGLHelper
import org.embeddedt.embeddium.gradle.unimined.ProductionJarHelper
import xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles

plugins {
    id("celeritas.platform-conventions")
    id("celeritas-unimined-plugin")
    id("xyz.wagyourtail.unimined") version "1.3.15-SNAPSHOT"
    id("xyz.wagyourtail.jvmdowngrader") version "1.1.3"
}

group = "org.embeddedt"
version = rootProject.version

data class VersionData(val mcpVersion: String, val forgeVersion: String)

val versionDataMap = mapOf(
        "1.12.2" to VersionData("39-1.12", "14.23.5.2860"),
        "1.10.2" to VersionData("39-1.12", "12.18.3.2511")
)

val minecraftVersion = project.name

val versionData = versionDataMap.getValue(project.name)

base.archivesName = "celeritas-forge-${project.name}"

val modCompileOnly by configurations.creating
configurations.compileOnly.get().extendsFrom(modCompileOnly)

if (stonecutter.eval(minecraftVersion, "<=1.10.2")) {
    configurations {
        all {
            resolutionStrategy.force("it.unimi.dsi:fastutil:7.1.0")
            resolutionStrategy.force("com.google.code.gson:gson:2.10.1")
        }
    }
}
unimined.minecraft {
    combineWith(
            project(":common"),
            project(":common").sourceSets.getByName("main")
    )

    version(project.name)

    mappings {
        searge()
        mcp("stable", versionData.mcpVersion)
    }

    minecraftForge {
        loader(versionData.forgeVersion)
        mixinConfig("mixins.celeritas.json")
        accessTransformer(rootProject.file("forge122/src/main/resources/META-INF/celeritas_at.cfg"))
    }

    val downgradeClient = tasks.register<DowngradeFiles>("downgradeClient") {
        inputCollection = sourceSet.output.classesDirs + sourceSet.runtimeClasspath
        classpath = files()
    }

    runs.config("client") {
        javaVersion = JavaVersion.VERSION_1_8
        classpath = downgradeClient.get().outputCollection + files(jvmdg.getDowngradedApi(JavaVersion.VERSION_1_8))
        systemProperty("fml.coreMods.load", "zone.rong.mixinbooter.MixinBooterPlugin,me.eigenraven.lwjgl3ify.core.Lwjgl3ifyCoremod,org.taumc.celeritas.core.CeleritasLoadingPlugin")
    }

    runs.config("server") {
        enabled = false
    }

    mods {
        remap(modCompileOnly) {
            // no additional configuration
        }
    }
}

dependencies {
    "shadow"("org.joml:joml:1.10.5")
    implementation("org.joml:joml:1.10.5")
    implementation("zone.rong:mixinbooter:10.5")
    compileOnly("com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:1.0.7") {
        exclude(group = "org.apache.logging.log4j")
    }
    modCompileOnly("maven.modrinth:fluidlogged-api:3.0.6")
}

tasks.named("preRunClient") {
    dependsOn("downgradeClient")
}

LWJGLHelper.convertLwjgl2To3(project)

ProductionJarHelper.configureProcessedResources(project)
ProductionJarHelper.setupLegacyFMLManifest(project)
ProductionJarHelper.createShadowRemapJar(project)

tasks.register("packageJar", Copy::class) {
    from(tasks.named<ShadowJar>("shadowRemapJar").get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}