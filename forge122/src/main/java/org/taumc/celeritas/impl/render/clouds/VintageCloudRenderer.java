package org.taumc.celeritas.impl.render.clouds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.clouds.CloudFrame;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderState;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderer;
import org.embeddedt.embeddium.impl.render.clouds.CloudTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.impl.render.terrain.GLStateManagerGlStateService;
import org.taumc.celeritas.impl.render.terrain.fog.GLStateManagerFogService;
import org.taumc.celeritas.mixin.core.terrain.ActiveRenderInfoAccessor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class VintageCloudRenderer {
    private static final ResourceLocation CLOUDS_TEXTURE_ID = new ResourceLocation("textures/environment/clouds.png");

    // Vanilla's default cloud range since 1.21.6
    private static final int CLOUD_RANGE_CHUNKS = 128;

    // Vanilla's fast clouds span 2048 blocks, making 8 block cells, and aren't shifted along Z like fancy clouds
    private static final float FAST_TEXTURE_SIZE = 2048.0f;

    // Where exponential fog has hidden everything, as exp(-x) < 1/255
    private static final float EXP_FOG_OPAQUE_DEPTH = (float) Math.log(255.0);

    private static final CloudRenderState RENDER_STATE = new CloudRenderState() {
        private boolean alphaTestWasEnabled;

        @Override
        public void setup(boolean fancy) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.colorMask(true, true, true, true);

            // The fixed function alpha test still applies to our program, and would cut off the fade at the edge
            this.alphaTestWasEnabled = GlStateManager.alphaState.alphaTest.currentState;
            GlStateManager.disableAlpha();

            if (fancy) {
                GlStateManager.enableCull();
            } else {
                GlStateManager.disableCull();
            }
        }

        @Override
        public void restore() {
            GlStateManager.disableBlend();
            GlStateManager.enableCull();

            if (this.alphaTestWasEnabled) {
                GlStateManager.enableAlpha();
            }
        }
    };

    private final Matrix4f modelViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();

    private CloudTexture texture;
    private @Nullable CloudRenderer renderer;

    public VintageCloudRenderer(IResourceManager resourceManager) {
        this.reloadTextures(resourceManager);
    }

    /**
     * {@return whether our clouds replace vanilla's} Without GL 3.2, vanilla's clouds are used even when the option is on.
     */
    public static boolean isEnabled() {
        return CeleritasVintage.options().performance.useFasterClouds && CloudRenderer.isSupported();
    }

    public static float getCloudRange() {
        return CLOUD_RANGE_CHUNKS * 16.0f;
    }

    /**
     * @param pass the anaglyph pass, as vanilla passes it ({@code 2} when anaglyph is disabled)
     */
    public void render(WorldClient world, int ticks, float partialTicks, int pass, double cameraX, double cameraY, double cameraZ) {
        float range = getCloudRange();
        float fogEnd = getFogEnd(range);

        if (fogEnd <= 0.0f) {
            return;
        }

        Vec3d color = world.getCloudColour(partialTicks);
        float red = (float) color.x;
        float green = (float) color.y;
        float blue = (float) color.z;

        // Vanilla's anaglyph color mixing
        if (pass != 2) {
            float anaglyphRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            float anaglyphGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            float anaglyphBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = anaglyphRed;
            green = anaglyphGreen;
            blue = anaglyphBlue;
        }

        boolean fancy = Minecraft.getMinecraft().gameSettings.shouldRenderClouds() == 2;

        // The terrain matrices vanilla captured for this pass. The far plane already covers the cloud range
        // (see CloudsEntityRendererMixin).
        this.modelViewMatrix.set(ActiveRenderInfoAccessor.getModelViewMatrix());
        this.projectionMatrix.set(ActiveRenderInfoAccessor.getProjectionMatrix());

        CloudFrame frame = new CloudFrame(this.modelViewMatrix, this.projectionMatrix, cameraX, cameraY, cameraZ,
                world.provider.getCloudHeight(), ticks + partialTicks, red, green, blue, fancy, range, fogEnd,
                fancy ? CloudFrame.DEFAULT_TEXTURE_SIZE : FAST_TEXTURE_SIZE,
                fancy ? CloudFrame.DEFAULT_TEXTURE_OFFSET_Z : 0.0f);

        RenderDevice.enterManagedCode();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            if (this.renderer == null) {
                this.renderer = new CloudRenderer(commandList, GLStateManagerGlStateService.INSTANCE, this.texture);
            }

            if (this.renderer.prepare(commandList, frame)) {
                this.renderer.render(commandList, RENDER_STATE, frame);
            }
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * Like vanilla since 1.21.6, clouds fade out over their own range, unless environmental fog (water, blindness, etc.)
     * is closer.
     */
    private static float getFogEnd(float cloudRange) {
        GLStateManagerFogService fog = GLStateManagerFogService.INSTANCE;
        float fogEnd;

        switch (fog.getFogMode()) {
            case NONE:
                return cloudRange;
            case EXP:
                fogEnd = EXP_FOG_OPAQUE_DEPTH / fog.getFogDensity();
                break;
            case EXP2:
                fogEnd = (float) Math.sqrt(EXP_FOG_OPAQUE_DEPTH) / fog.getFogDensity();
                break;
            default:
                fogEnd = fog.getFogEnd();
                break;
        }

        if (fogEnd < fog.getSkyFogEdgeEnd()) {
            return Math.min(cloudRange, fogEnd);
        }

        return cloudRange;
    }

    public void reloadTextures(IResourceManager resourceManager) {
        this.destroy();
        this.texture = loadTexture(resourceManager);
    }

    public void destroy() {
        if (this.renderer == null) {
            return;
        }

        RenderDevice.enterManagedCode();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.renderer.delete(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }

        this.renderer = null;
    }

    private static CloudTexture loadTexture(IResourceManager resourceManager) {
        try (InputStream inputStream = resourceManager.getResource(CLOUDS_TEXTURE_ID).getInputStream()) {
            BufferedImage image = TextureUtil.readBufferedImage(inputStream);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = ColorARGB.toABGR(pixels[i]);
            }

            return new CloudTexture(width, height, pixels);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }
    }
}
