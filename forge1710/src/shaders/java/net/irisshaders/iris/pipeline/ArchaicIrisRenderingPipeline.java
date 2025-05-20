package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ArchaicShadowRenderer;
import net.irisshaders.iris.targets.ClearPassCreator;
import net.irisshaders.iris.targets.RenderTargetStateListener;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import org.embeddedt.embeddium.compat.mc.ICamera;
import org.embeddedt.embeddium.compat.mc.ILevelRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

public class ArchaicIrisRenderingPipeline extends CommonIrisRenderingPipeline implements IrisRenderingPipeline, WorldRenderingPipeline, /*ShaderRenderingPipeline,*/ RenderTargetStateListener {
    @Nullable private final ArchaicShadowRenderer shadowRenderer;
    private final ShadowCompositeRenderer shadowCompositeRenderer;


    public ArchaicIrisRenderingPipeline(ProgramSet programSet) {
        super(programSet);

        if (shadowRenderTargets == null && shadowDirectives.isShadowEnabled() == OptionalBoolean.TRUE) {
            shadowRenderTargets = new ShadowRenderTargets(this, shadowMapResolution, shadowDirectives);
        }

        if (shadowRenderTargets != null) {
//            ShaderInstance shader = shaderMap.getShader(ShaderKey.SHADOW_TERRAIN_CUTOUT);
            boolean shadowUsesImages = false;

//            if (shader instanceof ExtendedShader shader2) {
//                shadowUsesImages = shader2.hasActiveImages();
//            }

            this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, false, shadowDirectives);
            this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(shadowRenderTargets, true, shadowDirectives);
            this.shadowCompositeRenderer = null;
//            this.shadowCompositeRenderer = new ShadowCompositeRenderer(this, programSet.getPackDirectives(), programSet.getShadowComposite(), programSet.getShadowCompCompute(), this.shadowRenderTargets, this.shaderStorageBufferHolder, customTextureManager.getNoiseTexture(), updateNotifier,
//                    customTextureManager.getCustomTextureIdMap(TextureStage.SHADOWCOMP), customImages, programSet.getPackDirectives().getExplicitFlips("shadowcomp_pre"), customTextureManager.getIrisCustomTextures(), customUniforms);

            if (programSet.getPackDirectives().getShadowDirectives().isShadowEnabled().orElse(true)) {
                this.shadowRenderer = null;
//                this.shadowRenderer = new ShadowRenderer(this, programSet.getShadow().orElse(null),
//                        programSet.getPackDirectives(), shadowRenderTargets, shadowCompositeRenderer, customUniforms, programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS));
            } else {
                shadowRenderer = null;
            }

            defaultFBShadow = shadowRenderTargets.createFramebufferWritingToMain(new int[] {0});
        } else {
            this.shadowClearPasses = ImmutableList.of();
            this.shadowClearPassesFull = ImmutableList.of();
            this.shadowCompositeRenderer = null;
            this.shadowRenderer = null;
        }

    }

    @Override
    public void beginLevelRendering() {

    }

    @Override
    public void renderShadows(ILevelRenderer worldRenderer, ICamera camera) {

    }

    @Override
    public void addDebugText(List<String> messages) {
        if (this.shadowRenderer != null) {
            messages.add("");
            shadowRenderer.addDebugText(messages);
        } else {
            messages.add("");
            messages.add("[Iris] Shadow Maps: not used by shader pack");
        }
    }

    @Override
    public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
        return OptionalInt.empty();
    }

    @Override
    public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
        return null;
    }

    @Override
    public WorldRenderingPhase getPhase() {
        return null;
    }

    @Override
    public void setPhase(WorldRenderingPhase phase) {

    }

    @Override
    public void setOverridePhase(WorldRenderingPhase phase) {

    }

    @Override
    public RenderTargetStateListener getRenderTargetStateListener() {
        return null;
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

    }

    @Override
    public void beginTranslucents() {

    }

    @Override
    public void finalizeLevelRendering() {

    }

    @Override
    public void finalizeGameRendering() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public SodiumTerrainPipeline getSodiumTerrainPipeline() {
        return null;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        return null;
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
        return null;
    }

    @Override
    public boolean shouldRenderUnderwaterOverlay() {
        return false;
    }

    @Override
    public boolean shouldRenderVignette() {
        return false;
    }

    @Override
    public boolean shouldRenderSun() {
        return false;
    }

    @Override
    public boolean shouldRenderMoon() {
        return false;
    }

    @Override
    public boolean shouldWriteRainAndSnowToDepthBuffer() {
        return false;
    }

    @Override
    public ParticleRenderingSettings getParticleRenderingSettings() {
        return null;
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
        return 0;
    }

    @Override
    public DHCompat getDHCompat() {
        return null;
    }

    @Override
    public void setIsMainBound(boolean bound) {

    }
}
