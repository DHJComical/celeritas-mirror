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

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[idx * getFormat().getIntegerSize()]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[idx * getFormat().getIntegerSize() + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.getVertexData()[idx * getFormat().getIntegerSize() + 2]);
    }

    @Override
    public int getColor(int idx) {
        var format = getFormat();
        int offset = format.getColorOffset();
        if (offset >= 0) {
            return this.getVertexData()[idx * format.getIntegerSize() + (offset / 4)];
        } else {
            return 0;
        }
    }

    @Override
    public Object celeritas$getSprite() {
        return this.sprite;
    }

    @Override
    public float getTexU(int idx) {
        var format = getFormat();
        int offset = format.getUvOffsetById(0);
        return Float.intBitsToFloat(this.getVertexData()[idx * format.getIntegerSize() + (offset / 4)]);
    }

    @Override
    public float getTexV(int idx) {
        var format = getFormat();
        int offset = format.getUvOffsetById(0);
        return Float.intBitsToFloat(this.getVertexData()[idx * format.getIntegerSize() + (offset / 4) + 1]);
    }

    @Override
    public int getLight(int idx) {
        var format = getFormat();
        if (format.hasUvOffset(1)) {
            int offset = format.getUvOffsetById(1);
            return this.getVertexData()[idx * format.getIntegerSize() + (offset / 4)];
        } else {
            return 0;
        }
    }

    @Override
    public int getForgeNormal(int idx) {
        var format = getFormat();
        int offset = format.getNormalOffset();
        if (offset >= 0) {
            return this.getVertexData()[idx * format.getIntegerSize() + (offset / 4)];
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
