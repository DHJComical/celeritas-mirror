package net.irisshaders.iris.compat.flywheel.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Wraps the buffer that Flywheel's model builder tesselates fluids into.
 */
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.TransformingVertexConsumer", remap = false)
public abstract class TransformingVertexConsumerMixin implements BlockSensitiveBufferBuilder
{
    @Shadow
    private VertexConsumer delegate;

    @Override
    public void beginBlock(short block, short renderType, byte lightEmission, int localPosX, int localPosY, int localPosZ)
    {
        if (delegate instanceof BlockSensitiveBufferBuilder blockSensitive)
        {
            blockSensitive.beginBlock(block, renderType, lightEmission, localPosX, localPosY, localPosZ);
        }
    }

    @Override
    public void endBlock()
    {
        if (delegate instanceof BlockSensitiveBufferBuilder blockSensitive)
        {
            blockSensitive.endBlock();
        }
    }
}
