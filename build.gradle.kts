
plugins {
    id("org.taumc.gradle.versioning") version "0.3.9"
}

project.version = tau.versioning.version(rootProject.properties["project_base_version"].toString(), rootProject.properties["release_channel"])
println("Celeritas: ${tau.versioning.version}")