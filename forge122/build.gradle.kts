import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.ObfuscationAttribute
import com.gtnewhorizons.retrofuturagradle.mcp.ApplySourceAccessTransformersTask
import com.gtnewhorizons.retrofuturagradle.minecraft.MinecraftTasks
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import org.embeddedt.embeddium.gradle.build.conventions.RFBArgs
import org.embeddedt.embeddium.gradle.build.conventions.ShadowHelper
import org.embeddedt.embeddium.gradle.mdg.remapper.ReobfuscateCodeAndMixinsTask

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.8"
    id("com.gradleup.shadow")
    id("embeddium-mdg-remapper")
    id("maven-publish")
}

group = "org.embeddedt"
version = rootProject.version

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

base.archivesName = "celeritas-forge-mc12.2"

val modCompileOnly by configurations.creating
configurations.compileOnly.get().extendsFrom(modCompileOnly)
val modRuntimeOnly by configurations.creating
configurations.runtimeOnly.get().extendsFrom(modRuntimeOnly)

val celeritasLwjglVersion = "3.3.3"

minecraft {
    javaCompatibilityVersion = 21
    mainLwjglVersion = 3
    mcVersion.set("1.12.2")
    lwjgl3Version = celeritasLwjglVersion
    RFBArgs.JAVA_17_ARGS.forEach { extraRunJvmArguments.add(it) }
}

repositories {
    maven {
        url = uri("https://maven.cleanroommc.com")
        content {
            includeGroup("zone.rong")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        // RetroFuturaGradle
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
        mavenContent {
            includeGroupByRegex("com\\.gtnewhorizons\\..+")
            includeGroup("com.gtnewhorizons")
        }
    }
    mavenLocal()
}

val lwjgl3ifyVersion = "1.0.1-38-g6410323"

val forgePatchDeps by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations {
    named("shadow") {
        attributes {
            attribute(ModUtils.DEOBFUSCATOR_TRANSFORMED, true)
        }
    }
}

dependencies {
    val lombokVersion = rootProject.properties["lombok_version"].toString()
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    implementation(project(":common")) {
        isTransitive = false
    }
    shadow(project(":common")) {
        isTransitive = false
    }
    "shadow"("org.joml:joml:1.10.5")
    implementation("org.joml:joml:1.10.5")
    implementation("zone.rong:mixinbooter:10.5")
    compileOnly("com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:1.0.11") {
        exclude(group = "org.apache.logging.log4j")
    }
    implementation("io.github.twilightflower:lwjgl3ify:${lwjgl3ifyVersion}:dev") {
        isTransitive = false
    }
    runtimeOnly("io.github.twilightflower:lwjgl3ify:${lwjgl3ifyVersion}:forgePatches") {
        isTransitive = false
    }
    forgePatchDeps("io.github.twilightflower:lwjgl3ify:${lwjgl3ifyVersion}:forgePatches") {
        isTransitive = false
    }
    "modRuntimeOnly"("curse.maven:ae2-223794:2747063")
    modCompileOnly("maven.modrinth:fluidlogged-api:3.0.6")
}

tasks.named("reobfJar").configure {
    enabled = false
}

tasks.register<ReobfuscateCodeAndMixinsTask>("celeritasRemapJar") {
    tsrgMappings = mcpTasks.srgFile("mcp-srg.srg")
    deobfMinecraftJar = mcpTasks.taskPackagePatchedMc.flatMap { it.archiveFile }
    classpath = sourceSets.main.get().compileClasspath
    archiveBaseName.set(base.archivesName)
    archiveClassifier.set("reobf")
    input = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    dependsOn(mcpTasks.taskGenerateForgeSrgMappings)
}

ShadowHelper.createShadowRemapJar(project, "celeritasRemapJar")

tasks.named("runClient") {
    enabled = false
}

tasks.register<RunMinecraftTask>("runRFBClient", RunMinecraftTask::class.java, com.gtnewhorizons.retrofuturagradle.util.Distribution.CLIENT).configure {
    val mcTasks = project.extensions.getByType<MinecraftTasks>()
    classpath(forgePatchDeps)
    classpath(mcpTasks.taskPackageMcLauncher)
    classpath(mcpTasks.taskPackagePatchedMc)
    classpath(mcpTasks.patchedConfiguration)
    classpath(tasks.named("jar"))
    classpath(configurations.runtimeClasspath)
    setup(project)
    dependsOn(
            mcpTasks.launcherSources.classesTaskName,
            mcTasks.taskDownloadVanillaAssets,
            mcpTasks.taskPackagePatchedMc,
            "jar"
    )
    mainClass.set("GradleStart")
    username.set(minecraft.username)
    userUUID.set(minecraft.userUUID)
    systemProperty("gradlestart.bouncerClient", "com.gtnewhorizons.retrofuturabootstrap.Main")
    systemProperty("fml.coreMods.load", "zone.rong.mixinbooter.MixinBooterPlugin,org.taumc.celeritas.core.CeleritasLoadingPlugin")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

tasks.named<ApplySourceAccessTransformersTask>("applySourceAccessTransformers") {
    accessTransformerFiles.from("src/main/resources/META-INF/celeritas_at.cfg")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["FMLAT"] = "celeritas_at.cfg"
        attributes["Lwjgl3ify-Aware"] = "true"
        attributes["FMLCorePlugin"] = "org.taumc.celeritas.core.CeleritasLoadingPlugin"
        attributes["FMLCorePluginContainsFMLMod"] = "true"
        attributes["ForceLoadAsMod"] = "true"
    }
}

tasks.register("packageJar", Copy::class) {
    from(tasks.named<ShadowJar>("shadowRemapJar").get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
    dependsOn(tasks.named("shadowRemapJar"))
}

tasks.processResources.configure {
    inputs.property("version", version)

    filesMatching("mcmod.info") {
        expand(mapOf("version" to inputs.properties["version"]))
    }

    from(rootProject.file("modern/src/main/resources/icon.png"))
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = base.archivesName.get()
            artifact(tasks.named<ShadowJar>("shadowRemapJar").map { it.archiveFile })
            artifact(tasks.named("sourcesJar")) {
                classifier = "sources"
            }
        }
    }
}