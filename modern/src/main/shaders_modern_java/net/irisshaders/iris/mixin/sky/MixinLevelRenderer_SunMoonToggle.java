package net.irisshaders.iris.mixin.sky;

import com.mojang.blaze3d.vertex.*;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Allows pipelines to disable the sun, moon, or both.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_SunMoonToggle {

    //? if >=1.21 {
    /*private static final String CELERITAS$DRAW_WITH_SHADER = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V";

    @Unique
	private void iris$emptyBuilder(Object data) {
        ((MeshData)data).close();
	}

    @Unique
    private void iris$drawBuilder(Object data) {
        BufferUploader.drawWithShader((MeshData)data);
    }
    *///?} else {
    private static final String CELERITAS$DRAW_WITH_SHADER = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V";
    @Unique
    private void iris$emptyBuilder(Object data) {
        ((BufferBuilder.RenderedBuffer)data).release();
    }

    @Unique
    private void iris$drawBuilder(Object data) {
        BufferUploader.drawWithShader((BufferBuilder.RenderedBuffer) data);
    }
    //?}

	@Redirect(method = "renderSky",
		at = @At(value = "INVOKE", target = CELERITAS$DRAW_WITH_SHADER),
		slice = @Slice(
			from = @At(value = "FIELD", target = "net/minecraft/client/renderer/LevelRenderer.SUN_LOCATION : Lnet/minecraft/resources/ResourceLocation;"),
			to = @At(value = "FIELD", target = "net/minecraft/client/renderer/LevelRenderer.MOON_LOCATION : Lnet/minecraft/resources/ResourceLocation;")),
		allow = 1)
	private void iris$beforeDrawSun(@Coerce Object meshData) {
		if (!Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderSun).orElse(true)) {
            iris$emptyBuilder(meshData);
		} else {
            iris$drawBuilder(meshData);
        }
	}

	@Redirect(method = "renderSky",
		at = @At(value = "INVOKE", target = CELERITAS$DRAW_WITH_SHADER),
		slice = @Slice(
			from = @At(value = "FIELD", target = "net/minecraft/client/renderer/LevelRenderer.MOON_LOCATION : Lnet/minecraft/resources/ResourceLocation;"),
			to = @At(value = "INVOKE", target = "net/minecraft/client/multiplayer/ClientLevel.getStarBrightness (F)F")),
		allow = 1)
	private void iris$beforeDrawMoon(@Coerce Object meshData) {
		if (!Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderMoon).orElse(true)) {
			iris$emptyBuilder(meshData);
		} else {
            iris$drawBuilder(meshData);
        }
	}
}
