import bs.ModLoader
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.embeddedt.embeddium.gradle.build.extensions.celeritasMixinConfigs
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty

plugins {
    id("java-library")
    id("com.gradleup.shadow")
}

val stonecutterExt = project.extensions.getByType<StonecutterBuildExtension>()
// Must match the split between modern_java and postmodern_java in celeritas.platform-conventions.
val postmodern = stonecutterExt.compare(stonecutterExt.current.version, "1.21.11") >= 0

// 26.1's feature dispatcher already batches entity draws, so batching is only compiled before it.
val sharedSourceSets = if (postmodern) arrayOf("shaders") else arrayOf("shaders", "batching")

sourceSets {
    main {
        sharedSourceSets.forEach { it ->
            java.srcDir("src/main/${it}_java")
            resources.srcDir("src/main/${it}_resources")
        }
        java.srcDir(if (postmodern) "src/main/shaders_postmodern_java" else "src/main/shaders_modern_java")
    }
}

val modMixinConfigs = project.celeritasMixinConfigs
if (!postmodern) {
    modMixinConfigs.add("oculus-batched-entity-rendering.mixins.json")
}
modMixinConfigs.addAll(listOf(
        "mixins.oculus.json",
        "mixins.oculus.compat.sodium.json",
        "mixins.oculus.compat.dh.json"
))
if (!postmodern) {
    // Their mixins only exist in shaders_modern_java.
    modMixinConfigs.addAll(listOf(
            "mixins.oculus.compat.indigo.json",
            "mixins.oculus.compat.indium.json",
            "mixins.oculus.compat.pixelmon.json"
    ))
}

dependencies {
    compileOnly("maven.modrinth:distanthorizonsapi:3.0.0")

    val glslTransformLib = "org.taumc:glsl-transformation-lib:${rootProject.property("glsl_transformation_lib_version")}:fat"
    val jcpp = "org.anarres:jcpp:1.4.14"
    val shaderDeps = arrayOf(glslTransformLib, jcpp)

    // 26.1 runs from the regular runtime classpath, and ModDevGradle rejects additionalRuntimeClasspath there.
    val additionalDepConfig = if (postmodern) null else listOf("additionalRuntimeClasspath").firstOrNull { it -> configurations.findByName(it) != null }

    shaderDeps.forEach {
        implementation(it) {
            isTransitive = false
        }
        shadow(it) {
            isTransitive = false
        }
        if (additionalDepConfig != null) {
            add(additionalDepConfig, it)
        }
    }
}

// The Flywheel backend is only compiled for versions that pin a Flywheel build, matching the colorwheel constant.
val flywheelVersion = versionedProperty("flywheel_version")

if (flywheelVersion != null) {
    sourceSets {
        main {
            java.srcDir("src/main/flywheel_java")
            resources.srcDir("src/main/flywheel_resources")
        }
    }

    modMixinConfigs.add("mixins.oculus.compat.flywheel.json")

    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "Create Maven"
                    url = uri("https://maven.createmod.net")
                }
            }
            filter {
                includeGroup("dev.engine-room.flywheel")
            }
        }
    }

    dependencies {
        val modLoader = requireNotNull(ModLoader.fromProject(project))
        val minecraftVersion = ModLoader.getMinecraftVersion(project)
        // Forge and Fabric jars are published obfuscated and need the loader's remapping configuration.
        val configurationName = if (modLoader == ModLoader.NEOFORGE) "compileOnly" else "modCompileOnly"
        add(configurationName, "dev.engine-room.flywheel:flywheel-${modLoader.friendlyName}-${minecraftVersion}:${flywheelVersion}") {
            isTransitive = false
        }
    }
}
