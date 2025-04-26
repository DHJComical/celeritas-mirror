package net.irisshaders.iris.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.RenderTargetStateListener;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import org.embeddedt.embeddium.compat.mc.ICamera;
import org.embeddedt.embeddium.compat.mc.ILevelRenderer;

import java.util.List;
import java.util.OptionalInt;

public interface WorldRenderingPipeline {
	void beginLevelRendering();

	void renderShadows(ILevelRenderer worldRenderer, ICamera camera);

	void addDebugText(List<String> messages);

	OptionalInt getForcedShadowRenderDistanceChunksForDisplay();

	Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap();

	WorldRenderingPhase getPhase();

	void setPhase(WorldRenderingPhase phase);

	void setOverridePhase(WorldRenderingPhase phase);

	RenderTargetStateListener getRenderTargetStateListener();

	int getCurrentNormalTexture();

	int getCurrentSpecularTexture();

	void onSetShaderTexture(int id);

	void beginHand();

	void beginTranslucents();

	void finalizeLevelRendering();

	void finalizeGameRendering();

	void destroy();

	SodiumTerrainPipeline getSodiumTerrainPipeline();

	FrameUpdateNotifier getFrameUpdateNotifier();

	boolean shouldDisableVanillaEntityShadows();

	boolean shouldDisableDirectionalShading();

	boolean shouldDisableFrustumCulling();

	boolean shouldDisableOcclusionCulling();

	CloudSetting getCloudSetting();

	boolean shouldRenderUnderwaterOverlay();

	boolean shouldRenderVignette();

	boolean shouldRenderSun();

	boolean shouldRenderMoon();

	boolean shouldWriteRainAndSnowToDepthBuffer();

	ParticleRenderingSettings getParticleRenderingSettings();

	boolean allowConcurrentCompute();

	boolean hasFeature(FeatureFlags flags);

	float getSunPathRotation();

	DHCompat getDHCompat();
}
