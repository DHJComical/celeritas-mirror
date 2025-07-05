package org.taumc.celeritas.impl.render.terrain;

import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderFogComponent;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.terrain.SimpleWorldRenderer;
import org.taumc.celeritas.impl.extensions.RenderGlobalExtension;
import org.taumc.celeritas.impl.render.terrain.matrix.PrimitiveChunkMatrixGetter;
import org.taumc.celeritas.mixin.core.MinecraftAccessor;

import java.util.*;

/**
 * Provides an extension to vanilla's world renderer.
 */
public class CeleritasWorldRenderer extends SimpleWorldRenderer<World, PrimitiveRenderSectionManager, Integer, BlockEntity, Float> {
    @Getter
    private SpriteTransparencyTracker transparencyTracker;

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

    /**
     * @return The CeleritasWorldRenderer based on the current dimension, or null if none is attached
     */
    public static CeleritasWorldRenderer instanceNullable() {
        var world = MinecraftAccessor.celeritas$getInstance().worldRenderer;

        if (world instanceof RenderGlobalExtension extension) {
            return extension.sodium$getWorldRenderer();
        }

        return null;
    }

    @Override
    protected void loadWorld(World world) {
        this.transparencyTracker = new SpriteTransparencyTracker();
        super.loadWorld(world);
    }

    @Override
    protected CameraState captureCameraState(float ticks) {
        Entity viewEntity = Objects.requireNonNull(MinecraftAccessor.celeritas$getInstance().camera, "Client must have view entity");

        double x = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * ticks;
        double y = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * ticks + (double) viewEntity.getEyeHeight();
        double z = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * ticks;

        float pitch = viewEntity.pitch;
        float yaw = viewEntity.yaw;
        float fogDistance = ChunkShaderFogComponent.FOG_SERVICE.getFogCutoff();

        return new CameraState(x, y, z, pitch, yaw, fogDistance);
    }

    @Override
    public int getEffectiveRenderDistance() {
        //? if <1.7 {
        int viewDist = MinecraftAccessor.celeritas$getInstance().options.viewDistance;
        if (viewDist > 4) {
            System.err.println("View distance cannot be zero, resetting");
            MinecraftAccessor.celeritas$getInstance().options.viewDistance = viewDist = 0;
        }
        return 16 >> viewDist;
        //?} else
        /*return MinecraftAccessor.celeritas$getInstance().options.viewDistance;*/
    }

    @Override
    protected ChunkRenderMatrices createChunkRenderMatrices() {
        return PrimitiveChunkMatrixGetter.getMatrices();
    }

    @Override
    protected PrimitiveRenderSectionManager createRenderSectionManager(CommandList commandList) {
        return PrimitiveRenderSectionManager.create(ChunkMeshFormats.VANILLA_LIKE, this.world, this.renderDistance, commandList);
    }

    @Override
    protected void renderBlockEntityList(List<BlockEntity> list, Float partialTicksBoxed) {
        float partialTicks = partialTicksBoxed;
        for (var blockEntity : list) {
            try {
                BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, partialTicks);
            } catch(RuntimeException e) {
                if(blockEntity.isRemoved()) {
                    System.err.println("Suppressing crash from invalid tile entity");
                } else {
                    throw e;
                }
            }
        }
    }
}
