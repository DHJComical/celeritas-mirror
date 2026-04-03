package org.embeddedt.embeddium.impl.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.modern.render.chunk.MojangVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.embeddedt.embeddium.api.MeshAppender;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;

import java.util.List;

public class MeshAppenderRenderer {
    public static void renderMeshAppenders(List<MeshAppender> appenders, BlockAndTintGetter world, SectionPos origin, ChunkBuildBuffers buffers) {
        if (appenders.isEmpty()) {
            return;
        }

        Reference2ReferenceArrayMap<Material, MojangVertexConsumer> usedMaterials = new Reference2ReferenceArrayMap<>();

        MeshAppender.Context context = new MeshAppender.Context(type -> {
            var material = ((RenderPassConfiguration<RenderType>)buffers.getRenderPassConfiguration()).getMaterialForRenderType(type);
            var vertexConsumer = usedMaterials.get(material);
            if (vertexConsumer == null) {
                vertexConsumer = new MojangVertexConsumer();
                vertexConsumer.initialize(buffers.get(material), material, null);
                usedMaterials.put(material, vertexConsumer);
            }
            return vertexConsumer;
        }, world, origin, buffers);

        for (MeshAppender appender : appenders) {
            appender.render(context);
        }

        if (!usedMaterials.isEmpty()) {
            usedMaterials.values().forEach(MojangVertexConsumer::close);
        }
    }
}
