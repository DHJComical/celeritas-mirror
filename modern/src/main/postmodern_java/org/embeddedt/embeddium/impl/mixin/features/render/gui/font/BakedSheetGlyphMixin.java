package org.embeddedt.embeddium.impl.mixin.features.render.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.embeddedt.embeddium.impl.render.vertex.FastVertexWriter;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedSheetGlyph.class)
public class BakedSheetGlyphMixin {
    @Shadow
    @Final
    private float u0;
    @Shadow
    @Final
    private float u1;
    @Shadow
    @Final
    private float v0;
    @Shadow
    @Final
    private float v1;
    @Shadow
    @Final
    private float left;
    @Shadow
    @Final
    private float right;
    @Shadow
    @Final
    private float up;
    @Shadow
    @Final
    private float down;

    @Shadow
    private static float extraThickness(boolean bold) {
        throw new AssertionError();
    }

    @Shadow
    private float shearTop() {
        throw new AssertionError();
    }

    @Shadow
    private float shearBottom() {
        throw new AssertionError();
    }

    /**
     * @author embeddedt
     * @reason Write each vertex in one call rather than filling its elements one at a time
     */
    @Overwrite
    private void render(boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer builder, int color, boolean bold, int packedLightCoords) {
        float x0 = x + this.left;
        float x1 = x + this.right;
        float y0 = y + this.up;
        float y1 = y + this.down;
        float shearY0 = italic ? this.shearTop() : 0.0F;
        float shearY1 = italic ? this.shearBottom() : 0.0F;
        float extraThickness = extraThickness(bold);
        celeritas$putVertex(builder, pose, x0 + shearY0 - extraThickness, y0 - extraThickness, z, color, this.u0, this.v0, packedLightCoords);
        celeritas$putVertex(builder, pose, x0 + shearY1 - extraThickness, y1 + extraThickness, z, color, this.u0, this.v1, packedLightCoords);
        celeritas$putVertex(builder, pose, x1 + shearY1 + extraThickness, y1 + extraThickness, z, color, this.u1, this.v1, packedLightCoords);
        celeritas$putVertex(builder, pose, x1 + shearY0 + extraThickness, y0 - extraThickness, z, color, this.u1, this.v0, packedLightCoords);
    }

    @Unique
    private static void celeritas$putVertex(VertexConsumer builder, Matrix4fc pose, float x, float y, float z, int color, float u, float v, int light) {
        if (!(builder instanceof FastVertexWriter writer) || !writer.celeritas$putTextVertex(pose, x, y, z, color, u, v, light)) {
            builder.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setLight(light);
        }
    }
}
