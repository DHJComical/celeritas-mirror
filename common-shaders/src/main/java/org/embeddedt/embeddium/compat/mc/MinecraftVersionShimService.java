package org.embeddedt.embeddium.compat.mc;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.ServiceLoader;

/**
 * Service for providing version-specific Minecraft functionality.
 */
public interface MinecraftVersionShimService {
    MinecraftVersionShimService MINECRAFT_SHIM = ServiceLoader.load(MinecraftVersionShimService.class).findFirst().orElseThrow();
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

    MCNativeImage createNativeImage(int width, int height, boolean useCalloc);
    MCNativeImage[] createNativeImageArray(int size);

    IResourceLocation makeResourceLocation(String namespace, String path);
    IResourceLocation makeResourceLocation(String str);

    // TODO(mitchej123): Does this belong here?
    void reloadIris() throws IOException;
    boolean irisAllowConcurrentUpdate();
}
