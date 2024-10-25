package org.embeddedt.embeddium.impl.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.compat.ccl.SinkingVertexBuilder;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.embeddedt.embeddium.api.MeshAppender;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public class MeshAppenderRenderer {
    private static final Vector3fc ZERO = new Vector3f();

    private static final ThreadLocal<Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder>> BUILDERS = ThreadLocal.withInitial(() -> {
        Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder> map = new Reference2ReferenceOpenHashMap<>();
        for(RenderType layer : RenderType.chunkBufferLayers()) {
            map.put(layer, new SinkingVertexBuilder());
        }
        return map;
    });

    public static void renderMeshAppenders(List<MeshAppender> appenders, BlockAndTintGetter world, SectionPos origin, ChunkBuildBuffers buffers) {
        if (appenders.isEmpty()) {
            return;
        }

        Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder> builders = BUILDERS.get();

        for (var it = builders.reference2ReferenceEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();

            entry.getValue().reset();
        }

        MeshAppender.Context context = new MeshAppender.Context(builders::get, world, origin, buffers);
        for (MeshAppender appender : appenders) {
            appender.render(context);
        }

        var renderPassConfig = buffers.getRenderPassConfiguration();

        for (var it = builders.reference2ReferenceEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();
            var material = renderPassConfig.getMaterialForRenderType(entry.getKey());

            entry.getValue().flush(buffers.get(material), material, ZERO);
        }
    }
}
