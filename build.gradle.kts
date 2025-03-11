import org.taumc.gradle.publishing.api.PublishChannel
import org.taumc.gradle.publishing.api.minecraft.ModEnvironment
import org.taumc.gradle.publishing.api.minecraft.ModLoader
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("org.taumc.gradle.versioning") version "0.3.18"
    id("org.taumc.gradle.publishing") version "0.3.18"
}

project.version = tau.versioning.version(rootProject.properties["project_base_version"].toString(), rootProject.properties["release_channel"])
println("Celeritas: ${tau.versioning.version}")

//project(":forge1710")

evaluationDependsOnChildren()

val publishTask = tau.publishing.publish {

    useTauGradleVersioning()
    changelog = "Further improvements to overall system stability and other minor adjustments have been made to enhance the user experience."

    discord {
        supportAllChannelsExcluding(PublishChannel.RELEASE)

        webhookURL = providers.environmentVariable("DISCORD_WEBHOOK")
        username = "Celeritas Test Builds"
        avatarURL = "https://git.taumc.org/embeddedt/celeritas/raw/branch/stonecutter/modern/src/main/resources/icon.png"

        setMessage("Celeritas dev build")
    }

    val archaic = project(":forge1710")
    dependsOn(archaic.tasks.named("remapJar"))

    modArtifact {
        files(project.provider { archaic.tasks.named<RemapJarTask>("remapJar").get().asJar.archiveFile })

        minecraftVersionRange = "1.7.10"
        javaVersions.add(JavaVersion.VERSION_21)

        environment = ModEnvironment.CLIENT_ONLY
        modLoaders.add(ModLoader.LEXFORGE)
    }
}
