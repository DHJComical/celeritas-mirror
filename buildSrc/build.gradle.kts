plugins {
    `kotlin-dsl`
    `java-library`
    id("java-gradle-plugin") // so we can assign and ID to our plugin
}

dependencies {
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.0")
    implementation("dev.kikugie:stonecutter:0.7-beta.2")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.kikugie.dev/releases")
    maven("https://maven.kikugie.dev/snapshots")
}

gradlePlugin {
    plugins {
        register("embeddium-fabric-remapper") {
            id = "embeddium-fabric-remapper"
            implementationClass = "org.embeddedt.embeddium.gradle.fabric.remapper.RemapperPlugin"
        }
        // here we register our plugin with an ID
        register("embeddium-fabric-module-finder") {
            id = "embeddium-fabric-module-finder"
            implementationClass = "org.embeddedt.embeddium.gradle.fabric.remapper.ModuleFinderPlugin"
        }
    }
}
