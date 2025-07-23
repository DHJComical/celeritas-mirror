plugins {
    id("java-library")
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("maven.modrinth:distanthorizonsapi:3.0.0")

    val glslTransformLib = "org.taumc:glsl-transformation-lib:${rootProject.property("glsl_transformation_lib_version")}:fat"
    val jcpp = "org.anarres:jcpp:1.4.14"
    val shaderDeps = arrayOf(glslTransformLib, jcpp)

    val additionalDepConfig = listOf("additionalRuntimeClasspath").first { it -> configurations.findByName(it) != null }

    shaderDeps.forEach {
        implementation(it) {
            isTransitive = false
        }
        shadow(it) {
            isTransitive = false
        }
        add(additionalDepConfig, it)
    }
}