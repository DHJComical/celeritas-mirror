import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import kotlin.text.split

plugins {
    id("embeddium-mdg-remapper")
    id("net.neoforged.moddev")
    id("maven-publish")
    id("celeritas.platform-conventions")
}

group = "org.embeddedt"
version = rootProject.version

neoForge {
    enable {
        version = "21.11.14-beta"
    }

    val parchmentVersion = rootProject.properties["parchment_version_1_21_11"]?.toString()

    if (parchmentVersion != null) {
        val parchmentData = parchmentVersion.split(":")
        parchment {
            minecraftVersion = parchmentData[0]
            mappingsVersion = parchmentData[1]
        }
    }

    mods {
        create("celeritas") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            ideName.set("")
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("modern/src/main/resources/icon.png"))
}

dependencies {
    implementation(project(":common")) {
        isTransitive = false
    }
}