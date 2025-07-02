package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.CommonShadowRenderer;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ArchaicShadowRenderer;
import net.irisshaders.iris.targets.ClearPassCreator;
import net.irisshaders.iris.targets.RenderTargetStateListener;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.embeddedt.embeddium.compat.mc.MCCamera;
import org.embeddedt.embeddium.compat.mc.MCLevelRenderer;
import org.embeddedt.embeddium.compat.mc.MCShaderInstance;
import org.embeddedt.embeddium.compat.mc.MCVertexFormat;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.interfaces.IRenderTargetExt;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ArchaicIrisRenderingPipeline extends CommonIrisRenderingPipeline implements IrisRenderingPipeline, WorldRenderingPipeline, ShaderRenderingPipeline, RenderTargetStateListener {
    @Nullable private final ArchaicShadowRenderer shadowRenderer;
    private final ShadowCompositeRenderer shadowCompositeRenderer;


    @Override
    protected void updateMCFBInfo() {
        final Framebuffer main = Minecraft.getMinecraft().getFramebuffer();

        this.mainFBHeight = main.framebufferHeight;
        this.mainFBWidth = main.framebufferWidth;
        this.mainFBDepthTextureId = ((IRenderTargetExt)main).getIris$depthTextureId();
        this.mainFBDepthBufferVersion = ((IRenderTargetExt)main).iris$getDepthBufferVersion();

    }

    public ArchaicIrisRenderingPipeline(ProgramSet programSet) {
        super(programSet);

        if (shadowRenderTargets == null && shadowDirectives.isShadowEnabled() == OptionalBoolean.TRUE) {
            shadowRenderTargets = new ShadowRenderTargets(this, shadowMapResolution, shadowDirectives);
        }

        if (hasShadowRenderTargets()) {
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
    protected boolean checkShadowUsesImages() {
        return false;
    }

    @Override
    protected void renderHorizon() {
        // TODO
        throw new UnsupportedOperationException("renderHorizon");
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
    protected CompletableFuture<MCShaderInstance> createShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId,
            AlphaTest fallbackAlpha, MCVertexFormat vertexFormat, FogMode fogMode, boolean isIntensity, boolean isFullbright, boolean isGlint, boolean isText)
            throws IOException {
        return null;
    }

    @Override
    protected MCShaderInstance createFallbackShader(String name, ShaderKey key) throws IOException {
        return null;
    }

    @Override
    protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Optional<ProgramSource> source, ShaderKey key, Executor syncExecutor)
            throws IOException {
        return null;
    }

    @Override
    protected MCShaderInstance createFallbackShadowShader(String name, ShaderKey key) throws IOException {
        return null;
    }

    @Override
    protected CompletableFuture<MCShaderInstance> createShadowShader(String name, Executor syncExecutor, ProgramSource source, ProgramId programId,
            AlphaTest fallbackAlpha, MCVertexFormat vertexFormat, boolean isIntensity, boolean isFullbright, boolean isText) throws IOException {
        return null;
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
    protected void destroyHorizonRenderer() {
        throw new UnsupportedOperationException("destroyHorizonRenderer");
    }

    @Override
    protected @Nullable CommonShadowRenderer createShadowRenderer(CommonIrisRenderingPipeline commonIrisRenderingPipeline, ProgramSource programSource,
            PackDirectives packDirectives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer shadowCompositeRenderer,
            CustomUniforms customUniforms, boolean b) {
        return null;
    }

    @Override
    protected ShaderKey[] getShaderKeyValues() {
        return new ShaderKey[0];
    }

    @Override
    public SodiumTerrainPipeline getSodiumTerrainPipeline() {
        return null;
    }

    @Override
    public ShaderMap getShaderMap() {
        return null;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        return null;
    }

    @Override
    public boolean shouldOverrideShaders() {
        return false;
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
