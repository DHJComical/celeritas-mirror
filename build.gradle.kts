import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import org.taumc.gradle.publishing.api.PublishChannel
import org.taumc.gradle.minecraft.ModEnvironment
import org.taumc.gradle.minecraft.ModLoader

plugins {
    id("org.taumc.gradle.versioning")
    id("org.taumc.gradle.publishing")
}

project.version = tau.versioning.version(rootProject.findProperty("project_base_version").toString(), rootProject.findProperty("release_channel"))
println("Celeritas: ${tau.versioning.version}")

//project(":forge1710")

val modernStonecutter = if (findProject(":modern") != null) {
    // Only :modern needs to be evaluated eagerly, to read the Stonecutter controller extension below.
    evaluationDependsOn(":modern")
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

    if (System.getenv("GITEA_TOKEN") != null) {
        github("Gitea") {
            supportAllChannels()
            uploadArtifacts = false

            apiEndpoint = "https://git.taumc.org/api/v1/"

            accessToken = System.getenv("GITEA_TOKEN")
            repository = "embeddedt/celeritas"
            tagName = tau.versioning.releaseTag
        }
    }

    if (modernStonecutter != null) {
        modernStonecutter.tree.values.flatMap { it.values }.forEach {
            val name = it.metadata.project
            val ourLoader = bs.ModLoader.fromName(name) ?: throw IllegalArgumentException("No modloader for ${name}")

            if (ourLoader == bs.ModLoader.FABRIC || modernStonecutter.eval(it.metadata.version, "<1.20.1")) {
                // Skip Fabric for now as the payload to Discord is too large otherwise.
                return@forEach
            }

            // Referenced by path so that the version project is only configured when this task actually runs.
            val versionProject = it.project

            // Minecraft 26.1 and later run on Java 25.
            val javaVersion = if (modernStonecutter.eval(it.metadata.version, ">=26.1")) {
                JavaVersion.VERSION_25
            } else {
                JavaVersion.VERSION_21
            }

            dependsOn("${versionProject.path}:packageJar")

            modArtifact {
                files(versionProject.provider {
                    versionProject.tasks.named<Copy>("packageJar").get().inputs.files.singleFile
                })

                minecraftVersionRange = bs.ModLoader.getMinecraftVersion(name)
                javaVersions.add(javaVersion)

                environment.set(ModEnvironment.CLIENT_ONLY)

                modLoaders.add(when(ourLoader) {
                    bs.ModLoader.FABRIC -> ModLoader.FABRIC
                    bs.ModLoader.FORGE -> ModLoader.LEXFORGE
                    bs.ModLoader.NEOFORGE -> ModLoader.NEOFORGE
                })
            }
        }
    }
}