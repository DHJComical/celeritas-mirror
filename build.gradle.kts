import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import org.taumc.gradle.publishing.api.PublishChannel
import org.taumc.gradle.publishing.api.minecraft.ModEnvironment
import org.taumc.gradle.publishing.api.minecraft.ModLoader
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("org.taumc.gradle.versioning")
    id("org.taumc.gradle.publishing")
}

project.version = tau.versioning.version(rootProject.properties["project_base_version"].toString(), rootProject.properties["release_channel"])
println("Celeritas: ${tau.versioning.version}")

//project(":forge1710")

evaluationDependsOnChildren()

val modernStonecutter = if (findProject(":modern") != null) {
    project(":modern").extensions.getByType(StonecutterControllerExtension::class.java)
} else {
    null
}

val publishTask = tau.publishing.publish {
    useTauGradleVersioning()
    changelog = "Further improvements to overall system stability and other minor adjustments have been made to enhance the user experience."

    discord {
        supportAllChannelsExcluding(PublishChannel.RELEASE)

        webhookURL = providers.environmentVariable("DISCORD_WEBHOOK")
        username = "Celeritas Test Builds"
        avatarURL = "https://git.taumc.org/embeddedt/celeritas/raw/branch/stonecutter/modern/src/main/resources/icon.png"

        testBuildPreset("Celeritas", "https://git.taumc.org/embeddedt/celeritas")
    }

    if (System.getenv("CELERITAS_PUBLISHING_TEST") == null) {
        github("Gitea") {
            supportAllChannels()
            uploadArtifacts = false

            apiEndpoint = "https://git.taumc.org/api/v1/"

            accessToken = System.getenv("GITEA_TOKEN")
            repository = "embeddedt/celeritas"
            tagName = tau.versioning.releaseTag
        }
    }

    if (findProject(":forge1710") != null) {
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

    if (modernStonecutter != null) {
        modernStonecutter.tree.values.flatMap { it.values }.forEach {
            val name = it.metadata.project
            val ourLoader = bs.ModLoader.fromName(name) ?: throw IllegalArgumentException("No modloader for ${name}")

            if (ourLoader == bs.ModLoader.FABRIC) {
                // Skip Fabric for now as the payload to Discord is too large otherwise.
                return@forEach
            }

            modArtifact {
                files(it.project.provider { it.project.tasks.named<Copy>("packageJar").get().inputs.files.singleFile })

                minecraftVersionRange = bs.ModLoader.getMinecraftVersion(name)
                javaVersions.add(JavaVersion.VERSION_21)

                environment = ModEnvironment.CLIENT_ONLY

                modLoaders.add(when(ourLoader) {
                    bs.ModLoader.FABRIC -> ModLoader.FABRIC
                    bs.ModLoader.FORGE -> ModLoader.LEXFORGE
                    bs.ModLoader.NEOFORGE -> ModLoader.NEOFORGE
                })
            }
        }
    }
}