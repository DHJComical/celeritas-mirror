import bs.ModLoader
import org.embeddedt.embeddium.gradle.build.extensions.celeritasMixinConfigs
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty

plugins {
    id("java-library")
    id("com.gradleup.shadow")
}

sourceSets {
    main {
        arrayOf("shaders", "batching").forEach { it ->
            java.srcDir("src/main/${it}_java")
            resources.srcDir("src/main/${it}_resources")
        }
    }
}

val modMixinConfigs = project.celeritasMixinConfigs
modMixinConfigs.add("oculus-batched-entity-rendering.mixins.json")
modMixinConfigs.addAll(listOf(
        "mixins.oculus.json",
        "mixins.oculus.compat.sodium.json",
        "mixins.oculus.compat.indigo.json",
        "mixins.oculus.compat.indium.json",
        "mixins.oculus.compat.dh.json",
        "mixins.oculus.compat.pixelmon.json"
))

dependencies {
    compileOnly("maven.modrinth:distanthorizonsapi:3.0.0")

    val glslTransformLib = "org.taumc:glsl-transformation-lib:${rootProject.property("glsl_transformation_lib_version")}:fat"
    val jcpp = "org.anarres:jcpp:1.4.14"
    val shaderDeps = arrayOf(glslTransformLib, jcpp)

    val additionalDepConfig = listOf("additionalRuntimeClasspath").firstOrNull { it -> configurations.findByName(it) != null }

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

// The Iris code compiled for every shader target calls into the Flywheel backend.
val flywheelVersion = requireNotNull(versionedProperty("flywheel_version")) {
    "Shader targets compile the Flywheel backend; set flywheel_version_${ModLoader.getMinecraftVersion(project).replace('.', '_')} in gradle.properties"
}

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
