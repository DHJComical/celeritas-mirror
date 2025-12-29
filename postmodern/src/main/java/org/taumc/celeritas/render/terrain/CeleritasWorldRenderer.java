package org.taumc.celeritas.render.terrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;

public class CeleritasWorldRenderer extends SimpleWorldRenderer<ClientLevel, PostmodernRenderSectionManager, ChunkSectionLayer, BlockEntity, CeleritasWorldRenderer.BlockEntityRenderContext> {
    private final Minecraft client;

    private Matrix4f poseMatrix, projectionMatrix;

    public CeleritasWorldRenderer(Minecraft client) {
        this.client = client;
    }

    /**
     * @return The CeleritasWorldRenderer based on the current dimension
     */
    public static CeleritasWorldRenderer instance() {
        var instance = instanceNullable();

        if (instance == null) {
            throw new IllegalStateException("No renderer attached to active world");
        }

        return instance;
    }

    public interface Holder {
        CeleritasWorldRenderer celeritas$getWorldRenderer();
    }

    /**
     * @return The CeleritasWorldRenderer based on the current dimension, or null if none is attached
     */
    public static CeleritasWorldRenderer instanceNullable() {
        var world = Minecraft.getInstance().levelRenderer;

        if (world instanceof Holder) {
            return ((Holder) world).celeritas$getWorldRenderer();
        }

        return null;
    }

    @Override
    public int getEffectiveRenderDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
    }

    @Override
    protected ChunkRenderMatrices createChunkRenderMatrices() {
        return new ChunkRenderMatrices(new Matrix4f(projectionMatrix), new Matrix4f(poseMatrix));
    }

    @Override
    protected PostmodernRenderSectionManager createRenderSectionManager(CommandList commandList) {
        return PostmodernRenderSectionManager.create(world, this.renderDistance, commandList);
    }

    @Override
    protected void renderBlockEntityList(List<BlockEntity> list, BlockEntityRenderContext blockEntityRenderContext) {

    }

    @Override
    public int getMinimumBuildHeight() {
        return world.getMinY();
    }

    @Override
    public int getMaximumBuildHeight() {
        return world.getMaxY() + 1;
    }

    public void setPoseMatrix(Matrix4fc poseMatrix) {
        this.poseMatrix = new Matrix4f(poseMatrix);
    }

    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public record BlockEntityRenderContext() {}
}
