package org.embeddedt.embeddium.impl.render.chunk.vertex.format;

import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.jetbrains.annotations.Nullable;

public interface ChunkVertexEncoder {
    long write(long ptr, Material material, Vertex vertex, int sectionIndex, @Nullable BlockRenderContext ctx);

    class Vertex {
        public float x;
        public float y;
        public float z;
        public int color;
        public float u;
        public float v;
        public int light;

        public static Vertex[] uninitializedQuad() {
            Vertex[] vertices = new Vertex[4];

            for (int i = 0; i < 4; i++) {
                vertices[i] = new Vertex();
            }

            return vertices;
        }
    }
}
