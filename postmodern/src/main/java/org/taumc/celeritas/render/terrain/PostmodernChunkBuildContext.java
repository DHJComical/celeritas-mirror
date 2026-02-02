package org.taumc.celeritas.render.terrain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.render.chunk.RenderPassConfiguration;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.joml.Vector3f;

import java.util.Map;

@Getter
public class PostmodernChunkBuildContext extends ChunkBuildContext {
    private final BlockModelShaper blockModels;
    private final SingleThreadedRandomSource randomSource;
    private final Map<Material, VertexTranslator> materialMap = new Reference2ReferenceOpenHashMap<>();
    private final PoseStack poseStack = new PoseStack();

    public PostmodernChunkBuildContext(RenderPassConfiguration<ChunkSectionLayer> renderPassConfiguration) {
        super(renderPassConfiguration);
        this.blockModels = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        this.randomSource = new SingleThreadedRandomSource(42L);
    }

    public VertexConsumer getVertexConsumer(ChunkSectionLayer layer) {
        return getVertexConsumer(buffers.getRenderPassConfiguration().getMaterialForRenderType(layer));
    }

    public VertexConsumer getVertexConsumer(Material material) {
        var c = materialMap.get(material);
        if (c == null) {
            c = new VertexTranslator(material, buffers.get(material));
            materialMap.put(material, c);
        }
        return c;
    }

    public void flushVertexConsumers() {
        materialMap.values().forEach(VertexTranslator::flushQuad);
    }

    private static class VertexTranslator implements VertexConsumer {
        private final ChunkVertexEncoder.Vertex[] quad;
        private final Material material;
        private final ChunkModelBuilder targetBuilder;
        private final Vector3f computedNormal = new Vector3f();
        private ChunkVertexEncoder.Vertex currentVertexObj;
        private int currentIndex;

        private VertexTranslator(Material material, ChunkModelBuilder targetBuilder) {
            this.material = material;
            this.targetBuilder = targetBuilder;
            this.quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
            this.currentIndex = -1;
        }

        private void applyNormal(int packedNormal) {
            var vertices = this.quad;
            for(int i = 0; i < 4; i++) {
                vertices[i].trueNormal = packedNormal;
            }
        }

        private void flushQuad() {
            if (currentIndex != 3) {
                return;
            }
            var n = computedNormal;
            ModelQuadUtil.calculateNormal(quad, n);
            applyNormal(NormI8.pack(n));
            var facing = ModelQuadUtil.findNormalFace(n.x, n.y, n.z);
            this.targetBuilder.getVertexBuffer(facing).push(quad, material);
            currentIndex = -1;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.currentIndex == 3) {
                flushQuad();
            }
            ChunkVertexEncoder.Vertex vertex;
            this.currentVertexObj = vertex = this.quad[++this.currentIndex];
            vertex.x = x;
            vertex.y = y;
            vertex.z = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            currentVertexObj.color = ColorARGB.pack(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            currentVertexObj.color = color;
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
        public VertexConsumer setLight(int packedLight) {
            currentVertexObj.light = packedLight;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            currentVertexObj.light = LightTexture.packWithFraction(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            currentVertexObj.trueNormal = NormI8.pack(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float p_456188_) {
            return this;
        }
    }
}
