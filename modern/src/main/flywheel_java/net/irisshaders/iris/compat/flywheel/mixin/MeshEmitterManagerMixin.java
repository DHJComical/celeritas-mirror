package net.irisshaders.iris.compat.flywheel.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * On Fabric the manager itself is the consumer handed to the block renderer, so it passes the block on to the
 * emitter of every layer.
 */
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.MeshEmitterManager", remap = false)
public abstract class MeshEmitterManagerMixin implements BlockSensitiveBufferBuilder
{
    @Shadow
    @Final
    private Reference2ReferenceMap<?, ?> emitterMap;

    @Override
    public void beginBlock(short block, short renderType, byte lightEmission, int localPosX, int localPosY, int localPosZ)
    {
        for (Object emitter : emitterMap.values())
        {
            ((BlockSensitiveBufferBuilder) emitter).beginBlock(block, renderType, lightEmission, localPosX, localPosY, localPosZ);
        }
    }

    @Override
    public void endBlock()
    {
        for (Object emitter : emitterMap.values())
        {
            ((BlockSensitiveBufferBuilder) emitter).endBlock();
        }
    }
}
