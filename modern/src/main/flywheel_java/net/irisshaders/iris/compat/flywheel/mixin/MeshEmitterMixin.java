package net.irisshaders.iris.compat.flywheel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
//? if >=1.21
//import com.mojang.blaze3d.vertex.MeshData;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import net.irisshaders.iris.compat.flywheel.engine.IrisTerrainVertexView;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flywheel builds block models into one buffer per material. This forwards the current block to whichever buffer a
 * quad lands in, and keeps the Iris terrain attributes that Flywheel's own mesh conversion drops.
 */
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.MeshEmitter", remap = false)
public abstract class MeshEmitterMixin implements BlockSensitiveBufferBuilder
{
    // Only blocks placed in a virtual world get real light emission. Single models use -1, which the backend's
    // shaders replace with the mesh center and the owning visual's emission.
    @Unique
    private boolean iris$isTerrain;
    @Unique
    private short iris$block = -1;
    @Unique
    private short iris$renderType = -1;
    @Unique
    private byte iris$lightEmission;
    @Unique
    private int iris$localPosX;
    @Unique
    private int iris$localPosY;
    @Unique
    private int iris$localPosZ;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void iris$beginModel(CallbackInfo ci)
    {
        iris$isTerrain = false;
    }

    @Inject(method = "prepareForBlock", at = @At("HEAD"))
    private void iris$beginTerrainBlock(CallbackInfo ci)
    {
        iris$isTerrain = true;
    }

    @Override
    public void beginBlock(short block, short renderType, byte lightEmission, int localPosX, int localPosY, int localPosZ)
    {
        iris$block = block;
        iris$renderType = renderType;
        iris$lightEmission = lightEmission;
        iris$localPosX = localPosX;
        iris$localPosY = localPosY;
        iris$localPosZ = localPosZ;
    }

    @Override
    public void endBlock()
    {
        beginBlock((short) -1, (short) -1, (byte) 0, 0, 0, 0);
    }

    @ModifyReturnValue(method = "getBuffer", at = @At("RETURN"))
    private BufferBuilder iris$forwardBlock(BufferBuilder buffer)
    {
        if (buffer instanceof BlockSensitiveBufferBuilder blockSensitive)
        {
            blockSensitive.beginBlock(iris$block, iris$renderType, iris$isTerrain ? iris$lightEmission : -1,
                    iris$localPosX, iris$localPosY, iris$localPosZ);
        }

        return buffer;
    }

    @WrapOperation(method = "end", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/baked/MeshHelper;blockVerticesToMesh(" +
            /*? if >=1.21 {*/ /*"Lcom/mojang/blaze3d/vertex/MeshData;" + *//*?} else {*/ "Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;" + /*?}*/
            "Ljava/lang/String;)Ldev/engine_room/flywheel/lib/model/SimpleQuadMesh;",
            // The descriptor names Minecraft classes, which Fabric remaps even though Flywheel's own names are not.
            remap = true))
    private SimpleQuadMesh iris$keepTerrainAttributes(/*? if >=1.21 {*/ /*MeshData data *//*?} else {*/ BufferBuilder.RenderedBuffer data /*?}*/,
                                                      String meshDescriptor, Operation<SimpleQuadMesh> original)
    {
        var drawState = data.drawState();

        if (drawState.format() != IrisVertexFormats.TERRAIN)
        {
            return original.call(data, meshDescriptor);
        }

        int vertexCount = drawState.vertexCount();
        IrisTerrainVertexView vertexView = new IrisTerrainVertexView();
        MemoryBlock dst = MemoryBlock.mallocTracked((long) vertexCount * vertexView.stride());

        MemoryUtil.memCopy(MemoryUtil.memAddress(data.vertexBuffer()), dst.ptr(), (long) drawState.format().getVertexSize() * vertexCount);

        vertexView.ptr(dst.ptr());
        vertexView.vertexCount(vertexCount);
        vertexView.nativeMemoryOwner(dst);

        return new SimpleQuadMesh(vertexView, meshDescriptor);
    }
}
