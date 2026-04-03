package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

//? if <1.17 {

/*import com.mojang.blaze3d.systems.RenderSystem;
import org.embeddedt.embeddium.impl.Celeritas;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.spongepowered.asm.mixin.*;

@Mixin(TextureAtlasSprite.class)
public abstract class TASMixin_Pre118 {
    @Shadow
    private int subFrame;

    @Shadow
    @Final
    private AnimationMetadataSection metadata;

    @Shadow
    private int frame;

    @Shadow
    public abstract int getFrameCount();

    @Shadow
    protected abstract void upload(int int_1);

    @Shadow
    @Final
    private TextureAtlasSprite.InterpolationData interpolationData;

    @Shadow @Final private float u0;

    @Shadow
    public abstract boolean isAnimation();

    /^*
     * @author JellySquid
     * @reason Allow conditional texture updating
     ^/
    @Overwrite
    public void cycleFrames() {
        this.subFrame++;

        boolean onDemand = Celeritas.options().performance.animateOnlyVisibleTextures;

        if (!onDemand || ((SpriteContentsExtended)this).sodium$isActive()) {
            this.uploadTexture();
        } else {
            // Check and update the frame index anyway to avoid getting out of sync
            if (this.subFrame >= this.metadata.getFrameTime(this.frame)) {
                int frameCount = this.metadata.getFrameCount() == 0 ? this.getFrameCount() : this.metadata.getFrameCount();
                this.frame = (this.frame + 1) % frameCount;
                this.subFrame = 0;
            }
        }
    }

    private void uploadTexture() {
        if (this.subFrame >= this.metadata.getFrameTime(this.frame)) {
            int prevFrameIndex = this.metadata.getFrameIndex(this.frame);
            int frameCount = this.metadata.getFrameCount() == 0 ? this.getFrameCount() : this.metadata.getFrameCount();

            this.frame = (this.frame + 1) % frameCount;
            this.subFrame = 0;

            int frameIndex = this.metadata.getFrameIndex(this.frame);

            if (prevFrameIndex != frameIndex && frameIndex >= 0 && frameIndex < this.getFrameCount()) {
                this.upload(frameIndex);
            }
        } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(this::updateInterpolatedTexture);
            } else {
                this.updateInterpolatedTexture();
            }
        }

        ((SpriteContentsExtended)this).sodium$setActive(false);
    }

    private void updateInterpolatedTexture() {
        this.interpolationData.uploadInterpolatedFrame();
    }
}

*///?}