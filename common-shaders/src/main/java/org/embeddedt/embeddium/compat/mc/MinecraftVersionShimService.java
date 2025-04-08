package org.embeddedt.embeddium.compat.mc;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.util.ServiceLoader;

/**
 * Service for providing version-specific Minecraft functionality.
 */
public interface MinecraftVersionShimService {
    MinecraftVersionShimService MINECRAFT_SHIM = ServiceLoader.load(MinecraftVersionShimService.class).findFirst().orElseThrow();
    Vector2i ZERO2I = new Vector2i();
    Vector3d ZERO3D = new Vector3d(0);
    Vector4f ZERO4F = new Vector4f(0);

    default boolean getSmartCull() {
        return false;
    }

    default void setSmartCull(boolean smartCull) {
        // Do nothing
    }

    default long getCurrentTick() {
        return 0L;
    }

    boolean isOnOSX();

    int getMipmapLevels();

    boolean isModLoaded(String modId);

    String translate(String key, Object... args);

    boolean isLevelLoaded();

    int getRenderDistanceInBlocks();
    int getEffectiveRenderDistance();
    Vector3d getUnshiftedCameraPosition();
    float getSkyAngle();
    void applyRotationYP(Matrix4f preCelestial, float degrees);
    void applyRotationXP(Matrix4f preCelestial, float degrees);
    void applyRotationZP(Matrix4f preCelestial, float degrees);

    int getMoonPhase();
    long getDayTime();
    long getDimensionTime(long orElse);

    boolean isCurrentDimensionNether();
    boolean isCurrentDimensionEnd();

    int getMinecraftRenderHeight();
    int getMinecraftRenderWidth();

    int getBedrockLevel();
    float getCloudHeight();
    int getHeightLimit();
    int getLogicalHeightLimit();
    boolean hasCeiling();
    boolean hasSkyLight();
    float getAmbientLight();
    Vector3d getPlayerLookVector();
    Vector3d getPlayerBodyVector();
    Vector4f getLightningBoltPosition();
    float getThunderStrength();
    float getCurrentHealth();
    float getCurrentHunger();
    float getCurrentAir();
    float getCurrentArmor();
    float getMaxAir();
    float getMaxHealth();
    boolean isFirstPersonCamera();
    boolean isSpectator();
    Vector3d getEyePosition();
    boolean isOnGround();
    boolean isHurt();
    boolean isInvisible();
    boolean isBurning();
    boolean isSneaking();
    boolean isSprinting();
    Vector3d getSkyColor();
    float getBlindness();
    float getDarknessFactor();
    float getPlayerMood();
    float getRainStrength();
    Vector2i getEyeBrightness();
    float getNightVision();
    int isEyeInWater();
    boolean hideGui();
    boolean isRightHanded();
    float getScreenBrightness();
    Vector2i getAtlasSize();
    Vector2i getTextureSize();

    MCNativeImage createNativeImage(int width, int height, boolean useCalloc);
    MCNativeImage[] createNativeImageArray(int size);

    IResourceLocation makeResourceLocation(String namespace, String path);
    IResourceLocation makeResourceLocation(String str);

    String getOsString();

    String getMcVersion();
    String getBackupVersionNumber();

    void markRendererReloadRequired();

    boolean isDHPresent();


    Matrix4f getShadowModelView(float sunPathRotation, float intervalSize);
    Matrix4f getShadowProjection(float shadowDistance, float nearPlane, float farPlane);

    void bindFramebuffer();
}
