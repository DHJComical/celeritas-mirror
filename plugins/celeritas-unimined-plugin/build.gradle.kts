plugins {
    `java-library`
    id("java-gradle-plugin") // so we can assign and ID to our plugin
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.0")
    implementation("xyz.wagyourtail.unimined:xyz.wagyourtail.unimined.gradle.plugin:1.3.15-SNAPSHOT")
}

repositories {
    mavenCentral()
    maven("https://maven.wagyourtail.xyz/releases")
    maven("https://maven.wagyourtail.xyz/snapshots")
}

gradlePlugin {
    plugins {
        register("celeritas-unimined-plugin") {
            id = "celeritas-unimined-plugin"
            implementationClass = "org.embeddedt.embeddium.gradle.unimined.UniminedPlugin"
        }
    }
}