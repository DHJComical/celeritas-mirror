import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.JSTTransformerTask
import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils
import org.embeddedt.embeddium.gradle.build.conventions.ShadowHelper
import org.embeddedt.embeddium.gradle.mdg.remapper.ReobfuscateCodeAndMixinsTask

plugins {
    id("com.gtnewhorizons.retrofuturagradle")
    id("com.gradleup.shadow")
    id("embeddium-mdg-remapper")
    id("maven-publish")
}

group = "org.embeddedt"
version = rootProject.version

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

base.archivesName = "celeritas-forge-mc7.10"

minecraft {
    mcVersion.set("1.7.10")
    mcpMappingChannel.set("stable")
    mcpMappingVersion.set("12")
}

val modUtils = extensions.getByName("modUtils") as ModUtils

fun deobf(spec: String): String {
    modUtils.deobfuscate(spec)
    return spec
}

repositories {
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    exclusiveContent {
        forRepository {
            ivy {
                name = "CoreTweaks releases"
                setUrl("https://github.com/makamys/CoreTweaks/releases/download/")
                patternLayout {
                    artifact("[revision]/[module]-1.7.10-[revision]+nomixin(-[classifier])(.[ext])")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeGroup("CoreTweaks")
        }
    }
    exclusiveContent {
        forRepository {
            ivy {
                name = "NotFine releases"
                setUrl("https://github.com/jss2a98aj/NotFine/releases/download/")
                patternLayout {
                    artifact("[revision]/[module]-[revision](-[classifier])(.[ext])")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeGroup("NotFine")
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
    mavenCentral()
}

configurations {
    named("shadow") {
        attributes {
            attribute(ModUtils.DEOBFUSCATOR_TRANSFORMED, true)
        }
    }
}

dependencies {
    val lombokVersion = rootProject.findProperty("lombok_version").toString()
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    val jabelVersion = rootProject.findProperty("jabel_version").toString()
    annotationProcessor("com.github.GTNewHorizons:jabel-javac-plugin:${jabelVersion}")
    compileOnly("com.github.GTNewHorizons:jabel-javac-plugin:${jabelVersion}")

    implementation(project(":common", configuration = "downgraded")) {
        isTransitive = false
    }
    shadow(project(":common", configuration = "downgraded")) {
        isTransitive = false
    }

    implementation("org.joml:joml:1.10.5")
    shadow("org.joml:joml:1.10.5")

    compileOnly("org.ow2.asm:asm-commons:9.6")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Only loaded when the game is launched under RetroFuturaBootstrap (lwjgl3ify packs).
    compileOnly("com.gtnewhorizons.retrofuturabootstrap:RetroFuturaBootstrap:1.0.11") {
        exclude(group = "org.apache.logging.log4j")
    }

    implementation("io.github.legacymoddingmc:unimixins:0.1.19:dev")
    implementation("com.github.GTNewHorizons:GTNHLib:0.6.8:dev")

    implementation(deobf("CoreTweaks:CoreTweaks:0.3.3.2"))
    implementation(deobf("NotFine:NotFine:0.2.5"))
    implementation(deobf("maven.modrinth:archaicfix:0.7.4"))
    implementation(deobf("maven.modrinth:etfuturum:2.6.2"))
}

tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = "21"
    options.release = 8

    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named("runServer") {
    enabled = false
}

// Mixin annotations are reobfuscated directly in the bytecode, so no refmap is generated or needed.
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

tasks.named<JSTTransformerTask>("applyJST") {
    accessTransformerFiles.from("src/main/resources/META-INF/celeritas_at.cfg")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["FMLAT"] = "celeritas_at.cfg"
        attributes["FMLCorePlugin"] = "org.taumc.celeritas.core.CeleritasLoadingPlugin"
        attributes["FMLCorePluginContainsFMLMod"] = "true"
        attributes["ForceLoadAsMod"] = "true"
        attributes["Lwjgl3ify-Aware"] = "true"
        attributes["MixinConfigs"] = "mixins.celeritas.json"
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

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
