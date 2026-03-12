plugins {
    `java-library`
    id("java-gradle-plugin") // so we can assign and ID to our plugin
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("net.fabricmc:tiny-remapper:0.11.0")
    implementation("net.fabricmc:mapping-io:0.7.1")
    implementation("net.fabricmc:mapping-io-extras:0.7.1")
    implementation("net.neoforged:srgutils:1.0.0")
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.neoforged.net/releases") }
        filter {
            includeGroup("net.neoforged")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter {
            includeGroup("net.fabricmc")
        }
    }
}

gradlePlugin {
    plugins {
        register("embeddium-mdg-remapper") {
            id = "embeddium-mdg-remapper"
            implementationClass = "org.embeddedt.embeddium.gradle.mdg.remapper.MDGRemapperPlugin"
        }
    }
}