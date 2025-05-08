package org.embeddedt.embeddium.impl.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.data.BuiltSectionMeshParts;
import org.jetbrains.annotations.Nullable;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class ChunkBuildOutput {
    public final RenderSection render;

    public final @Nullable BuiltRenderSectionData info;
    public final Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes;

    public final int buildTime;

    private boolean partialUpload;

    public ChunkBuildOutput(RenderSection render, @Nullable BuiltRenderSectionData info, Reference2ReferenceMap<TerrainRenderPass, BuiltSectionMeshParts> meshes, int buildTime) {
        this.render = render;
        this.info = info;
        this.meshes = meshes;

        this.buildTime = buildTime;

        if (this.info != null) {
            this.info.bake();
        }
    }

    public BuiltSectionMeshParts getMesh(TerrainRenderPass pass) {
        return this.meshes.get(pass);
    }

    public void delete() {
        for (BuiltSectionMeshParts data : this.meshes.values()) {
            if(data.getVertexData() != null) {
                data.getVertexData().free();
            }
            if(data.getIndexData() != null) {
                data.getIndexData().free();
            }
        }
    }

    public boolean isIndexOnlyUpload() {
        return partialUpload;
    }

    public void setIndexOnlyUpload(boolean flag) {
        partialUpload = flag;
    }
}
