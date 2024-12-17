package org.embeddedt.embeddium.impl.render.chunk.compile.buffers;

//? if >=1.15
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.impl.common.datastructure.ContextBundle;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.modern.render.chunk.ModernRenderSectionBuiltInfo;
import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.frapi.SpriteFinderCache;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ChunkMeshBufferBuilder[] vertexBuffers;
    private final boolean splitBySide;
    //? if >=1.15
    private final MojangVertexConsumer vertexConsumer = new MojangVertexConsumer();

    private ContextBundle<RenderSection> renderData;

    public BakedChunkModelBuilder(ChunkMeshBufferBuilder[] vertexBuffers, boolean splitBySide) {
        this.vertexBuffers = vertexBuffers;
        this.splitBySide = splitBySide;
    }

    @Override
    public ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing) {
        return splitBySide ? this.vertexBuffers[facing.ordinal()] : this.vertexBuffers[ModelQuadFacing.UNASSIGNED.ordinal()];
    }

    @Override
    public void addSprite(TextureAtlasSprite sprite) {
        this.renderData.getContext(ModernRenderSectionBuiltInfo.ANIMATED_SPRITES).add(sprite);
    }

    //? if >=1.15 {
    @Override
    public ChunkModelVertexConsumer asVertexConsumer(Material material, BlockRenderContext ctx) {
        this.vertexConsumer.initialize(material, ctx);
        return this.vertexConsumer;
    }
    //?}

    public void destroy() {
        for (ChunkMeshBufferBuilder builder : this.vertexBuffers) {
            if(builder != null) {
                builder.destroy();
            }
        }
    }

    public void begin(ContextBundle<RenderSection> renderData, int sectionIndex) {
        this.renderData = renderData;

        for (var vertexBuffer : this.vertexBuffers) {
            if(vertexBuffer != null) {
                vertexBuffer.start(sectionIndex);
            }
        }
    }

    //? if >=1.15 {
    class MojangVertexConsumer implements ChunkModelVertexConsumer {
        private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
        private ChunkVertexEncoder.Vertex currentVertexObj;
        private int currentIndex = -1;
        private final Vector3f computedNormal = new Vector3f();
        private Material material;
        private @Nullable BlockRenderContext ctx;
        private float xOff, yOff, zOff;

        private boolean hasDefaultColor;
        private int defaultColor;

        private void initialize(Material material, @Nullable BlockRenderContext ctx) {
            if((currentIndex + 1) == 4) {
                // Flush the last quad that was rendered, so the material isn't switched underneath it.
                flushQuad();
            }
            this.material = material;
            this.ctx = ctx;
            this.xOff = this.yOff = this.zOff = 0;
            this.currentIndex = -1;
            this.hasDefaultColor = false;
        }

        private void applyDefaultColor() {
            if(hasDefaultColor) {
                int color = defaultColor;
                var vertices = this.vertices;
                for(int i = 0; i < 4; i++) {
                    vertices[i].color = color;
                }
            }
        }

        private void flushQuad() {
            applyDefaultColor();
            triggerSpriteAnimation();
            var n = computedNormal;
            ModelQuadUtil.calculateNormal(vertices, n);
            var facing = ModelQuadUtil.findNormalFace(n.x, n.y, n.z);
            getVertexBuffer(facing).push(vertices, material, ctx);
            currentIndex = -1;
        }

        private void triggerSpriteAnimation() {
            float uTotal = 0, vTotal = 0;
            var vertices = this.vertices;
            for(int i = 0; i < 4; i++) {
                var vertex = vertices[i];
                uTotal += vertex.u;
                vTotal += vertex.v;
            }
            var sprite = SpriteFinderCache.forBlockAtlas().findNearestSprite(uTotal / 4, vTotal / 4);
            if(sprite != null) {
                addSprite(sprite);
            }
        }

        private int flushLastVertex() {
            int nextIndex = currentIndex + 1;
            if(nextIndex == 4) {
                flushQuad();
                nextIndex = 0;
            }
            currentIndex = nextIndex;
            return nextIndex;
        }

        @Override
        public void embeddium$setOffset(Vector3fc offset) {
            xOff = offset.x();
            yOff = offset.y();
            zOff = offset.z();
        }

        @Override
        public void close() {
            if(currentIndex >= 0) {
                flushLastVertex();
                currentIndex = -1; // safety, to make sure we start at vertex 0 with next addVertex call
            }
            this.ctx = null;
        }

        @Override
        //? if <1.21
        public VertexConsumer vertex(double x, double y, double z) {
        //? if >=1.21
        /*public VertexConsumer addVertex(float x, float y, float z) {*/
            int index = flushLastVertex();
            var vertex = this.vertices[index];
            vertex.x = xOff + (float)x;
            vertex.y = yOff + (float)y;
            vertex.z = zOff + (float)z;
            currentVertexObj = vertex;
            return this;
        }

        @Override
        //? if <1.21
        public VertexConsumer color(int r, int g, int b, int a) {
        //? if >=1.21
        /*public VertexConsumer setColor(int r, int g, int b, int a) {*/
            currentVertexObj.color = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
            return this;
        }

        @Override
        //? if <1.21
        public VertexConsumer uv(float u, float v) {
        //? if >=1.21
        /*public VertexConsumer setUv(float u, float v) {*/
            var vertex = currentVertexObj;
            vertex.u = u;
            vertex.v = v;
            return this;
        }

        @Override
        //? if <1.21
        public VertexConsumer overlayCoords(int p_350815_, int p_350629_) {
        //? if >=1.21
        /*public VertexConsumer setUv1(int p_350815_, int p_350629_) {*/
            return this;
        }

        @Override
        //? if <1.21
        public VertexConsumer uv2(int p_350859_, int p_351004_) {
        //? if >=1.21
        /*public VertexConsumer setUv2(int p_350859_, int p_351004_) {*/
            currentVertexObj.light = (p_351004_ << 16) | (p_350859_ & 0xFFFF);
            return this;
        }

        @Override
        //? if <1.21
        public VertexConsumer normal(float p_350429_, float p_350286_, float p_350836_) {
        //? if >=1.21
        /*public VertexConsumer setNormal(float p_350429_, float p_350286_, float p_350836_) {*/
            return this;
        }

        //? if >=1.17 <1.21 {

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            defaultColor = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
            hasDefaultColor = true;
        }

        @Override
        public void unsetDefaultColor() {
            hasDefaultColor = false;
        }

        //?}

        //? if <1.21 {
        @Override
        public void endVertex() {
            // NO-OP: since on 1.21 this doesn't exist, everything is implemented in such a way that it doesn't matter
        }
        //?}
    }
    //?}
}
