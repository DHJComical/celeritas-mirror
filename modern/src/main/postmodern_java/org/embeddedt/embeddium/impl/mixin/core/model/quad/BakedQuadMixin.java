package org.embeddedt.embeddium.impl.mixin.core.model.quad;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.modern.util.ModernQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements BakedQuadView {
    @Shadow
    public abstract Vector3fc position(int index);

    @Shadow
    public abstract long packedUV(int index);

    @Shadow
    public abstract TextureAtlasSprite sprite();

    @Shadow
    public abstract int tintIndex();

    @Shadow
    public abstract Direction direction();

    @Shadow
    public abstract boolean shade();

    @Shadow
    public abstract int lightEmission();

    @Shadow
    public abstract BakedNormals bakedNormals();

    @Shadow
    public abstract BakedColors bakedColors();

    @Shadow
    @Final
    private boolean hasAmbientOcclusion;

    @Unique
    private int flags;

    @Unique
    private int normal;

    @Unique
    private ModelQuadFacing normalFace;

    @Override
    public float getX(int idx) {
        return this.position(idx).x();
    }

    @Override
    public float getY(int idx) {
        return this.position(idx).y();
    }

    @Override
    public float getZ(int idx) {
        return this.position(idx).z();
    }

    @Override
    public int getColor(int idx) {
        return this.bakedColors().color(idx);
    }

    @Override
    public Object celeritas$getSprite() {
        return this.sprite();
    }

    @Override
    public float getTexU(int idx) {
        return UVPair.unpackU(this.packedUV(idx));
    }

    @Override
    public float getTexV(int idx) {
        return UVPair.unpackV(this.packedUV(idx));
    }

    @Override
    public int getLight(int idx) {
        return 0;
    }

    @Override
    public int getVanillaLightEmission() {
        return this.lightEmission();
    }

    @Override
    public int getForgeNormal(int idx) {
        return this.bakedNormals().normal(idx);
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
    public @Nullable SpriteTransparencyLevel getTransparencyLevel() {
        TextureAtlasSprite s = this.sprite();
        if (s != null && (this.flags & ModelQuadFlags.IS_TRUSTED_SPRITE) != 0) {
            return SpriteTransparencyLevel.Holder.getTransparencyLevel(s);
        } else {
            return null;
        }
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex();
    }

    @Override
    public ModelQuadFacing getNormalFace() {
        var face = this.normalFace;
        if (face == null) {
            this.normalFace = face = ModelQuadUtil.findNormalFace(getComputedFaceNormal());
        }
        return face;
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
    public ModelQuadFacing getLightFace() {
        return ModernQuadFacing.fromDirection(this.direction());
    }

    @Override
    @Unique(silent = true) // The target class has a function with the same name in a remapped environment
    public boolean hasShade() {
        return this.shade();
    }

    @Override
    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Override
    public int getVerticesCount() {
        return BakedQuad.VERTEX_COUNT;
    }
}
