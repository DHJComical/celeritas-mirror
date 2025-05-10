package org.embeddedt.embeddium.impl;


import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.helpers.JomlConversions;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.shadows.ShadowMatrices;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.texture.TextureInfoCache;
import net.irisshaders.iris.texture.TextureTracker;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.compat.mc.IResourceLocation;
import org.embeddedt.embeddium.compat.mc.MCAbstractTexture;
import org.embeddedt.embeddium.compat.mc.MCNativeImage;
import org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService;
import net.minecraft.client.Minecraft;
import org.embeddedt.embeddium.impl.loader.common.EarlyLoaderServices;
import org.joml.*;
import org.joml.Math;

import java.util.Objects;
import java.util.stream.StreamSupport;

import static com.mitchej123.glsm.RenderSystemService.RENDER_SYSTEM;


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
    public int getEffectiveRenderDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
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
    public boolean isOnGround() {
        final Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.onGround();
    }

    @Override
    public boolean isHurt() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.hurtTime > 0; // Do not use isHurt, that's not what we want!
        } else {
            return false;
        }
    }

    @Override
    public boolean isInvisible() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.isInvisible();
        } else {
            return false;
        }
    }

    @Override
    public boolean isBurning() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.isOnFire();
        } else {
            return false;
        }
    }

    @Override
    public boolean isSneaking() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.isCrouching();
        } else {
            return false;
        }
    }

    @Override
    public boolean isSprinting() {
        final Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.isSprinting();
        } else {
            return false;
        }
    }

    @Override
    public Vector3d getSkyColor() {
        final Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.cameraEntity == null) {
            return ZERO3D;
        }

        return JomlConversions.fromVec3(client.level.getSkyColor(client.cameraEntity.position(),
                CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    @Override
    public float getBlindness() {
        final Minecraft client = Minecraft.getInstance();
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity instanceof LivingEntity) {
            MobEffectInstance blindness = ((LivingEntity) cameraEntity).getEffect(MobEffects.BLINDNESS);

            if (blindness != null) {
                // Guessing that this is what OF uses, based on how vanilla calculates the fog value in FogRenderer
                // TODO: Add this to ShaderDoc
                if (blindness.isInfiniteDuration()) {
                    return 1.0f;
                } else {
                    return Math.clamp(0.0F, 1.0F, blindness.getDuration() / 20.0F);
                }
            }
        }

        return 0.0F;
    }

    @Override
    public float getDarknessFactor() {
        final Minecraft client = Minecraft.getInstance();
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity instanceof LivingEntity) {
            MobEffectInstance darkness = ((LivingEntity) cameraEntity).getEffect(MobEffects.DARKNESS);

            //? if <1.20.6 {
            if (darkness != null && darkness.getFactorData().isPresent()) {
                return darkness.getFactorData().get().getFactor((LivingEntity) cameraEntity, CapturedRenderingState.INSTANCE.getTickDelta());
            }
            //?} else {
            /*if (darkness != null) {
                return darkness.getBlendFactor((LivingEntity) cameraEntity, CapturedRenderingState.INSTANCE.getTickDelta());
            }
            *///?}
        }

        return 0.0F;
    }

    @Override
    public float getPlayerMood() {
        final Minecraft client = Minecraft.getInstance();
        if (!(client.cameraEntity instanceof LocalPlayer)) {
            return 0.0F;
        }

        // This should always be 0 to 1 anyways but just making sure
        return Math.clamp(0.0F, 1.0F, ((LocalPlayer) client.cameraEntity).getCurrentMood());
    }

    @Override
    public float getRainStrength() {
        final Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return 0f;
        }

        // Note: Ensure this is in the range of 0 to 1 - some custom servers send out of range values.
        return Math.clamp(0.0F, 1.0F,
                client.level.getRainLevel(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    @Override
    public Vector2i getEyeBrightness() {
        final Minecraft client = Minecraft.getInstance();
        if (client.cameraEntity == null || client.level == null) {
            return ZERO2I;
        }

        Vec3 feet = client.cameraEntity.position();
        Vec3 eyes = new Vec3(feet.x, client.cameraEntity.getEyeY(), feet.z);
        BlockPos eyeBlockPos = BlockPos.containing(eyes);

        int blockLight = client.level.getBrightness(LightLayer.BLOCK, eyeBlockPos);
        int skyLight = client.level.getBrightness(LightLayer.SKY, eyeBlockPos);

        return new Vector2i(blockLight * 16, skyLight * 16);
    }

    @Override
    public float getNightVision() {
        final Minecraft client = Minecraft.getInstance();
        Entity cameraEntity = client.getCameraEntity();

        if (cameraEntity instanceof LivingEntity livingEntity) {

            try {
                // See MixinGameRenderer#iris$safecheckNightvisionStrength.
                //
                // We modify the behavior of getNightVisionScale so that it's safe for us to call it even on entities
                // that don't have the effect, allowing us to pick up modified night vision strength values from mods
                // like Origins.
                //
                // See: https://github.com/apace100/apoli/blob/320b0ef547fbbf703de7154f60909d30366f6500/src/main/java/io/github/apace100/apoli/mixin/GameRendererMixin.java#L153
                float nightVisionStrength =
                        GameRenderer.getNightVisionScale(livingEntity, CapturedRenderingState.INSTANCE.getTickDelta());

                if (nightVisionStrength > 0) {
                    // Just protecting against potential weird mod behavior
                    return Math.clamp(0.0F, 1.0F, nightVisionStrength);
                }
            } catch (NullPointerException e) {
                // If our injection didn't get applied, a NullPointerException will occur from calling that method if
                // the entity doesn't currently have night vision. This isn't pretty but it's functional.
                return 0.0F;
            }
        }

        // Conduit power gives the player a sort-of night vision effect when underwater.
        // This lets existing shaderpacks be compatible with conduit power automatically.
        //
        // Yes, this should be the player entity, to match LightTexture.
        if (client.player != null && client.player.hasEffect(MobEffects.CONDUIT_POWER)) {
            float underwaterVisibility = client.player.getWaterVision();

            if (underwaterVisibility > 0.0f) {
                // Just protecting against potential weird mod behavior
                return Math.clamp(0.0F, 1.0F, underwaterVisibility);
            }
        }

        return 0.0F;
    }

    @Override
    public int isEyeInWater() {
        // Note: With certain utility / cheat mods, this method will return air even when the player is submerged when
        // the "No Overlay" feature is enabled.
        //
        // I'm not sure what the best way to deal with this is, but the current approach seems to be an acceptable one -
        // after all, disabling the overlay results in the intended effect of it not really looking like you're
        // underwater on most shaderpacks. For now, I will leave this as-is, but it is something to keep in mind.
        final Minecraft client = Minecraft.getInstance();
        FogType submersionType = client.gameRenderer.getMainCamera().getFluidInCamera();

        if (submersionType == FogType.WATER) {
            return 1;
        } else if (submersionType == FogType.LAVA) {
            return 2;
        } else if (submersionType == FogType.POWDER_SNOW) {
            return 3;
        } else {
            return 0;
        }
    }

    @Override
    public boolean hideGui() {
        final Minecraft client = Minecraft.getInstance();
        return client.options.hideGui;
    }

    @Override
    public boolean isRightHanded() {
        final Minecraft client = Minecraft.getInstance();
        return client.options.mainHand().get() == HumanoidArm.RIGHT;
    }

    @Override
    public float getScreenBrightness() {
        final Minecraft client = Minecraft.getInstance();
        return client.options.gamma().get().floatValue();
    }

    @Override
    public Vector2i getAtlasSize() {
        int glId = RENDER_SYSTEM.getShaderTexture(0);

        MCAbstractTexture texture = TextureTracker.INSTANCE.getTexture(glId);
        if (texture instanceof TextureAtlas atlas) {
            TextureAtlasAccessor atlasAccessor = (TextureAtlasAccessor) atlas;
            return new Vector2i(atlasAccessor.callGetWidth(), atlasAccessor.callGetHeight());
        }

        return ZERO2I;
    }

    @Override
    public Vector2i getTextureSize() {
        int glId = GlStateManagerAccessor.getTEXTURES()[0].binding;

        TextureInfoCache.TextureInfo info = TextureInfoCache.INSTANCE.getInfo(glId);
        return new Vector2i(info.getWidth(), info.getHeight());
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
    public String getOsString() {
        return switch (Util.getPlatform()) {
            case OSX -> "MC_OS_MAC";
            case LINUX -> "MC_OS_LINUX";
            case WINDOWS -> "MC_OS_WINDOWS";
            // Note: Optifine doesn't have a macro for Solaris. https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L709-L714
            default -> "MC_OS_UNKNOWN";
        };
    }


    @Override
    public String getMcVersion() {
        return Iris.getReleaseTarget();
    }

    @Override
    public String getBackupVersionNumber() {
        return Iris.getBackupVersionNumber();
    }

    @Override
    public void markRendererReloadRequired() {
        if (Minecraft.getInstance().levelRenderer != null) {
            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    @Override
    public boolean isDHPresent() {
        return EarlyLoaderServices.INSTANCE.isModLoaded("distanthorizons");
    }

    @Override
    public Matrix4f getShadowModelView(float sunPathRotation, float intervalSize) {
        return new Matrix4f(ShadowRenderer.createShadowModelView(sunPathRotation, intervalSize).last().pose());
    }

    @Override
    public Matrix4f getShadowProjection(float shadowDistance, float nearPlane, float farPlane) {
        return ShadowMatrices.createOrthoMatrix(shadowDistance, nearPlane, farPlane);
    }

    @Override
    public void bindFramebuffer() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

}
