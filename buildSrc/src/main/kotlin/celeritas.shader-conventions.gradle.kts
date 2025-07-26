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

val modMixinConfigs = project.extra.get("celeritasMixinConfigs") as MutableList<String>
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