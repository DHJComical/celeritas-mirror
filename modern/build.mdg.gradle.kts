import bs.ModLoader
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import org.embeddedt.embeddium.gradle.build.extensions.versionedProperty
import org.embeddedt.embeddium.gradle.mdg.remapper.GenerateATFromAWTask
import org.embeddedt.embeddium.gradle.mdg.remapper.GenerateNamedToIntermediaryTSRGTask
import org.embeddedt.embeddium.gradle.mdg.remapper.ReobfuscateCodeAndMixinsTask
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector
import org.gradle.kotlin.dsl.named

plugins {
    // Apply the plugin. You can find the latest version at https://projects.neoforged.net/neoforged/ModDevGradle
    id("net.neoforged.moddev") apply false
    id("net.neoforged.moddev.legacyforge") apply false
    id("embeddium-mdg-remapper")
    id("celeritas.platform-conventions")
    id("celeritas.shader-conventions") apply false
    id("embeddium-fabric-module-finder")
    id("maven-publish")
}

group = "org.embeddedt"
version = rootProject.version

val modLoader = ModLoader.fromProject(project)!!
val minecraftVersion = ModLoader.getMinecraftVersion(project)!!

val defaultArchiveBaseName = "celeritas-${modLoader.friendlyName}-mc${minecraftVersion.replace("^1\\.".toRegex(), "")}"

val generatedATPath = layout.buildDirectory.file("generated/accesstransformer.cfg")

val generateAccessTransformer = tasks.register<GenerateATFromAWTask>("generateAccessTransformer") {
    accessTransformerPath.set(generatedATPath)
    accessWidenerPath.set(rootProject.file("modern/src/main/resources/embeddium.accesswidener"))
}

class MDGConfig(val modDevExtension: ModDevExtension, val productionJarTask: String)

tasks.named<Jar>("jar") {
    archiveBaseName = defaultArchiveBaseName
    archiveClassifier.set("deobf")
}

java {
    withSourcesJar()
}

val neoforgePr = versionedProperty("neoforge_pr")
if (neoforgePr != null) {
    repositories {
        maven {
            name = "NeoForge PR maven"
            url = uri("https://prmaven.neoforged.net/NeoForge/pr$neoforgePr")
            content {
                includeModule("net.neoforged", "neoforge")
                includeModule("net.neoforged", "testframework")
            }
        }
    }
}

val isDecompDisabled = System.getenv("CELERITAS_DISABLE_DECOMP") == "true"

val config: MDGConfig = if (modLoader == ModLoader.NEOFORGE) {
    apply(plugin = "net.neoforged.moddev")
    val neoForge = project.extensions.getByName("neoForge") as NeoForgeExtension
    neoForge.enable {
        version = requireNotNull(versionedProperty("neoforge")) { "NeoForge version must be specified for $minecraftVersion" }
        isDisableRecompilation = isDecompDisabled
    }
    neoForge.unitTest.enable()
    MDGConfig(neoForge, "jar")
} else {
    apply(plugin = "net.neoforged.moddev.legacyforge")
    val legacyForge = project.extensions.getByName("legacyForge") as LegacyForgeExtension
    legacyForge.enable {
        forgeVersion = versionedProperty("forge")
        isDisableRecompilation = isDecompDisabled
    }
    val obfuscation = project.extensions.getByType<ObfuscationExtension>()
    val generateNamedToIntermediary = tasks.register<GenerateNamedToIntermediaryTSRGTask>("generateNamedToIntermediaryTSRG") {
        tsrgPath = layout.buildDirectory.file("generated/namedToIntermediaryCeleritas.tsrg")
        forgeVersion = legacyForge.version
    }
    generateAccessTransformer.configure {
        tsrgMappings = generateNamedToIntermediary.flatMap { it -> it.tsrgPath }
        dependsOn(generateNamedToIntermediary)
    }
    tasks.named("reobfJar").configure {
        enabled = false
    }
    tasks.register<ReobfuscateCodeAndMixinsTask>("celeritasRemapJar") {
        tsrgMappings = obfuscation.namedToSrgMappings
        deobfMinecraftJar = tasks.named<CreateMinecraftArtifacts>("createMinecraftArtifacts").flatMap { it -> it.compiledArtifact }
        classpath = sourceSets.main.get().compileClasspath
        archiveBaseName.set(defaultArchiveBaseName)
        archiveClassifier.set("reobf")
        input = tasks.named<Jar>("jar").flatMap { it -> it.archiveFile }
    }
    MDGConfig(legacyForge, "celeritasRemapJar")
}

val modDevExtension = config.modDevExtension

val parchmentVersion = versionedProperty("parchment_version")

if (parchmentVersion != null) {
    val parchmentData = parchmentVersion.split(":")
    modDevExtension.parchment {
        minecraftVersion = parchmentData[0]
        mappingsVersion = parchmentData[1]
    }
}

val mainMod = modDevExtension.mods.create("embeddium") {
    sourceSet(sourceSets.main.get())
    val commonProj = project(":common")
    listOf("main", "lwjgl3", "lwjglCommon").forEach {
        sourceSet(commonProj.sourceSets.getByName(it))
    }
}

if (modLoader == ModLoader.NEOFORGE) {
    (modDevExtension as NeoForgeExtension).unitTest.testedMod = mainMod

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
} else {
    // Disable unit tests
    sourceSets.named("test") {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    tasks.named("test") {
        enabled = false
    }
}

tasks.named("createMinecraftArtifacts") {
    dependsOn(generateAccessTransformer)
}

modDevExtension.accessTransformers.from(generatedATPath)

val modMixinConfigs = project.extra.get("celeritasMixinConfigs") as MutableList<String>

if (stonecutter.constants.getOrDefault("shaders", false)) {
    apply(plugin = "celeritas.shader-conventions")
}

modDevExtension.runs {
    create("client") {
        client()
        ideName.set("")
        for (config in modMixinConfigs) {
            programArgument("--mixin.config")
            programArgument(config)
        }
        if (stonecutter.eval(minecraftVersion, ">=1.20.5")) {
            // Use generational ZGC as we are on Java 21+
            jvmArgument("-XX:+UseZGC")
            jvmArgument("-XX:+ZGenerational")
        }
        jvmArgument("-XX:+UnlockDiagnosticVMOptions")
        jvmArgument("-XX:+DebugNonSafepoints")
    }
}

dependencies {
    shadow(project(":common")) {
        isTransitive = false
    }
    val ffapiVersion = versionedProperty("ffapi")
    if (ffapiVersion != null) {
        for (module in rootProject.property("fabric_api_modules").toString().split(",")) {
            compileOnly(fabricApiModuleFinder.module(modLoader, module,ffapiVersion))
        }
    }

    compileOnly("net.fabricmc:fabric-loader:${rootProject.property("fabricloader")}")

    if (modLoader != ModLoader.NEOFORGE) {
        val mixinExtrasVersion = rootProject.property("mixinextras").toString()
        compileOnly("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")

        implementation("io.github.llamalad7:mixinextras-${modLoader.friendlyName}:$mixinExtrasVersion")
        "jarJar"("io.github.llamalad7:mixinextras-${modLoader.friendlyName}:$mixinExtrasVersion")
    }

    if (stonecutter.eval(minecraftVersion, "<1.19.3")) {
        val jomlDep = "org.joml:joml:${rootProject.property("joml_version")}"
        implementation(jomlDep)
        "jarJar"(jomlDep)
        "additionalRuntimeClasspath"(jomlDep)
    }

    ModDependencyCollector.obtainDeps(project) { cfg, dep ->
        dependencies.add(cfg, dep)
    }

    testImplementation(platform("org.junit:junit-bom:${rootProject.property("junit_version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes.put("MixinConfigs", modMixinConfigs.joinToString(","))
        attributes.put("Fabric-Loom-Mixin-Remap-Type", "static")
        attributes.put("Fabric-Mapping-Namespace", "intermediary")
    }
}

tasks.named<ProcessResources>("processResources") {
    from(generateAccessTransformer.flatMap { it -> it.accessTransformerPath }) {
        into("META-INF/")
    }
}

val shadowJar = tasks.register<ShadowJar>("shadowRemapJar") {
    archiveBaseName = defaultArchiveBaseName
    archiveClassifier = ""
    configurations = listOf(project.configurations.shadow.get())
    from(zipTree(tasks.named<Jar>(config.productionJarTask).get().archiveFile))
    manifest.inheritFrom(tasks.named<Jar>("jar").get().manifest)
    if (modLoader == ModLoader.FORGE && stonecutter.eval(minecraftVersion, "<1.17")) {
        relocate("com.llamalad7.mixinextras", "org.embeddedt.embeddium.impl.shadow.mixinextras")
        relocate("org.joml", "org.embeddedt.embeddium.impl.shadow.joml")
    }
    mergeServiceFiles()

    from("COPYING", "COPYING.LESSER", "README.md")
}

val packageJar = tasks.register("packageJar", Copy::class) {
    from(shadowJar.get().archiveFile)
    into("${rootProject.layout.buildDirectory.get()}/libs/${project.version}")
}

tasks.named("assemble") {
    dependsOn(packageJar)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = defaultArchiveBaseName
            artifact(shadowJar.map { it.archiveFile })
            artifact(tasks.named("sourcesJar")) {
                classifier = "sources"
            }
        }
    }
}