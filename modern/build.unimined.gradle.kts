import bs.ModLoader
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.embeddedt.embeddium.gradle.build.conventions.ShadowHelper
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector
import org.embeddedt.embeddium.gradle.unimined.ProductionJarHelper
import org.gradle.kotlin.dsl.named

plugins {
    id("xyz.wagyourtail.unimined") version "1.3.16-SNAPSHOT"
    id("celeritas.platform-conventions")
    id("celeritas-unimined-plugin")
    id("xyz.wagyourtail.jvmdowngrader") version "1.1.3"
}

group = "org.embeddedt"
version = rootProject.version

// Mod loader is always Forge <1.17

val minecraftVersion = ModLoader.getMinecraftVersion(project)!!

base.archivesName = "celeritas-forge-mc${minecraftVersion.replace("^1\\.".toRegex(), "")}"

val modCompileOnly = configurations.create("modCompileOnly")
configurations.compileClasspath.get().extendsFrom(modCompileOnly)
val modApi = configurations.create("modApi")
configurations.compileClasspath.get().extendsFrom(modApi)

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion("9.6")
            because("Force ASM to a modern version that supports Java 21")
        }
        if (requested.group == "org.lwjgl") {
            useVersion("3.3.3")
            because("Force LWJGL to a modern version that supports Java 21")
        }
    }
}

val generatedATPath = layout.buildDirectory.file("generated/accesstransformer.cfg").get().asFile
generatedATPath.parentFile.mkdirs()

unimined.minecraft {
    listOf("main", "lwjglCommon", "lwjgl3").forEach {
        combineWith(project(":common"), project(":common").sourceSets.getByName(it))
    }

    version(minecraftVersion)

    mappings {
        searge()
        devFallbackNamespace("searge")
        mojmap()
        val parchmentVersion = versionedProperty("parchment_version")
        if (parchmentVersion != null) {
            val parchmentData = parchmentVersion.split(":")
            parchment(parchmentData[0], parchmentData[1])
        }
    }

    mods {
        remap(modCompileOnly, modApi)
    }

    val embeddiumAccessWidener = rootProject.file("modern/src/main/resources/embeddium.accesswidener")
    minecraftForge {
        loader(versionedProperty("forge")!!.split("-")[1])
        mixinConfig(project.extra.get("celeritasMixinConfigs") as List<String>)
        accessTransformer(aw2at(embeddiumAccessWidener, generatedATPath))
    }

    runs {
        config("client") {
            workingDir = project.layout.projectDirectory.dir("run").asFile
            javaVersion = JavaVersion.VERSION_21
            val modSets = listOf(sourceSets.main.get(), project(":common").sourceSets.getByName("main"))
            val modClasses = modSets.flatMap { listOf(it.output.resourcesDir) + it.output.classesDirs }.joinToString(File.pathSeparator) { "embeddium%%" + it!!.absolutePath }
            environment.put("MOD_CLASSES", modClasses)
        }
        config("server") {
            enabled = false
        }
    }
}

dependencies {
    val jomlDep = "org.joml:joml:${rootProject.property("joml_version")}"
    implementation(jomlDep)
    shadow(jomlDep)

    val mixinExtrasVersion = rootProject.property("mixinextras").toString()
    implementation("io.github.llamalad7:mixinextras-common:${mixinExtrasVersion}")
    shadow("io.github.llamalad7:mixinextras-common:${mixinExtrasVersion}")

    compileOnly("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")

    ModDependencyCollector.obtainDeps(project) { cfg, dep ->
        dependencies.add(cfg, dep)
    }
}

ProductionJarHelper.configureProcessedResources(project)
ShadowHelper.createShadowRemapJar(project)
ProductionJarHelper.configureRemapJar(project)

tasks.named<ProcessResources>("processResources") {
    from(generatedATPath) {
        into("META-INF/")
    }
}

tasks.named<ShadowJar>("shadowRemapJar") {
    archiveClassifier.set("pre-downgrade")
    relocate("com.llamalad7.mixinextras", "org.embeddedt.embeddium.impl.shadow.mixinextras")
    relocate("org.joml", "org.embeddedt.embeddium.impl.shadow.joml")
}

val customDowngrade = tasks.register<xyz.wagyourtail.jvmdg.gradle.task.DowngradeJar>("downgradeShadowRemapJar") {
    inputFile.set(tasks.named<ShadowJar>("shadowRemapJar").get().archiveFile)
    archiveClassifier.set("post-downgrade")
}

tasks.register<xyz.wagyourtail.jvmdg.gradle.task.ShadeJar>("shadeDowngradedShadowRemapJar") {
    inputFile.set(customDowngrade.flatMap { it.archiveFile })
    archiveClassifier.set("")
}

tasks.register("packageJar", Copy::class) {
    from(tasks.named<xyz.wagyourtail.jvmdg.gradle.task.ShadeJar>("shadeDowngradedShadowRemapJar").get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}

tasks.named("genIntellijRuns") {
    enabled = false
}
