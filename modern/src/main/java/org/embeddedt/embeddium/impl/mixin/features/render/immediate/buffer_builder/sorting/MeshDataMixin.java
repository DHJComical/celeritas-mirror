package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.sorting;

//? if >=1.21 {

/*//? if >=1.21.9-beta.1
/^import com.mojang.blaze3d.vertex.CompactVectorArray;^/
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.nio.ByteBuffer;

@Mixin(MeshData.class)
public abstract class MeshDataMixin {
    /^*
     * @author JellySquid
     * @reason Avoid slow memory accesses
     ^/
    @Overwrite
    private static
    /^? if <1.21.9-beta.1 {^/Vector3f[] /^?} else {^/ /^CompactVectorArray ^//^?}^/
    unpackQuadCentroids(ByteBuffer buffer, int vertices, VertexFormat format) {
        int vertexStride = format.getVertexSize();
        int primitiveCount = vertices / 4;

        //? if <1.21.9-beta.1 {
        Vector3f[] centers = new Vector3f[primitiveCount];
        //?} else
        /^CompactVectorArray centers = new CompactVectorArray(primitiveCount);^/

        int renderedBufferPointer = buffer.position();

        for (int index = 0; index < primitiveCount; ++index) {
            long v1 = MemoryUtil.memAddress(buffer, renderedBufferPointer + (((index * 4) + 0) * vertexStride));
            long v2 = MemoryUtil.memAddress(buffer, renderedBufferPointer + (((index * 4) + 2) * vertexStride));

            float x1 = MemoryUtil.memGetFloat(v1 + 0);
            float y1 = MemoryUtil.memGetFloat(v1 + 4);
            float z1 = MemoryUtil.memGetFloat(v1 + 8);

            float x2 = MemoryUtil.memGetFloat(v2 + 0);
            float y2 = MemoryUtil.memGetFloat(v2 + 4);
            float z2 = MemoryUtil.memGetFloat(v2 + 8);

            //? if <1.21.9-beta.1 {
            centers[index] = new Vector3f(
            //?} else
            /^centers.set(index,^/
                    (x1 + x2) * 0.5F, (y1 + y2) * 0.5F, (z1 + z2) * 0.5F
            );
        }

        return centers;
    }
}

*///?}
