package org.taumc.celeritas.mixin.core.terrain;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.CeleritasWorldRenderer;

import java.util.*;
import java.util.function.Consumer;

@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin implements RenderGlobalExtension {

    @Shadow
    @Final
    private Map<Integer, DestroyBlockProgress> damagedBlocks;

    @Shadow @Final private Minecraft mc;
    @Shadow
    @Final
    private RenderManager renderManager;
    @Shadow
    private int countEntitiesRendered;

    @Shadow
    protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    @Shadow
    private WorldClient world;
    private CeleritasWorldRenderer renderer;

    @Redirect(method = "loadRenderers", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", ordinal = 1))
    private int nullifyBuiltChunkStorage(GameSettings settings) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, CallbackInfo ci) {
        this.renderer = new CeleritasWorldRenderer(minecraft);
    }

    @Override
    public CeleritasWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Inject(method = "setWorldAndLoadRenderers", at = @At("RETURN"))
    private void onWorldChanged(WorldClient world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int getRenderedChunks() {
        return this.renderer.getVisibleChunkCount();
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean hasNoChunkUpdates() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "setDisplayListEntitiesDirty", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int renderBlockLayer(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn) {
        RenderDevice.enterManagedCode();

        RenderHelper.disableStandardItemLighting();

        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(this.mc.getTextureMapBlocks().getGlTextureId());
        GlStateManager.enableTexture2D();

        this.mc.entityRenderer.enableLightmap();

        double d3 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * partialTicks;
        double d4 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * partialTicks;
        double d5 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * partialTicks;

        try {
            this.renderer.drawChunkLayer(blockLayerIn, d3, d4, d5);
        } finally {
            RenderDevice.exitManagedCode();
        }

        this.mc.entityRenderer.disableLightmap();

        return 1;
    }

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setupTerrain(Entity entity, double tick, ICamera camera, int frame, boolean spectator) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(((ViewportProvider)camera).sodium$createViewport(), (float)tick, frame, spectator, false);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, important);
    }

    // The following two redirects force light updates to trigger chunk updates and not check vanilla's chunk renderer
    // flags
    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;hasNoFreeRenderBuilders()Z"))
    private boolean alwaysHaveBuilders(ChunkRenderDispatcher instance) {
        return false;
    }

    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 1))
    private boolean alwaysHaveNoTasks(Set instance) {
        return true;
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V", shift = At.Shift.AFTER, ordinal = 1), cancellable = true)
    public void sodium$renderTileEntities(Entity entity, ICamera camera, float partialTicks, CallbackInfo ci) {
        this.renderer.renderBlockEntities(partialTicks, damagedBlocks);

        this.mc.entityRenderer.disableLightmap();
        this.mc.profiler.endSection();
        ci.cancel();
    }

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getDebugInfoRenders() {
        return this.renderer.getChunksDebugString();
    }

    private List<Entity>[] getLoadedEntityList() {
        int numPasses = 2;
        List<Entity>[] passesArray = new List[numPasses];
        for (int i = 0; i < numPasses; i++) {
            passesArray[i] = new ArrayList<>();
        }
        Consumer<Entity> addEntity = entity -> {
            for (int i = 0; i < numPasses; i++) {
                if (entity.shouldRenderInPass(i)) {
                    passesArray[i].add(entity);
                }
            }
        };
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (this.world.getChunkProvider() instanceof ChunkProviderClientAccessor provider) {
            var loadedChunks = provider.celeritas$getLoadedChunks();
            List<Entity> allEntities = new ArrayList<>(this.world.loadedEntityList.size());
            for (Chunk chunk : loadedChunks.values()) {
                if (!((ChunkAccessor)chunk).celeritas$getHasEntities()) {
                    continue;
                }
                ClassInheritanceMultiMap<Entity>[] entityMaps = chunk.getEntityLists();
                for (ClassInheritanceMultiMap<Entity> map : entityMaps) {
                    map.forEach(addEntity);
                }
            }
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            this.world.loadedEntityList.forEach(addEntity);
        }
        return passesArray;
    }

    private List<Entity>[] celeritas$collectedEntities;

    /**
     * @author embeddedt
     * @reason reimplement entity render loop because vanilla's relies on the renderInfos list
     */
    @Inject(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderInfos:Ljava/util/List;", ordinal = 0))
    private void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci,
                                @Local(ordinal = 1) List<Entity> outlineEntityList,
                                @Local(ordinal = 2) List<Entity> multipassEntityList,
                                @Local(ordinal = 0) double renderViewX,
                                @Local(ordinal = 1) double renderViewY,
                                @Local(ordinal = 2) double renderViewZ) {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        if (pass == 0 || celeritas$collectedEntities == null) {
            celeritas$collectedEntities = getLoadedEntityList();
        }
        EntityPlayerSP player = this.mc.player;
        BlockPos.MutableBlockPos entityBlockPos = new BlockPos.MutableBlockPos();
        // Apply entity distance scaling
        Entity.setRenderDistanceWeight(MathHelper.clamp((double)this.mc.gameSettings.renderDistanceChunks / 8.0D, 1.0D, 2.5D) * 1);

        for(Entity entity : celeritas$collectedEntities[pass]) {
            // Do regular vanilla checks for visibility
            if(!this.renderManager.shouldRender(entity, camera, renderViewX, renderViewY, renderViewZ) && !entity.isRidingOrBeingRiddenBy(player)) {
                continue;
            }

            // Check if any corners of the bounding box are in a visible subchunk
            if(!CeleritasWorldRenderer.instance().isEntityVisible(entity)) {
                continue;
            }

            boolean isSleeping = renderViewEntity instanceof EntityLivingBase && ((EntityLivingBase) renderViewEntity).isPlayerSleeping();

            if ((entity != renderViewEntity || this.mc.gameSettings.thirdPersonView != 0 || isSleeping)
                    && (entity.posY < 0.0D || entity.posY >= 256.0D || this.world.isBlockLoaded(entityBlockPos.setPos(entity))))
            {
                ++this.countEntitiesRendered;
                this.renderManager.renderEntityStatic(entity, partialTicks, false);

                if (this.isOutlineActive(entity, renderViewEntity, camera))
                {
                    outlineEntityList.add(entity);
                }

                if (this.renderManager.isRenderMultipass(entity)) {
                    multipassEntityList.add(entity);
                }
            }
        }
    }
}

