package org.embeddedt.embeddium.impl.render.chunk.compile.buffers;

//? if >=1.15
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing);

    void addSprite(TextureAtlasSprite sprite);

    //? if >=1.15
    ChunkModelVertexConsumer asVertexConsumer(Material material, @Nullable BlockRenderContext ctx);
}
