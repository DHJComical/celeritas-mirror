package net.irisshaders.iris.pipeline;

import java.util.List;
import java.util.OptionalInt;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.RenderTargetStateListener;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;

import org.embeddedt.embeddium.compat.mc.ICamera;
import org.embeddedt.embeddium.compat.mc.ILevelRenderer;

public abstract class VanillaRenderingPipeline implements WorldRenderingPipeline {
	public VanillaRenderingPipeline() {
		WorldRenderingSettings.INSTANCE.setDisableDirectionalShading(shouldDisableDirectionalShading());
		WorldRenderingSettings.INSTANCE.setUseSeparateAo(false);
		WorldRenderingSettings.INSTANCE.setSeparateEntityDraws(false);
		WorldRenderingSettings.INSTANCE.setAmbientOcclusionLevel(1.0f);
		WorldRenderingSettings.INSTANCE.setUseExtendedVertexFormat(false);
		WorldRenderingSettings.INSTANCE.setVoxelizeLightBlocks(false);

	}

	@Override
	public void beginLevelRendering() {
		// Use the default Minecraft framebuffer and ensure that no programs are in use
        MINECRAFT_SHIM.bindFramebuffer();
		GL_STATE_MANAGER.glUseProgram(0);
	}

	@Override
	public void renderShadows(ILevelRenderer worldRenderer, ICamera camera) {
		// stub: nothing to do here
	}

	@Override
	public void addDebugText(List<String> messages) {
		// stub: nothing to do here
	}

	@Override
	public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
		return OptionalInt.empty();
	}

	@Override
	public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
		return Object2ObjectMaps.emptyMap();
	}

	@Override
	public WorldRenderingPhase getPhase() {
		return WorldRenderingPhase.NONE;
	}

	@Override
	public void setPhase(WorldRenderingPhase phase) {

	}

	@Override
	public void setOverridePhase(WorldRenderingPhase phase) {

	}

	@Override
	public RenderTargetStateListener getRenderTargetStateListener() {
		return RenderTargetStateListener.NOP;
	}

	@Override
	public int getCurrentNormalTexture() {
		return 0;
	}

	@Override
	public int getCurrentSpecularTexture() {
		return 0;
	}

	@Override
	public void onSetShaderTexture(int id) {

	}

	@Override
	public void beginHand() {
		// stub: nothing to do here
	}

	@Override
	public void beginTranslucents() {
		// stub: nothing to do here
	}

	@Override
	public void finalizeLevelRendering() {
		// stub: nothing to do here
	}

	@Override
	public void finalizeGameRendering() {
		// stub: nothing to do here
	}

	@Override
	public void destroy() {
		// stub: nothing to do here
	}

	@Override
	public SodiumTerrainPipeline getSodiumTerrainPipeline() {
		// no shaders to override
		return null;
	}

	@Override
	public FrameUpdateNotifier getFrameUpdateNotifier() {
		// return a dummy notifier
		return new FrameUpdateNotifier();
	}

	@Override
	public boolean shouldDisableVanillaEntityShadows() {
		return false;
	}

	@Override
	public boolean shouldDisableDirectionalShading() {
		return false;
	}

	@Override
	public boolean shouldDisableFrustumCulling() {
		return false;
	}

	@Override
	public boolean shouldDisableOcclusionCulling() {
		return false;
	}

	@Override
	public CloudSetting getCloudSetting() {
		return CloudSetting.DEFAULT;
	}

	@Override
	public boolean shouldRenderUnderwaterOverlay() {
		return true;
	}

	@Override
	public boolean shouldRenderVignette() {
		return true;
	}

	@Override
	public boolean shouldRenderSun() {
		return true;
	}

	@Override
	public boolean shouldRenderMoon() {
		return true;
	}

	@Override
	public boolean shouldWriteRainAndSnowToDepthBuffer() {
		return false;
	}

	@Override
	public boolean allowConcurrentCompute() {
		return false;
	}

	@Override
	public boolean hasFeature(FeatureFlags flags) {
		return false;
	}

	@Override
	public float getSunPathRotation() {
		// No sun tilt
		return 0;
	}

	@Override
	public DHCompat getDHCompat() {
		return null;
	}
}
