package org.embeddedt.embeddium.impl.mixin.features.render.entity.fast_render;

//? if <1.20 {
/*import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
*///?}
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.impl.model.ModelCuboidAccessor;
import org.embeddedt.embeddium.impl.render.immediate.model.EntityRenderer;
import org.embeddedt.embeddium.impl.render.immediate.model.ModelCuboid;
import org.embeddedt.embeddium.impl.render.immediate.model.ModelPartData;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.model.geom.ModelPart;
import org.embeddedt.embeddium.impl.render.matrix_stack.CachingPoseStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.*;

// Inject after most other mods
@Mixin(value = ModelPart.class, priority = 1500)
public class ModelPartMixin implements ModelPartData {
    @Shadow
    public float x;
    @Shadow
    public float y;
    @Shadow
    public float z;

    //? if >=1.19 {
    @Shadow
    public float xScale;
    @Shadow
    public float yScale;
    @Shadow
    public float zScale;
    @Shadow
    public boolean skipDraw;
    //?}

    @Shadow
    public float yRot;
    @Shadow
    public float xRot;
    @Shadow
    public float zRot;

    @Shadow
    public boolean visible;

    //? if >=1.17 {
    @Shadow @Final
    private List<ModelPart.Cube> cubes;
    @Shadow @Final
    private Map<String, ModelPart> children;
    //?} else {
    /*@Shadow @Final
    private ObjectList<ModelPart.Cube> cubes;
    @Shadow @Final
    private ObjectList<ModelPart> children;
    *///?}

    @Unique
    private ModelPart[] sodium$children;

    @Unique
    private ModelCuboid[] sodium$cuboids;

    @Inject(method = {
            //? if >=1.16
            "/<init>/"
            //? if <1.16
            /*"<init>(Lnet/minecraft/client/model/Model;)V", "<init>(IIII)V"*/
    }, at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        var copies = new ModelCuboid[cubes.size()];

        for (int i = 0; i < cubes.size(); i++) {
            var accessor = (ModelCuboidAccessor) cubes.get(i);
            copies[i] = accessor.sodium$copy();
        }

        this.sodium$cuboids = copies;
        this.sodium$children = children
                //? if >=1.17
                .values()
                .toArray(ModelPart[]::new);
    }

    //? if <1.21
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";
    //? if >=1.21
    /*private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V";*/

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    private void enableCachingBeforePush(PoseStack stack) {
        ((CachingPoseStack)stack).embeddium$setCachingEnabled(true);
        stack.pushPose();
    }

    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void disableCachingAfterPop(PoseStack stack) {
        stack.popPose();
        ((CachingPoseStack)stack).embeddium$setCachingEnabled(false);
    }

    //? if >=1.17 {
    @Redirect(method = RENDER, at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"))
    private <E> Iterator<E> skipAllocIfEmpty(Collection<E> instance) {
        return instance.isEmpty() ? Collections.emptyIterator() : instance.iterator();
    }
    //?}

    /**
     * @author JellySquid, embeddedt
     * @reason Rewrite entity rendering to use faster code path. Original approach of replacing the entire render loop
     * had to be neutered to accommodate mods injecting custom logic here and/or mutating the models at runtime.
     */
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack.Pose matrixPose, VertexConsumer vertices, int light, int overlay,
                          //? if <1.21
                          float red, float green, float blue, float alpha,
                          //? if >=1.21
                          /*int color,*/
                          CallbackInfo ci) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        EntityRenderer.prepareNormals(matrixPose);

        var cubes = this.cubes;
        //? if <1.21
        int packedColor = ColorABGR.pack(red, green, blue, alpha);
        //? if >=1.21
        /*int packedColor = ColorARGB.toABGR(color);*/

        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < cubes.size(); i++) {
            var cube = cubes.get(i);
            var simpleCuboid = ((ModelCuboidAccessor)cube).embeddium$getSimpleCuboid();
            if(simpleCuboid != null) {
                EntityRenderer.renderCuboidFast(matrixPose, writer, simpleCuboid, light, overlay, packedColor);
            } else {
                // Must use slow path as this cube can't be converted to a simple cuboid
                //? if >=1.17 {
                cube.compile(
                //?} else
                /*compileCube(cube,*/
                        matrixPose, vertices, light, overlay,
                        //? if <1.21
                        red, green, blue, alpha
                        //? if >=1.21
                        /*color*/
                );
            }
        }
    }

    //? if <1.17 {
    /*private void compileCube(ModelPart.Cube cube, PoseStack.Pose matrixPose, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        ModelPart.Polygon[] var13 = cube.polygons;

        for (ModelPart.Polygon lv4 : var13) {
            Vector3f lv5 = lv4.normal.copy();
            lv5.transform(matrixPose.normal());
            float l = lv5.x();
            float m = lv5.y();
            float n = lv5.z();

            for (int o = 0; o < 4; ++o) {
                ModelPart.Vertex lv6 = lv4.vertices[o];
                float p = lv6.pos.x() / 16.0F;
                float q = lv6.pos.y() / 16.0F;
                float r = lv6.pos.z() / 16.0F;
                Vector4f lv7 = new Vector4f(p, q, r, 1.0F);
                lv7.transform(matrixPose.pose());
                vertices.vertex(lv7.x(), lv7.y(), lv7.z(), red, green, blue, alpha, lv6.u, lv6.v, overlay, light, l, m, n);
            }
        }
    }
    *///?}

    /**
     * @author JellySquid
     * @reason Apply transform more quickly
     */
    @Overwrite
    public void translateAndRotate(PoseStack matrixStack) {
        if (this.x != 0.0F || this.y != 0.0F || this.z != 0.0F) {
            matrixStack.translate(this.x * (1.0f / 16.0f), this.y * (1.0f / 16.0f), this.z * (1.0f / 16.0f));
        }

        //? if >=1.20 {
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            MatrixHelper.rotateZYX(((CachingPoseStack)matrixStack).celeritas$last(), this.zRot, this.yRot, this.xRot);
        }
        //?} else {
        /*// TODO do rotations without allocating so many Quaternions
        if (this.zRot != 0.0F) {
            matrixStack.mulPose(Vector3f.ZP.rotation(this.zRot));
        }

        if (this.yRot != 0.0F) {
            matrixStack.mulPose(Vector3f.YP.rotation(this.yRot));
        }

        if (this.xRot != 0.0F) {
            matrixStack.mulPose(Vector3f.XP.rotation(this.xRot));
        }
        *///?}

        //? if >=1.19 {
        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrixStack.scale(this.xScale, this.yScale, this.zScale);
        }
        //?}
    }

    @Override
    public ModelCuboid[] getCuboids() {
        return this.sodium$cuboids;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public boolean isHidden() {
        //? if >=1.19 {
        return this.skipDraw;
        //?} else
        /*return false;*/
    }

    @Override
    public ModelPart[] getChildren() {
        return this.sodium$children;
    }
}
