package org.embeddedt.embeddium.impl.chunk;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
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

        ReferenceArraySet<Material> usedMaterials = new ReferenceArraySet<>();

        MeshAppender.Context context = new MeshAppender.Context(type -> {
            var material = buffers.getRenderPassConfiguration().getMaterialForRenderType(type);
            usedMaterials.add(material);
            return buffers.get(material).asVertexConsumer(material, null);
        }, world, origin, buffers);

        for (MeshAppender appender : appenders) {
            appender.render(context);
        }

        if (!usedMaterials.isEmpty()) {
            for (Material material : usedMaterials) {
                buffers.get(material).asVertexConsumer(material, null).close();
            }
        }
    }
}
