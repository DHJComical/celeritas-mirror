package org.embeddedt.embeddium.impl.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import org.embeddedt.embeddium.impl.common.util.NativeBuffer;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

public class ChunkSortOutput extends ChunkTaskOutput {
    public record SortedMesh(NativeBuffer indexData) {}

    public final Reference2ReferenceMap<TerrainRenderPass, SortedMesh> meshes;

    public ChunkSortOutput(RenderSection render, int buildTime, Reference2ReferenceMap<TerrainRenderPass, SortedMesh> meshes) {
        super(render, buildTime);
        this.meshes = meshes;
    }

    @Override
    public void delete() {
        for (SortedMesh data : this.meshes.values()) {
            data.indexData().free();
        }
    }
}
