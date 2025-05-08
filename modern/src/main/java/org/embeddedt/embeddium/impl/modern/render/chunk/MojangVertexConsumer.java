package org.embeddedt.embeddium.impl.modern.render.chunk;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.data.MinecraftBuiltRenderSectionData;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.texture.TextureAtlasExtended;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;

//? if >=1.15 {
public class MojangVertexConsumer implements VertexConsumer, AutoCloseable {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final TextureAtlas blocksAtlas = (TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
    private ChunkVertexEncoder.Vertex currentVertexObj;
    private int currentIndex = -1;
    private final Vector3f computedNormal = new Vector3f();
    private Material material;
    private @Nullable BlockRenderContext ctx;
    private float xOff, yOff, zOff;
    private ChunkModelBuilder targetBuilder;

    private boolean hasDefaultColor;
    private int defaultColor;

    public MojangVertexConsumer initialize(ChunkModelBuilder targetBuilder, Material material, @Nullable BlockRenderContext ctx) {
        if((currentIndex + 1) == 4) {
            // Flush the last quad that was rendered, so the material isn't switched underneath it.
            flushQuad();
        }
        this.targetBuilder = targetBuilder;
        this.material = material;
        this.ctx = ctx;
        this.xOff = this.yOff = this.zOff = 0;
        this.currentIndex = -1;
        this.hasDefaultColor = false;
        return this;
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

    private void applyNormal(int packedNormal) {
        var vertices = this.vertices;
        for(int i = 0; i < 4; i++) {
            vertices[i].trueNormal = packedNormal;
        }
    }

    private void flushQuad() {
        applyDefaultColor();
        triggerSpriteAnimation();
        var n = computedNormal;
        ModelQuadUtil.calculateNormal(vertices, n);
        applyNormal(NormI8.pack(n));
        var facing = ModelQuadUtil.findNormalFace(n.x, n.y, n.z);
        this.targetBuilder.getVertexBuffer(facing).push(vertices, material);
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
        var sprite = ((TextureAtlasExtended)this.blocksAtlas).celeritas$findFromUV(uTotal / 4, vTotal / 4);
        if (SpriteUtil.hasAnimation(sprite) && this.targetBuilder.getSectionContextBundle() instanceof MinecraftBuiltRenderSectionData<?,?> mcData) {
            //noinspection unchecked
            ((Collection<TextureAtlasSprite>)mcData.animatedSprites).add(sprite);
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
        this.targetBuilder = null;
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
        currentVertexObj.vanillaNormal = NormI8.pack(p_350429_, p_350286_, p_350836_);
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
