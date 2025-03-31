package org.embeddedt.embeddium.impl;


import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.helpers.JomlConversions;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.compat.mc.IResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class MinecraftModernVersionShimImpl implements MinecraftVersionShimService {

    @Override
    public boolean getSmartCull() {
        return Minecraft.getInstance().smartCull;
    }

    @Override
    public void setSmartCull(boolean smartCull) {
        Minecraft.getInstance().smartCull = smartCull;
    }

    @Override
    public long getCurrentTick() {
        if (Minecraft.getInstance().level == null) {
            return 0L;
        } else {
            return Minecraft.getInstance().level.getGameTime();
        }
    }

    @Override
    public boolean isOnOSX() {
        return Minecraft.ON_OSX;
    }

    @Override
    public int getMipmapLevels() {
        return Minecraft.getInstance().options.mipmapLevels().get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return EarlyLoaderServices.INSTANCE.isModLoaded(modId);
    }

    @Override
    public String translate(String key, Object... args) {
        return I18n.get(key, args);
    }

    @Override
    public boolean isLevelLoaded() {
        return Minecraft.getInstance().level != null;
    }

    @Override
    public int getRenderDistanceInBlocks() {
        // TODO: Should we ask the game renderer for this?
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public Vector3d getUnshiftedCameraPosition() {
        return JomlConversions.fromVec3(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    private ClientLevel getWorld() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }
    @Override
    public float getSkyAngle() {
        return getWorld().getTimeOfDay(CapturedRenderingState.INSTANCE.getTickDelta());
    }

    @Override
    public void applyRotationYP(Matrix4f mat, float degrees) {
        mat.rotate(Axis.YP.rotationDegrees(degrees));
    }

    @Override
    public void applyRotationXP(Matrix4f mat, float degrees) {
        mat.rotate(Axis.XP.rotationDegrees(degrees));
    }

    @Override
    public void applyRotationZP(Matrix4f mat, float degrees) {
        mat.rotate(Axis.ZP.rotationDegrees(degrees));
    }

    @Override
    public int getMoonPhase() {
        return getWorld().getMoonPhase();
    }

    @Override
    public long getDayTime() {
        return getWorld().getDayTime();
    }

    @Override
    public long getDimensionTime(long orElse) {
        return getWorld().dimensionType().fixedTime().orElse(orElse);
    }

    @Override
    public boolean isCurrentDimensionNether() {
        return Iris.getCurrentDimension() == DimensionId.NETHER;
    }

    @Override
    public boolean isCurrentDimensionEnd() {
        return Iris.getCurrentDimension() == DimensionId.END;
    }

    @Override
    public int getMinecraftRenderHeight() {
        return Minecraft.getInstance().getMainRenderTarget().height;
    }

    @Override
    public int getMinecraftRenderWidth() {
        return Minecraft.getInstance().getMainRenderTarget().width;
    }

    @Override
    public int getBedrockLevel() {
        final ClientLevel level = getWorld();
        return level != null ? level.dimensionType().minY() : 0;
    }

    @Override
    public float getCloudHeight() {
        final ClientLevel level = getWorld();
        return level != null ? level.effects().getCloudHeight() : 192.0f;
    }

    @Override
    public int getHeightLimit() {
        final ClientLevel level = getWorld();
        return level != null ? level.dimensionType().height() : 256;
    }

    @Override
    public int getLogicalHeightLimit() {
        final ClientLevel level = getWorld();
        return level != null ? level.dimensionType().logicalHeight() : 256;
    }

    @Override
    public boolean hasCeiling() {
        final ClientLevel level = getWorld();
        return level != null && level.dimensionType().hasCeiling();
    }

    @Override
    public boolean hasSkyLight() {
        final ClientLevel level = getWorld();
        return level == null || level.dimensionType().hasSkyLight();
    }

    @Override
    public float getAmbientLight() {
        final ClientLevel level = getWorld();
        return level != null ? level.dimensionType().ambientLight() : 0f;
    }

    @Override
    public Vector3d getPlayerLookVector() {
        if (Minecraft.getInstance().cameraEntity instanceof LivingEntity livingEntity) {
            return JomlConversions.fromVec3(livingEntity.getViewVector(CapturedRenderingState.INSTANCE.getTickDelta()));
        } else {
            return ZERO3D;
        }
    }

    @Override
    public Vector3d getPlayerBodyVector() {
        return JomlConversions.fromVec3(Minecraft.getInstance().getCameraEntity().getForward());
    }

    @Override
    public Vector4f getLightningBoltPosition() {
        if (Minecraft.getInstance().level != null) {
            return StreamSupport.stream(Minecraft.getInstance().level.entitiesForRendering().spliterator(), false).filter(bolt -> bolt instanceof LightningBolt).findAny().map(bolt -> {
                Vector3d unshiftedCameraPosition = CameraUniforms.getUnshiftedCameraPosition();
                //? if >=1.21 {
                /*float deltaFrameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                 *///?} else {
                float deltaFrameTime = Minecraft.getInstance().getDeltaFrameTime();
                //?}
                Vec3 vec3 = bolt.getPosition(deltaFrameTime);
                return new Vector4f((float) (vec3.x - unshiftedCameraPosition.x), (float) (vec3.y - unshiftedCameraPosition.y), (float) (vec3.z - unshiftedCameraPosition.z), 1);
            }).orElse(ZERO4F);
        } else {
            return ZERO4F;
        }
    }

    @Override
    public float getThunderStrength() {
        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F,
                Minecraft.getInstance().level.getThunderLevel(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    @Override
    public float getCurrentHealth() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return Minecraft.getInstance().player.getHealth() / Minecraft.getInstance().player.getMaxHealth();
    }

    @Override
    public float getCurrentHunger() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return Minecraft.getInstance().player.getFoodData().getFoodLevel() / 20f;
    }

    @Override
    public float getCurrentAir() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return (float) Minecraft.getInstance().player.getAirSupply() / (float) Minecraft.getInstance().player.getMaxAirSupply();
    }

    @Override
    public float getCurrentArmor() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return Minecraft.getInstance().player.getArmorValue() / 50.0f;
    }

    @Override
    public float getMaxAir() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return Minecraft.getInstance().player.getMaxAirSupply();
    }

    @Override
    public float getMaxHealth() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().gameMode.getPlayerMode().isSurvival()) {
            return -1;
        }

        return Minecraft.getInstance().player.getMaxHealth();
    }

    @Override
    public boolean isFirstPersonCamera() {
        // If camera type is not explicitly third-person, assume it's first-person.
        return switch (Minecraft.getInstance().options.getCameraType()) {
            case THIRD_PERSON_BACK, THIRD_PERSON_FRONT -> false;
            default -> true;
        };
    }

    @Override
    public boolean isSpectator() {
        return Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR;
    }

    @Override
    public Vector3d getEyePosition() {
        Objects.requireNonNull(Minecraft.getInstance().getCameraEntity());
        Vec3 pos = Minecraft.getInstance().getCameraEntity().getEyePosition(CapturedRenderingState.INSTANCE.getTickDelta());
        return new Vector3d(pos.x, pos.y, pos.z);
    }

    @Override
    public MCNativeImage createNativeImage(int width, int height, boolean useCalloc) {
        return (MCNativeImage)(Object) new NativeImage(width, height, useCalloc);
    }

    @Override
    public MCNativeImage[] createNativeImageArray(int size) {
        return (MCNativeImage[])(Object[])new NativeImage[size];
    }

    public IResourceLocation makeResourceLocation(String namespace, String path) {
        //? if >=1.21
        /*return (IResourceLocation)(Object) ResourceLocation.fromNamespaceAndPath(namespace, path);*/
        //? if <1.21
        return (IResourceLocation)(Object) new ResourceLocation(namespace, path);
    }

    public IResourceLocation makeResourceLocation(String str) {
        //? if >=1.21 {
        /*if(str.contains(":")) {
            return (IResourceLocation)(Object) ResourceLocation.parse(str);
        } else {
            return (IResourceLocation)(Object) ResourceLocation.withDefaultNamespace(str);
        }
        *///?} else
        return (IResourceLocation)(Object)new ResourceLocation(str);
    }

    @Override
    public void reloadIris() throws IOException {
        Iris.reload();
    }

    @Override
    public boolean irisAllowConcurrentUpdate() {
        return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::allowConcurrentCompute).orElse(false);
    }


}
