package org.embeddedt.embeddium.impl;

import cpw.mods.fml.common.Loader;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import org.embeddedt.embeddium.compat.mc.IResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService;
import org.embeddedt.embeddium.compat.mc.PlatformUtilService;
import org.joml.*;
import org.joml.Math;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.nio.file.Path;

public class MinecraftArchaicVersionShimImpl implements MinecraftVersionShimService, PlatformUtilService {
    private static final Minecraft client = Minecraft.getMinecraft();
    private static final boolean isDevelopmentEnvironment = (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");


    @Override
    public boolean isOnOSX() {
        return Minecraft.isRunningOnMac;
    }

    @Override
    public int getMipmapLevels() {
        return 0;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    @Override
    public String translate(String key, Object... args) {
        return I18n.format(key, args);
    }

    private WorldClient getWorld() {
        return Minecraft.getMinecraft().theWorld;
    }

    @Override
    public boolean isLevelLoaded() {
        return getWorld() != null;
    }

    @Override
    public int getRenderDistanceInBlocks() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16;
    }

    @Override
    public int getEffectiveRenderDistance() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
    }

    @Override
    public Vector3d getUnshiftedCameraPosition() {
        // TODO: Needs to be captured in 1.7.10 via mixins
        return null;
    }

    @Override
    public float getSkyAngle() {
        return getWorld().getCelestialAngle(CapturedRenderingState.INSTANCE.getTickDelta());
    }

    @Override
    public void applyRotationYP(Matrix4f preCelestial, float degrees) {
        preCelestial.rotateY(degrees * IrisCommon.DEGREES_TO_RADIANS);
    }

    @Override
    public void applyRotationXP(Matrix4f preCelestial, float degrees) {
        preCelestial.rotateX(degrees * IrisCommon.DEGREES_TO_RADIANS);
    }

    @Override
    public void applyRotationZP(Matrix4f preCelestial, float degrees) {
        preCelestial.rotateZ(degrees * IrisCommon.DEGREES_TO_RADIANS);
    }

    @Override
    public int getMoonPhase() {
        return getWorld().getMoonPhase();
    }

    @Override
    public long getDayTime() {
        return getWorld().getWorldTime() % 24000L;
    }

    @Override
    public long getDimensionTime(long orElse) {
        return getDayTime();
    }

    @Override
    public boolean isCurrentDimensionNether() {
        final WorldClient level = Minecraft.getMinecraft().theWorld;
        return level != null && (level.provider.isHellWorld || level.provider instanceof WorldProviderHell || level.provider.dimensionId == -1);
    }

    @Override
    public boolean isCurrentDimensionEnd() {
        final WorldClient level = getWorld();
        return level != null && (level.provider instanceof WorldProviderEnd || level.provider.dimensionId == 1);
    }

    @Override
    public int getMinecraftRenderHeight() {
        return Minecraft.getMinecraft().getFramebuffer().framebufferHeight;
    }

    @Override
    public int getMinecraftRenderWidth() {
        return Minecraft.getMinecraft().getFramebuffer().framebufferWidth;
    }

    @Override
    public int getBedrockLevel() {
        return 0;
    }

    @Override
    public float getCloudHeight() {
        final WorldClient level = getWorld();
        return (level != null && level.provider != null) ? level.provider.getCloudHeight() : 192.0f;
    }

    @Override
    public int getHeightLimit() {
        final WorldClient level = getWorld();
        return (level != null && level.provider != null) ? level.provider.getHeight() : 256;
    }

    @Override
    public int getLogicalHeightLimit() {
        final WorldClient level = getWorld();
        return (level != null && level.provider != null) ? level.provider.getActualHeight() : 256;
    }

    @Override
    public boolean hasCeiling() {
        final WorldClient level = getWorld();
        return level != null && level.provider != null && level.provider.hasNoSky;
    }

    @Override
    public boolean hasSkyLight() {
        return !hasCeiling();
    }

    @Override
    public float getAmbientLight() {
        final WorldClient level = getWorld();
        return (level != null && level.provider != null) ? level.provider.lightBrightnessTable[0] : 0f;
    }

    @Override
    public Vector3d getPlayerLookVector() {
        return null;
    }

    @Override
    public Vector3d getPlayerBodyVector() {
        return null;
    }

    @Override
    public Vector4f getLightningBoltPosition() {
        return null;
    }

    @Override
    public float getThunderStrength() {
        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F, Minecraft.getMinecraft().theWorld.thunderingStrength);
    }

    @Override
    public float getCurrentHealth() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }

        return player.getHealth() / player.getMaxHealth();
    }

    @Override
    public float getCurrentHunger() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }

        return player.getFoodStats().getFoodLevel() / 20f;
    }

    @Override
    public float getCurrentAir() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }

        return (float) player.getAir() / (float) player.getAir();
    }

    @Override
    public float getMaxAir() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }
        return 300.0F;
    }

    @Override
    public float getCurrentArmor() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }

        return player.getTotalArmorValue() / 50.0f;
    }

    @Override
    public float getMaxHealth() {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !Minecraft.getMinecraft().playerController.gameIsSurvivalOrAdventure()) {
            return -1;
        }

        return player.getMaxHealth();
    }

    @Override
    public boolean isFirstPersonCamera() {
        return (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1);
    }

    @Override
    public boolean isSpectator() {
        // Can't seem to get WorldSettings.GameType to work here with unimined... passing on it for now
        return false;
//        final PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
//        if(controller == null)
//            return false;
//        return controller.currentGameType.getID() == 3;
    }

    final Vector3d eyePosition = new Vector3d();
    @Override
    public Vector3d getEyePosition() {
        final EntityLivingBase eye = Minecraft.getMinecraft().renderViewEntity;
        return eyePosition.set(eye.posX, eye.posY, eye.posZ);
    }

    @Override
    public boolean isOnGround() {
        return client.thePlayer != null && client.thePlayer.onGround;
    }

    @Override
    public boolean isHurt() {
        // Do not use isHurt, that's not what we want!
        return (client.thePlayer != null &&  client.thePlayer.hurtTime > 0);
    }

    @Override
    public boolean isInvisible() {
        return (client.thePlayer != null &&  client.thePlayer.isInvisible());
    }

    @Override
    public boolean isBurning() {
        return client.thePlayer != null && client.thePlayer.fire > 0 && !client.thePlayer.isImmuneToFire();
    }

    @Override
    public boolean isSneaking() {
        return (client.thePlayer != null && client.thePlayer.isSneaking());
    }

    @Override
    public boolean isSprinting() {
        return (client.thePlayer != null && client.thePlayer.isSprinting());
    }

    @Override
    public Vector3d getSkyColor() {
        if (client.theWorld == null || client.renderViewEntity == null) {
            return ZERO3D;
        }
        final Vec3 skyColor = client.theWorld.getSkyColor(client.renderViewEntity, CapturedRenderingState.INSTANCE.getTickDelta());
        return new Vector3d(skyColor.xCoord, skyColor.yCoord, skyColor.zCoord);
    }

    @Override
    public float getBlindness() {
        final EntityLivingBase cameraEntity = client.renderViewEntity;

        if (cameraEntity instanceof EntityLiving livingEntity && livingEntity.isPotionActive(Potion.blindness)) {
            final PotionEffect blindness = livingEntity.getActivePotionEffect(Potion.blindness);

            if (blindness != null) {
                // Guessing that this is what OF uses, based on how vanilla calculates the fog value in BackgroundRenderer
                // TODO: Add this to ShaderDoc
                return Math.clamp(0.0F, 1.0F, blindness.getDuration() / 20.0F);
            }
        }

        return 0.0F;
    }

    @Override
    public float getDarknessFactor() {
        // TODO: What should this be?
        return 0.0F;
    }

    @Override
    public float getPlayerMood() {
        // TODO: What should this be?
        return 0.0F;
    }

    @Override
    public float getRainStrength() {
        if (client.theWorld == null) {
            return 0f;
        }

        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F, client.theWorld.getRainStrength(CapturedRenderingState.INSTANCE.getTickDelta()));

    }

    @Override
    public Vector2i getEyeBrightness() {
        if (client.renderViewEntity == null || client.theWorld == null) {
            return ZERO2I;
        }
        // This is what ShadersMod did in 1.7.10
        final int eyeBrightness = client.renderViewEntity.getBrightnessForRender(CapturedRenderingState.INSTANCE.getTickDelta());
        return new Vector2i((eyeBrightness & 0xffff), (eyeBrightness >> 16));
    }

    @Override
    public float getNightVision() {
        Entity cameraEntity = client.renderViewEntity;

        if (cameraEntity instanceof EntityPlayer entityPlayer) {
            if (!entityPlayer.isPotionActive(Potion.nightVision)) {
                return 0.0F;
            }
            float nightVisionStrength = client.entityRenderer.getNightVisionBrightness(entityPlayer, CapturedRenderingState.INSTANCE.getTickDelta());

            try {
                if (nightVisionStrength > 0) {
                    // Just protecting against potential weird mod behavior
                    return Math.clamp(0.0F, 1.0F, nightVisionStrength);
                }
            } catch (NullPointerException e) {
                return 0.0F;
            }
        }

        return 0.0F;
    }

    @Override
    public int isEyeInWater() {
        return 0;
    }

    @Override
    public boolean hideGui() {
        return false;
    }

    @Override
    public boolean isRightHanded() {
        return false;
    }

    @Override
    public float getScreenBrightness() {
        return 0;
    }

    @Override
    public Vector2i getAtlasSize() {
        return null;
    }

    @Override
    public Vector2i getTextureSize() {
        return null;
    }

    @Override
    public MCNativeImage createNativeImage(int width, int height, boolean useCalloc) {
        // TODO
        return null;
    }

    @Override
    public MCNativeImage[] createNativeImageArray(int size) {
        // TODO
        return new MCNativeImage[0];
    }

    @Override
    public IResourceLocation makeResourceLocation(String namespace, String path) {
        return (IResourceLocation)(Object) new ResourceLocation(namespace, path);
    }

    @Override
    public IResourceLocation makeResourceLocation(String str) {
        return (IResourceLocation)(Object)new ResourceLocation(str);
    }

    @Override
    public String getOsString() {
        return switch (Platform.get()) {
            case Platform.MACOSX -> "MC_OS_MAC";
            case Platform.LINUX -> "MC_OS_LINUX";
            case Platform.WINDOWS -> "MC_OS_WINDOWS";
        };
    }

    @Override
    public String getMcVersion() {
        return Loader.MC_VERSION;
    }

    @Override
    public String getBackupVersionNumber() {
        return "1.7.10";
    }

    @Override
    public void markRendererReloadRequired() {
        // TODO
    }

    @Override
    public boolean isDHPresent() {
        return false;
    }

    @Override
    public Matrix4f getShadowModelView(float sunPathRotation, float intervalSize) {
        // TODO
        return null;
    }

    @Override
    public Matrix4f getShadowProjection(float shadowDistance, float nearPlane, float farPlane) {
        return null;
    }

    @Override
    public void bindFramebuffer() {
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
    }

    @Override
    public boolean isLoadValid() {
        return true;
    }

    @Override
    public boolean modPresent(String modid) {
        return Loader.isModLoaded(modid);
    }

    @Override
    public String getModName(String modId) {
        // TODO
        return "";
    }


    @Override
    public boolean isDevelopmentEnvironment() {
        return isDevelopmentEnvironment;
    }

    @Override
    public Path getConfigDir() {
        return Minecraft.getMinecraft().mcDataDir.toPath().resolve("config");
    }

    @Override
    public Path getGameDir() {
        return Minecraft.getMinecraft().mcDataDir.toPath();
    }
}
