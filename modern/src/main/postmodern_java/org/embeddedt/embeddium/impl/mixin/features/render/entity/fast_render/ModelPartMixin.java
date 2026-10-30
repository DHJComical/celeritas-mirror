package org.embeddedt.embeddium.impl.mixin.features.render.entity.fast_render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// Inject after most other mods
@Mixin(value = ModelPart.class, priority = 1500)
public class ModelPartMixin {
    // Model parts are only posed on the render thread.
    @Unique
    private static final Quaternionf celeritas$scratchRotation = new Quaternionf();

    @Shadow
    public float x;
    @Shadow
    public float y;
    @Shadow
    public float z;
    @Shadow
    public float xRot;
    @Shadow
    public float yRot;
    @Shadow
    public float zRot;
    @Shadow
    public float xScale;
    @Shadow
    public float yScale;
    @Shadow
    public float zScale;

    /**
     * @author JellySquid
     * @reason Skip identity transforms and avoid allocating a quaternion per rotated part
     */
    @Overwrite
    public void translateAndRotate(PoseStack poseStack) {
        if (this.x != 0.0F || this.y != 0.0F || this.z != 0.0F) {
            poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        }

        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose(celeritas$scratchRotation.rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }
}
