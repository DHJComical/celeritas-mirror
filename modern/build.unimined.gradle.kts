import bs.ModLoader
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector

plugins {
    id("xyz.wagyourtail.unimined") version "1.3.15-SNAPSHOT"
    id("celeritas.platform-conventions")
}

group = "org.embeddedt"
version = rootProject.version

// Mod loader is always Forge <1.17

val minecraftVersion = ModLoader.getMinecraftVersion(project)!!

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

unimined.minecraft {
    combineWith(
            project(":common"),
            project(":common").sourceSets.getByName("main")
    )

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
    val generatedATPath = layout.buildDirectory.file("generated/accesstransformer.cfg").get().asFile
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
    implementation("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")

    compileOnly("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")

    ModDependencyCollector.obtainDeps(project) { cfg, dep ->
        dependencies.add(cfg, dep)
    }
}