plugins {
    `kotlin-dsl`
    `java-library`
    id("java-gradle-plugin") // so we can assign and ID to our plugin
}

dependencies {
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation("org.ow2.asm:asm-commons:9.10.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
    implementation("dev.kikugie:stonecutter:0.10-alpha.8")
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter {
            includeGroupAndSubgroups("net.fabricmc")
        }
    }
    exclusiveContent {
        forRepositories(
            maven("https://maven.kikugie.dev/releases"),
            maven("https://maven.kikugie.dev/snapshots")
        )
        filter {
            includeGroupAndSubgroups("dev.kikugie")
        }
    }
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
