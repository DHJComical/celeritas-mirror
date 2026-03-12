plugins {
    `java-library`
    id("java-gradle-plugin") // so we can assign and ID to our plugin
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.0")
    implementation("xyz.wagyourtail.unimined:xyz.wagyourtail.unimined.gradle.plugin:1.3.16-SNAPSHOT")
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.neoforged.net/releases") }
        filter {
            includeGroup("net.neoforged")
            includeGroup("net.minecraftforge")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter {
            includeGroup("net.fabricmc")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.wagyourtail.xyz/snapshots") }
        forRepository { maven("https://maven.wagyourtail.xyz/releases") }
        filter {
            includeGroupAndSubgroups("xyz.wagyourtail")
        }
    }
}

gradlePlugin {
    plugins {
        register("celeritas-unimined-plugin") {
            id = "celeritas-unimined-plugin"
            implementationClass = "org.embeddedt.embeddium.gradle.unimined.UniminedPlugin"
        }
    }
}