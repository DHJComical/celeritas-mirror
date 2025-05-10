import xyz.wagyourtail.unimined.api.minecraft.EnvType

plugins {
    id("xyz.wagyourtail.unimined")
}

repositories {
    maven("https://maven.glass-launcher.net/releases/")
    maven("https://api.modrinth.com/maven")
}

group = "org.embeddedt"
version = rootProject.version

evaluationDependsOn(":common")

unimined.minecraft {
    combineWith(project(":common"), project(":common").sourceSets.getByName("main"))

    version = "b1.7.3"
    side = EnvType.CLIENT

    mappings {
        babricIntermediary()
        biny("2f404bc")
    }

    babric {
        loader("0.15.6-babric.2")
    }

    minecraftRemapper.config {
        ignoreConflicts(true)
    }

    runs.config("client") {
        javaVersion = JavaVersion.VERSION_21
    }
}

configurations.getByName("minecraftLibraries").dependencies.removeIf {
    it.group.equals("org.lwjgl.lwjgl")
}

dependencies {
    "minecraftLibraries"("org.mcphackers:legacy-lwjgl3:1.0")
    val lwjglVersion = "3.3.3"
    "minecraftLibraries"("org.lwjgl:lwjgl:${lwjglVersion}")
    "minecraftLibraries"("org.lwjgl:lwjgl-opengl:${lwjglVersion}")
    "minecraftLibraries"("org.lwjgl:lwjgl-openal:${lwjglVersion}")
    "minecraftLibraries"("org.lwjgl:lwjgl-glfw:${lwjglVersion}")
    "minecraftLibraries"("org.lwjgl:lwjgl-stb:${lwjglVersion}")
    "minecraftLibraries"("org.lwjgl:lwjgl:${lwjglVersion}:natives-linux")
    "minecraftLibraries"("org.lwjgl:lwjgl-opengl:${lwjglVersion}:natives-linux")
    "minecraftLibraries"("org.lwjgl:lwjgl-openal:${lwjglVersion}:natives-linux")
    "minecraftLibraries"("org.lwjgl:lwjgl-glfw:${lwjglVersion}:natives-linux")
    "minecraftLibraries"("org.lwjgl:lwjgl-stb:${lwjglVersion}:natives-linux")

    implementation("org.joml:joml:1.10.5")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("com.google.guava:guava:31.1-jre")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}