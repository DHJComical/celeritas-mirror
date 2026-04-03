package org.embeddedt.embeddium.impl.mixin.features.render.entity.fast_render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.embeddedt.embeddium.api.util.*;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.impl.model.ModelCuboidAccessor;
import org.embeddedt.embeddium.impl.render.immediate.model.EntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

//? if >=1.17 {
import java.util.List;
//?} else {
/*import it.unimi.dsi.fastutil.objects.ObjectList;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
*///?}

// Inject before everyone else
@Mixin(value = ModelPart.class, priority = 700)
public class ModelPartMixin_Early {
    //? if >=1.17 {
    @Shadow
    @Final
    private List<ModelPart.Cube> cubes;
    //?} else {
    /*@Shadow @Final
    private ObjectList<ModelPart.Cube> cubes;
    *///?}

    /**
     * @author JellySquid, embeddedt
     * @reason Rewrite entity rendering to use faster code path. Original approach of replacing the entire render loop
     * had to be neutered to accommodate mods injecting custom logic here and/or mutating the models at runtime.
     */
    @Overwrite
    public void compile(PoseStack.Pose matrixPose, VertexConsumer vertices, int light, int overlay
                        //? if <1.21
                          , float red, float green, float blue, float alpha
                        //? if >=1.21
                          /*,int color*/
    ) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(vertices);

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
            if(writer != null && simpleCuboid != null) {
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
}
