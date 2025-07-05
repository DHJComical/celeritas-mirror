package org.embeddedt.embeddium.impl.mixin.core.model.quad;

import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.modern.render.chunk.sprite.SpriteTransparencyLevelHolder;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import static org.embeddedt.embeddium.impl.util.ModelQuadUtil.*;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements BakedQuadView {
    @Shadow
    @Final
    protected int[] vertices;

    @Shadow
    @Final
    protected TextureAtlasSprite sprite;

    @Shadow
    @Final
    protected int tintIndex;

    @Shadow
    @Final
    protected Direction direction; // This is really the light face, but we can't rename it.

    //? if >=1.16 {
    @Shadow
    @Final
    private boolean shade;
    //?}

    //? if forgelike && <1.16 {
    /*@Shadow
    @Final
    private boolean applyDiffuseLighting;
    *///?}

    //? if forgelike && >=1.19 {
    @Shadow(remap = false)
    @Final
    private boolean hasAmbientOcclusion;
    //?}

    @Unique
    private int flags;

    @Unique
    private int normal;

    @Unique
    private ModelQuadFacing normalFace;

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + POSITION_INDEX + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.vertices[vertexOffset(idx) + COLOR_INDEX];
    }

    @Override
    public TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getLight(int idx) {
        return this.vertices[vertexOffset(idx) + LIGHT_INDEX];
    }

    @Override
    public int getForgeNormal(int idx) {
        return this.vertices[vertexOffset(idx) + NORMAL_INDEX];
    }

    @Override
    public int getFlags() {
        int f = this.flags;
        if ((f & ModelQuadFlags.IS_POPULATED) == 0) {
            this.flags = f = ModelQuadFlags.getQuadFlags(this, direction, f);
        }
        return f;
    }

    @Override
    public void addFlags(int flags) {
        this.flags |= flags;
    }

    @Override
    public @Nullable SpriteTransparencyLevel getTransparencyLevel() {
        if (this.sprite != null && (this.flags & ModelQuadFlags.IS_TRUSTED_SPRITE) != 0) {
            return SpriteTransparencyLevelHolder.getTransparencyLevel(this.sprite);
        } else {
            return null;
        }
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex;
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
    public Direction getLightFace() {
        return this.direction;
    }

    @Override
    @Unique(silent = true) // The target class has a function with the same name in a remapped environment
    public boolean hasShade() {
        //? if >=1.16 {
        return this.shade;
        //?} else if forgelike && <1.16 {
        /*return this.applyDiffuseLighting;
        *///?} else
        /*return false;*/
    }

    //? if forgelike && >=1.19 {
    @Override
    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }
    //?}

    @Override
    public int getVerticesCount() {
        return this.vertices.length / 8;
    }
}
