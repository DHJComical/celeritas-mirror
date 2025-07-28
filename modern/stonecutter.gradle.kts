import bs.ModLoader
import org.embeddedt.embeddium.gradle.stonecutter.ModDependencyCollector

plugins {
    id("dev.kikugie.stonecutter")
}


stonecutter active "1.20.1-forge" /* [SC] DO NOT EDIT */

// constants

stonecutter.parameters {
    val configuredModLoader = ModLoader.fromName(current.project)

    fun versionedProperty(name: String) =
            rootProject.properties[name + "_" + ModLoader.getMinecraftVersion(current.project).replace('.', '_')]?.toString()

    const("fabric", configuredModLoader == ModLoader.FABRIC)
    const("forge", configuredModLoader == ModLoader.FORGE)
    const("neoforge", configuredModLoader == ModLoader.NEOFORGE)
    const("forgelike", configuredModLoader == ModLoader.NEOFORGE || configuredModLoader == ModLoader.FORGE)

    const("shaders", stonecutter.compare(current.version, "1.20") >= 0 && stonecutter.compare(current.version, "1.21.3") < 0)
    const("settings_gui", stonecutter.compare(current.version, "1.21.5") < 0)

    val fabricApiVersion =
        if (configuredModLoader == ModLoader.FABRIC) {
            versionedProperty("fabric_api_version")
        } else if (configuredModLoader == ModLoader.NEOFORGE || (configuredModLoader == ModLoader.FORGE && stonecutter.eval(ModLoader.getMinecraftVersion(current.project), "<1.20.2"))) {
            versionedProperty("ffapi")
        } else {
            null
        }

    if (fabricApiVersion != null) {
        const("ffapi", !"true".equals(versionedProperty("disable_frapi_on")))
    } else {
        const("ffapi", false)
    }

    swap("gui_render_method") {
        if(stonecutter.compare(current.version, "1.20") >= 0)
            "@Override public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {"
        else
            "@Override public void render(PoseStack matrices, int mouseX, int mouseY, float delta) { GuiGraphics drawContext = new GuiGraphics(matrices); "
    }

    swap("guigfx") {
        if(stonecutter.compare(current.version, "1.20") >= 0) "import net.minecraft.client.gui.GuiGraphics;" else "import org.embeddedt.embeddium.impl.gui.compat.GuiGraphics;"
    }

    swap("rng") {
        if(stonecutter.compare(current.version, "1.19") >= 0) "RandomSource" else "Random"
    }

    swap("rng_import") {
        if(stonecutter.compare(current.version, "1.19") >= 0) "import net.minecraft.util.RandomSource;" else "import java.util.Random;"
    }

    val doAnimateTickBiomeLambdaName = "lambda\$doAnimateTick\$" + when {
        eval (current.version, ">=1.21.5") -> 9
        eval (current.version, ">=1.18 <1.21.5-alpha.25.8.a") -> 8
        eval (current.version, ">=1.17") -> 5
        else -> 4
    }
    swaps["doAnimateTickBiomeLambda"] = "@Shadow private void ${doAnimateTickBiomeLambdaName}(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {throw new AssertionError();} private final Consumer<AmbientParticleSettings> embeddium\$particleSettingsConsumer = settings -> ${doAnimateTickBiomeLambdaName}(embeddium\$particlePos, settings);"

    replacements.string {
        direction = eval(current.version, ">=1.21.5")
        phase = "first"
        replace("net.neoforged.neoforge.client.model.data", "net.neoforged.neoforge.model.data")
    }

    replacements.string {
        direction = eval(current.version, ">=1.21.5")
        phase = "first"
        replace("com.mojang.blaze3d.platform.GlStateManager", "com.mojang.blaze3d.opengl.GlStateManager")
    }
}

ModDependencyCollector.defineConsts(stonecutter)
