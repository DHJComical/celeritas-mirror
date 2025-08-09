package org.taumc.celeritas.impl.render.terrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.MinecraftForgeClient;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.joml.Matrix4f;
import org.taumc.celeritas.CeleritasVintage;
import org.taumc.celeritas.mixin.core.terrain.ActiveRenderInfoAccessor;

import java.util.*;

/**
 * Provides an extension to vanilla's {@link net.minecraft.client.renderer.RenderGlobal}.
 */
public class CeleritasWorldRenderer extends SimpleWorldRenderer<WorldClient, VintageRenderSectionManager, BlockRenderLayer, TileEntity, CeleritasWorldRenderer.TileEntityRenderContext>  {
    public record TileEntityRenderContext(Map<Integer, DestroyBlockProgress> damagedBlocks, float partialTicks) {}

    /**
     * @return The CeleritasWorldRenderer based on the current dimension
     */
    public static CeleritasWorldRenderer instance() {
        return SimpleWorldRenderer.Provider.getWorldRenderer(Minecraft.getMinecraft().renderGlobal);
    }

    /**
     * @return The CeleritasWorldRenderer based on the current dimension, or null if none is attached
     */
    public static CeleritasWorldRenderer instanceNullable() {
        return SimpleWorldRenderer.Provider.getWorldRendererNullable(Minecraft.getMinecraft().renderGlobal);
    }

    @Override
    public int getEffectiveRenderDistance() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
    }

    @Override
    protected ChunkRenderMatrices createChunkRenderMatrices() {
        return new ChunkRenderMatrices(ActiveRenderInfoAccessor.getProjectionMatrix(), ActiveRenderInfoAccessor.getModelViewMatrix());
    }

    @Override
    protected VintageRenderSectionManager createRenderSectionManager(CommandList commandList) {
        return VintageRenderSectionManager.create(chooseVertexType(), this.world, this.getEffectiveRenderDistance(), commandList);
    }

    /**
     * Performs a render pass for the given {@link BlockRenderLayer} and draws all visible chunks for it.
     */
    public void drawChunkLayer(BlockRenderLayer renderLayer, double x, double y, double z) {
        super.drawChunkLayer(renderLayer, x, y, z);

        GlStateManager.resetColor();
    }

    @Override
    protected CameraState captureCameraState(float ticks) {
        Entity viewEntity = Objects.requireNonNull(Minecraft.getMinecraft().getRenderViewEntity(), "Client must have view entity");

        double x = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * ticks;
        double y = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * ticks + (double) viewEntity.getEyeHeight();
        double z = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * ticks;

        float pitch = viewEntity.rotationPitch;
        float yaw = viewEntity.rotationYaw;
        float fogDistance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        return new CameraState(x, y, z, pitch, yaw, fogDistance);
    }


    @Override
    protected void renderBlockEntityList(List<TileEntity> list, TileEntityRenderContext tileEntityRenderContext) {
        int pass = MinecraftForgeClient.getRenderPass();
        float partialTicks = tileEntityRenderContext.partialTicks;

        for (TileEntity tileEntity : list) {
            if(!tileEntity.shouldRenderInPass(pass))
                continue;

            try {
                TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
            } catch(RuntimeException e) {
                if(tileEntity.isInvalid()) {
                    CeleritasVintage.logger().error("Suppressing crash from invalid tile entity", e);
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void renderBlockEntities(TileEntityRenderContext tileEntityRenderContext) {
        int pass = MinecraftForgeClient.getRenderPass();
        TileEntityRendererDispatcher.instance.preDrawBatch();
        super.renderBlockEntities(tileEntityRenderContext);
        TileEntityRendererDispatcher.instance.drawBatch(pass);
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!CeleritasVintage.options().performance.useEntityCulling || this.renderSectionManager.isInShadowPass()) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (entity.isGlowing() || entity.getAlwaysRenderNameTagForRender()) {
            return true;
        }

        //? if <1.21.2
        AxisAlignedBB box = entity.getRenderBoundingBox();
        //? if >=1.21.2
        /*AABB box = renderer.getBoundingBoxForCulling(entity);*/

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private ChunkVertexType chooseVertexType() {
        if (!CeleritasVintage.options().performance.useCompactVertexFormat) {
            return ChunkMeshFormats.VANILLA_LIKE;
        }

        return ChunkMeshFormats.COMPACT;
    }
}
