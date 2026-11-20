package org.embeddedt.embeddium.impl.render.immediate;

//? if >=1.17 {
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.render.clouds.ModifyCloudRenderingEvent;
import org.embeddedt.embeddium.impl.gl.compat.FogHelper;
import org.embeddedt.embeddium.impl.gl.compat.ModernGlStateService;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.embeddedt.embeddium.impl.render.clouds.CloudFrame;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderState;
import org.embeddedt.embeddium.impl.render.clouds.CloudRenderer;
import org.embeddedt.embeddium.impl.render.clouds.CloudTexture;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.io.IOException;
import java.io.InputStream;

public class ModernCloudRenderer {
    private static final ResourceLocation CLOUDS_TEXTURE_ID = ResourceLocationUtil.make("textures/environment/clouds.png");

    // Vanilla's default cloud range since 1.21.6
    private static final int CLOUD_RANGE_CHUNKS = 128;

    private static final CloudRenderState RENDER_STATE = new CloudRenderState() {
        @Override
        public void setup(boolean fancy) {
            //? if <1.21 {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL30C.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            //?} else {
            //RenderType.clouds().setupRenderState();
            //?}

            if (fancy) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
        }

        @Override
        public void restore() {
            //? if <1.21 {
            RenderSystem.disableBlend();
            //?} else
            //RenderType.clouds().clearRenderState();
            RenderSystem.enableCull();
        }
    };

    private CloudTexture texture;
    private @Nullable CloudRenderer renderer;

    public ModernCloudRenderer(ResourceProvider factory) {
        this.reloadTextures(factory);
    }

    private static int fireModifyCloudRenderDistanceEvent(int distance) {
        var event = new ModifyCloudRenderingEvent(distance);
        ModifyCloudRenderingEvent.BUS.post(event);
        return event.getCloudRenderDistance();
    }

    public static float getCloudRange() {
        return fireModifyCloudRenderDistanceEvent(CLOUD_RANGE_CHUNKS) * 16.0f;
    }

    public void render(@Nullable ClientLevel world, LocalPlayer player, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (world == null) {
            return;
        }

        float cloudHeight = world.effects().getCloudHeight();

        // Vanilla uses NaN height as a way to disable cloud rendering
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        float range = getCloudRange();
        float fogEnd = getFogEnd(range);

        if (fogEnd <= 0.0f) {
            return;
        }

        Vec3 color = world.getCloudColor(tickDelta);
        boolean fancy = Minecraft.getInstance().options.getCloudsType() == CloudStatus.FANCY;

        CloudFrame frame = new CloudFrame(modelViewMatrix, projectionMatrix, cameraX, cameraY, cameraZ, cloudHeight,
                ticks + tickDelta, (float) color.x, (float) color.y, (float) color.z, fancy, range, fogEnd);

        RenderDevice.enterManagedCode();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            if (this.renderer == null) {
                this.renderer = new CloudRenderer(commandList, ModernGlStateService.INSTANCE, this.texture);
            }

            if (this.renderer.prepare(commandList, frame)) {
                this.draw(commandList, this.renderer, frame);
            }
        } finally {
            exitManagedCode();
        }
    }

    private void draw(CommandList commandList, CloudRenderer renderer, CloudFrame frame) {
        renderer.render(commandList, RENDER_STATE, frame);
    }

    /**
     * Like vanilla since 1.21.6, clouds fade out over their own range, unless environmental fog (water, blindness, etc.)
     * is closer.
     */
    private static float getFogEnd(float cloudRange) {
        FogHelper fog = FogHelper.INSTANCE;

        if (fog.getFogMode() != ChunkFogMode.NONE && fog.getFogEnd() < fog.getSkyFogEdgeEnd()) {
            return Math.min(cloudRange, fog.getFogEnd());
        }

        return cloudRange;
    }

    public void reloadTextures(ResourceProvider factory) {
        this.destroy();
        this.texture = loadTexture();
    }

    public void destroy() {
        if (this.renderer == null) {
            return;
        }

        RenderDevice.enterManagedCode();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.renderer.delete(commandList);
        } finally {
            exitManagedCode();
        }

        this.renderer = null;
    }

    private static void exitManagedCode() {
        RenderDevice.exitManagedCode();
        com.mojang.blaze3d.vertex.BufferUploader.reset();
    }

    private static CloudTexture loadTexture() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        try {
            //? if >=1.19 {
            Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                    .orElseThrow();
            try (InputStream inputStream = resource.open()){
            //?} else {
            /*Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID);
            try (InputStream inputStream = resource.getInputStream()){
            *///?}
                try (NativeImage image = NativeImage.read(inputStream)) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    int[] pixels = new int[width * height];

                    for (int z = 0; z < height; z++) {
                        for (int x = 0; x < width; x++) {
                            pixels[x + z * width] = image.getPixelRGBA(x, z);
                        }
                    }

                    return new CloudTexture(width, height, pixels);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }
    }
}
//?}
