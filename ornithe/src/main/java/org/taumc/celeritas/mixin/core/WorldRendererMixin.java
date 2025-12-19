package org.taumc.celeritas.mixin.core;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
//? if >=1.8 {
/*import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.block.BlockLayer;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.taumc.celeritas.impl.render.entity.EntityGatherer;
*///?}
import net.minecraft.world.World;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;
//? if <1.0.0-beta.8
/*import org.taumc.celeritas.impl.render.terrain.compile.PrimitiveBuiltRenderSectionData;*/

@Mixin(value = WorldRenderer.class, priority = 900)
public abstract class WorldRendererMixin implements RenderGlobalExtension {

    @Shadow
    private Minecraft minecraft;

    //? if <1.8 {
    @Shadow
    private int chunkGridSizeX, chunkGridSizeY, chunkGridSizeZ;
    //?}

    //? if >=1.8 {
    /*@Shadow
    private int viewDistance;

    @Shadow
    public abstract void reload();

    @Shadow
    private ClientWorld world;
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    private int globalEntityCount;
    *///?}

    private CeleritasWorldRenderer renderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.renderer = new CeleritasWorldRenderer();
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(@Coerce World world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }


    //? if <1.8 {
    @Inject(method = { "reload", "m_6748042" }, at = @At(opcode = Opcodes.PUTFIELD, value = "FIELD", target = "Lnet/minecraft/client/render/world/WorldRenderer;chunkGridSizeZ:I", shift = At.Shift.AFTER))
    private void nullifyBuiltChunkStorage(CallbackInfo ci) {
        this.chunkGridSizeX = 0;
        this.chunkGridSizeY = 0;
        this.chunkGridSizeZ = 0;
    }
    //?}

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    //? if <1.8 {
    public int render(LivingEntity viewEntity, int layer, double ticks) {
    //?} else
    /*public int render(BlockLayer layer, double ticks, int anaglyphRenderPass, Entity viewEntity) {*/
        // Allow FalseTweaks mixin to replace constant
        @SuppressWarnings("unused")
        double magicSortingConstantValue = 1.0D;
        RenderDevice.enterManagedCode();

        Lighting.turnOff();

        double d3 = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * ticks;
        // Do not apply eye height here or weird offsets will happen
        double d4 = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * ticks;
        double d5 = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * ticks;

        //? if >=1.0.0-beta.8 {
        this.minecraft.gameRenderer.enableLightMap(/*? if <1.8 {*/ticks/*?}*/);
        //?}

        try {
            this.renderer.drawChunkLayer(layer, d3, d4, d5);
        } finally {
            RenderDevice.exitManagedCode();
        }

        //? if >=1.0.0-beta.8 {
        this.minecraft.gameRenderer.disableLightMap(/*? if <1.8 {*/ticks/*?}*/);
        //?}

        return 1;
    }

    @Unique
    private int frame = 0;

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    //? if >=1.8 {
    /*public void setupRender(Entity camera, double tickDelta, Culler culler, int frame, boolean loadChunks) {
        if (this.minecraft.options.viewDistance != this.viewDistance) {
            this.reload();
        }

        updateFrustums(culler, (float)tickDelta);
    }
    *///?}
    public void updateFrustums(Culler camera, float tick) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(((ViewportProvider)camera).sodium$createViewport(),
                    CeleritasWorldRenderer.captureCameraState(tick),
                    this.frame++, this.minecraft.player.noClip, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void markDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    //? if <1.3 {
    private static final String ON_RELOAD = "m_6748042";
    //?} else
    /*private static final String ON_RELOAD = "reload()V";*/

    @Inject(method = ON_RELOAD, at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Overwrite
    //? if <1.8 {
    public boolean compileChunks(LivingEntity camera, boolean force) {
        return true;
    }
    //?} else {
    /*public void compileChunksUntil(long time) {

    }
    *///?}

    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/world/WorldRenderer;globalBlockEntities:"
            //? if >=1.8 {
            /*+ "Ljava/util/Set;"
            *///?} else
            + "Ljava/util/List;"
            , ordinal = 0))
    public void sodium$renderTileEntities(CallbackInfo ci, @Local(ordinal = 0, argsOnly = true) float partialTicks) {
        this.renderer.renderBlockEntities(partialTicks);
    }

    //? if >=1.8 {
    /*private final EntityGatherer celeritas$entityGatherer = new EntityGatherer();

    @Inject(method = "renderEntities", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=entities"))
    private void celeritas$renderEntities(Entity camera, Culler culler, float tickDelta, CallbackInfo ci, @Local(ordinal = 0) double d, @Local(ordinal = 1) double e, @Local(ordinal = 2) double g) {
        celeritas$entityGatherer.clear();
        var entityList = celeritas$entityGatherer.getLoadedEntityList(this.world);

        BlockPos.Mutable entityBlockPos = new BlockPos.Mutable();

        for (Entity entity : entityList) {
            if (!this.entityRenderDispatcher.shouldRender(entity, culler, d, e, g) && entity.rider != this.minecraft.player) {
                if (entity instanceof WitherSkullEntity) {
                    this.minecraft.getEntityRenderDispatcher().renderNameTag(entity, tickDelta);
                }
                continue;
            }

            boolean isSelfSleeping = this.minecraft.getCamera() instanceof LivingEntity ? ((LivingEntity)this.minecraft.getCamera()).isSleeping() : false;
            if (entity == this.minecraft.getCamera() && this.minecraft.options.perspective == 0 && !isSelfSleeping) {
                continue;
            }

            if (entity.y >= 0.0 && entity.y < 256.0) {
                entityBlockPos.set(MathHelper.floor(entity.x), MathHelper.floor(entity.y), MathHelper.floor(entity.z));
                if (!this.world.isChunkLoaded(entityBlockPos)) {
                    continue;
                }
            }

            this.globalEntityCount++;
            this.entityRenderDispatcher.renderSecondPass(entity, tickDelta);
        }
    }
    *///?}

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getChunkDebugInfo() {
        return this.renderer.getChunksDebugString();
    }

    //? if <1.0.0-beta.8.1 {
    /*/^*
     * @author embeddedt
     * @reason trigger chunk updates when sky light level changes
     ^/
    @Overwrite
    public void onAmbientDarknessChanged() {
        for (var section : this.renderer.getRenderSectionManager().getSectionsWithSkyLight()) {
            this.renderer.scheduleRebuildForChunk(section.getChunkX(), section.getChunkY(), section.getChunkZ(), false);
        }
    }
    *///?}
}

