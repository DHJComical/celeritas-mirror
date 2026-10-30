package org.embeddedt.embeddium.impl.modern.render.chunk;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.util.ARGB;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class MojangVertexConsumer implements VertexConsumer, AutoCloseable {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private ChunkVertexEncoder.Vertex currentVertexObj;
    private int currentIndex = -1;
    private final Vector3f computedNormal = new Vector3f();
    private Material material;
    private float xOff, yOff, zOff;
    private ChunkModelBuilder targetBuilder;

    public MojangVertexConsumer initialize(ChunkModelBuilder targetBuilder, Material material, @Nullable BlockRenderContext ctx) {
        if ((currentIndex + 1) == 4) {
            // Flush the last quad that was rendered, so the material isn't switched underneath it.
            flushQuad();
        }
        this.targetBuilder = targetBuilder;
        this.material = material;
        this.xOff = this.yOff = this.zOff = 0;
        this.currentIndex = -1;
        return this;
    }

    private void flushQuad() {
        var n = computedNormal;
        ModelQuadUtil.calculateNormal(vertices, n);
        int packedNormal = NormI8.pack(n);
        var vertices = this.vertices;
        for (int i = 0; i < 4; i++) {
            vertices[i].trueNormal = packedNormal;
        }
        this.targetBuilder.getVertexBuffer(ModelQuadUtil.findNormalFace(n.x, n.y, n.z)).push(vertices, material);
        currentIndex = -1;
    }

    private ChunkVertexEncoder.Vertex nextVertex() {
        int nextIndex = currentIndex + 1;
        if (nextIndex == 4) {
            flushQuad();
            nextIndex = 0;
        }
        currentIndex = nextIndex;
        return currentVertexObj = this.vertices[nextIndex];
    }

    public void embeddium$setOffset(Vector3fc offset) {
        xOff = offset.x();
        yOff = offset.y();
        zOff = offset.z();
    }

    @Override
    public void close() {
        if (currentIndex >= 0) {
            if (currentIndex == 3) {
                flushQuad();
            }
            currentIndex = -1; // safety, to make sure we start at vertex 0 with next addVertex call
        }
        this.targetBuilder = null;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        var vertex = nextVertex();
        vertex.x = xOff + x;
        vertex.y = yOff + y;
        vertex.z = zOff + z;
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlayCoords, int lightCoords, float nx, float ny, float nz) {
        var vertex = nextVertex();
        vertex.x = xOff + x;
        vertex.y = yOff + y;
        vertex.z = zOff + z;
        vertex.color = ColorARGB.toABGR(color);
        vertex.u = u;
        vertex.v = v;
        vertex.light = lightCoords;
        vertex.vanillaNormal = NormI8.pack(nx, ny, nz);
    }

    @Override
    public void putBlockBakedQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        if (currentIndex == 3) {
            flushQuad();
        }

        var quadView = (BakedQuadView)(Object)quad;
        int vanillaNormal = NormI8.pack(quad.direction().getStepX(), quad.direction().getStepY(), quad.direction().getStepZ());
        int trueNormal = quadView.getComputedFaceNormal();
        int lightEmission = quad.materialInfo().lightEmission();
        var bakedColors = quad.bakedColors();
        float ox = xOff + x, oy = yOff + y, oz = zOff + z;
        var vertices = this.vertices;

        for (int i = 0; i < 4; i++) {
            var vertex = vertices[i];
            Vector3fc pos = quad.position(i);
            long packedUv = quad.packedUV(i);
            vertex.x = ox + pos.x();
            vertex.y = oy + pos.y();
            vertex.z = oz + pos.z();
            vertex.color = ColorARGB.toABGR(ARGB.multiply(instance.getColor(i), bakedColors.color(i)));
            vertex.u = UVPair.unpackU(packedUv);
            vertex.v = UVPair.unpackV(packedUv);
            vertex.light = instance.getLightCoordsWithEmission(i, lightEmission);
            vertex.vanillaNormal = vanillaNormal;
            vertex.trueNormal = trueNormal;
        }

        this.targetBuilder.getVertexBuffer(quadView.getNormalFace()).push(vertices, material);
        currentIndex = -1;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        currentVertexObj.color = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        currentVertexObj.color = ColorARGB.toABGR(color);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        var vertex = currentVertexObj;
        vertex.u = u;
        vertex.v = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        currentVertexObj.light = (v << 16) | (u & 0xFFFF);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        currentVertexObj.vanillaNormal = NormI8.pack(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float lineWidth) {
        return this;
    }
}
