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
            rootProject.findProperty(name + "_" + ModLoader.getMinecraftVersion(current.project).replace('.', '_'))?.toString()

    constants["fabric"] = configuredModLoader == ModLoader.FABRIC
    constants["forge"] = configuredModLoader == ModLoader.FORGE
    constants["neoforge"] = configuredModLoader == ModLoader.NEOFORGE
    constants["forgelike"] = configuredModLoader == ModLoader.NEOFORGE || configuredModLoader == ModLoader.FORGE

    constants["shaders"] = stonecutter.compare(current.version, "1.20") >= 0 && stonecutter.compare(current.version, "1.21.3") < 0
    constants["settings_gui"] = stonecutter.compare(current.version, "1.21.5") < 0

    val fabricApiVersion =
        if (configuredModLoader == ModLoader.FABRIC) {
            versionedProperty("fabric_api_version")
        } else if (configuredModLoader == ModLoader.NEOFORGE || (configuredModLoader == ModLoader.FORGE && stonecutter.eval(ModLoader.getMinecraftVersion(current.project), "<1.20.2"))) {
            versionedProperty("ffapi")
        } else {
            null
        }

    if (fabricApiVersion != null) {
        constants["ffapi"] = !"true".equals(versionedProperty("disable_frapi_on"))
    } else {
        constants["ffapi"] = false
    }

    swaps["gui_render_method"] = {
        if(stonecutter.compare(current.version, "1.20") >= 0)
            "@Override public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {"
        else
            "@Override public void render(PoseStack matrices, int mouseX, int mouseY, float delta) { GuiGraphics drawContext = new GuiGraphics(matrices); "
    }

    swaps["guigfx"] = {
        if(stonecutter.compare(current.version, "1.20") >= 0) "import net.minecraft.client.gui.GuiGraphics;" else "import org.embeddedt.embeddium.impl.gui.compat.GuiGraphics;"
    }

    swaps["rng"] = {
        if(stonecutter.compare(current.version, "1.19") >= 0) "RandomSource" else "Random"
    }

    swaps["rng_import"] = {
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
        replace("net.neoforged.neoforge.client.model.data", "net.neoforged.neoforge.model.data")
    }

    replacements.string {
        direction = eval(current.version, ">=1.21.5")
        replace("com.mojang.blaze3d.platform.GlStateManager", "com.mojang.blaze3d.opengl.GlStateManager")
    }

    replacements.string {
        direction = eval(current.version, ">=1.21.11")
        replace("net.minecraft.Util", "net.minecraft.util.Util")
    }

    replacements.string {
        direction = eval(current.version, ">=26.1")
        replace("net.minecraft.world.level.BlockAndTintGetter", "net.minecraft.client.renderer.block.BlockAndTintGetter")
    }

    replacements.string {
        direction = eval(current.version, ">=26.1")
        replace("net.minecraft.client.renderer.block.model.BakedQuad", "net.minecraft.client.resources.model.geometry.BakedQuad")
    }

    replacements.string {
        direction = eval(current.version, ">=1.21.11")
        replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
    }

    replacements.regex(eval(current.version, ">=1.21.11")) {
        replace("\\bResourceLocation\\b", "Identifier")
        reverse("\\bIdentifier\\b", "ResourceLocation")
    }
}

ModDependencyCollector.defineConsts(stonecutter)
