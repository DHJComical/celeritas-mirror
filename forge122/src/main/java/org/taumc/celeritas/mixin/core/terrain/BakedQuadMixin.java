package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.taumc.celeritas.impl.render.terrain.compile.light.VintageDiffuseProvider;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements BakedQuadView {
    @Shadow
    @Final
    protected EnumFacing face;

    @Shadow
    @Final
    protected boolean applyDiffuseLighting;

    @Shadow
    public abstract int[] getVertexData();

    @Shadow
    public abstract VertexFormat getFormat();

    @Shadow
    @Final
    protected TextureAtlasSprite sprite;
    @Shadow
    @Final
    protected int tintIndex;
    @Unique
    private int flags;

    @Unique
    private int normal;

    @Unique
    private ModelQuadFacing normalFace;

    @Override
    public ModelQuadFacing getNormalFace() {
        var face = this.normalFace;
        if (face == null) {
            this.normalFace = face = ModelQuadUtil.findNormalFace(getComputedFaceNormal());
        }
        return face;
    }

    @Override
    public int getFlags() {
        int f = this.flags;
        if ((f & ModelQuadFlags.IS_POPULATED) == 0) {
            this.flags = f = ModelQuadFlags.getQuadFlags(this, getLightFace(), f);
        }
        return f;
    }

    @Override
    public void addFlags(int flags) {
        this.flags |= flags;
    }

    @Override
    public boolean hasShade() {
        return this.applyDiffuseLighting;
    }

    @Override
    public int getVerticesCount() {
        return this.getVertexData().length / this.getFormat().getIntegerSize();
    }

    @Override
    public @Nullable SpriteTransparencyLevel getTransparencyLevel() {
        return null;
    }

    private static final int BLOCK_VERTEX_SIZE = DefaultVertexFormats.BLOCK.getIntegerSize();
    private static final int POSITION_INDEX = 0;
    private static final int COLOR_INDEX = 3;
    private static final int TEXTURE_INDEX = 4;
    private static final int LIGHT_INDEX = 6;

    private static int vertexOffset(int idx) {
        return idx * BLOCK_VERTEX_SIZE;
    }


    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[vertexOffset(idx) + POSITION_INDEX + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.getVertexData()[vertexOffset(idx) + COLOR_INDEX];
    }

    @Override
    public Object celeritas$getSprite() {
        return this.sprite;
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getLight(int idx) {
        if (this.getFormat() == DefaultVertexFormats.BLOCK) {
            return this.getVertexData()[vertexOffset(idx) + LIGHT_INDEX];
        } else {
            return 0;
        }
    }

    @Override
    public int getForgeNormal(int idx) {
        if (this.getFormat() == DefaultVertexFormats.ITEM) {
            return this.getVertexData()[vertexOffset(idx) + LIGHT_INDEX];
        } else {
            return 0;
        }
    }

    @Override
    public ModelQuadFacing getLightFace() {
        // Handle mods not supplying a light face
        var face = this.face;
        return face == null ? ModelQuadFacing.POS_Y : VintageDiffuseProvider.fromEnumFacing(face);
    }

    @Override
    public int getComputedFaceNormal() {
        int n = this.normal;
        if (n == 0) {
            this.normal = n = ModelQuadUtil.calculateNormal(this);
        }
        return n;
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex;
    }
}
