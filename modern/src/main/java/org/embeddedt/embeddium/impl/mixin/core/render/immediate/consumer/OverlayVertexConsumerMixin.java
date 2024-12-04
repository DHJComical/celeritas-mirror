package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

// TODO be less lazy with the preprocessor

//? if >=1.20 {
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.TextureAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import net.minecraft.core.Direction;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheetedDecalTextureGenerator.class)
public class OverlayVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private Matrix3f normalInversePose;

    @Shadow
    @Final
    private Matrix4f cameraInversePose;

    @Shadow
    @Final
    private float textureScale;

    @Unique
    private boolean isFullWriter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format,
                this.normalInversePose, this.cameraInversePose, this.textureScale);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param inverseNormalMatrix The inverted normal matrix
     * @param inverseTextureMatrix The inverted texture matrix
     * @param textureScale The amount which the overlay texture should be adjusted
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormatDescription format,
                                  Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix, float textureScale) {
        long stride = format.stride();

        var offsetPosition = format.getElementOffset(CommonVertexAttribute.POSITION);
        var offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);
        var offsetNormal = format.getElementOffset(CommonVertexAttribute.NORMAL);
        var offsetTexture = format.getElementOffset(CommonVertexAttribute.TEXTURE);

        int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

        var normal = new Vector3f(Float.NaN);
        var position = new Vector4f(Float.NaN);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            position.x = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
            position.y = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
            position.z = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);
            position.w = 1.0f;

            int packedNormal = MemoryUtil.memGetInt(ptr + offsetNormal);
            normal.x = NormI8.unpackX(packedNormal);
            normal.y = NormI8.unpackY(packedNormal);
            normal.z = NormI8.unpackZ(packedNormal);

            Vector3f transformedNormal = inverseNormalMatrix.transform(normal);
            //? >=1.21.2
            /*Direction direction = Direction.getApproximateNearest(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());*/
            //? <1.21.2
            Direction direction = Direction.getNearest(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());

            Vector4f transformedTexture = inverseTextureMatrix.transform(position);
            transformedTexture.rotateY(3.1415927F);
            transformedTexture.rotateX(-1.5707964F);
            transformedTexture.rotate(direction.getRotation());

            float textureU = -transformedTexture.x() * textureScale;
            float textureV = -transformedTexture.y() * textureScale;

            ColorAttribute.set(ptr + offsetColor, color);
            TextureAttribute.put(ptr + offsetTexture, textureU, textureV);

            ptr += stride;
        }
    }
}
//?} else if >=1.18 {

/*import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.TextureAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import net.minecraft.core.Direction;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheetedDecalTextureGenerator.class)
public class OverlayVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private com.mojang.math.Matrix3f normalInversePose;

    private Matrix3f jomlNormalInverse;

    @Shadow
    @Final
    private com.mojang.math.Matrix4f cameraInversePose;

    private Matrix4f jomlCameraInverse;

    @Unique
    private boolean isFullWriter;

    @Unique
    private Quaternionf jomlQuaternion = new Quaternionf();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
        this.jomlNormalInverse = JomlHelper.copy(this.normalInversePose);
        this.jomlCameraInverse = JomlHelper.copy(this.cameraInversePose);
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format, this.jomlNormalInverse, this.jomlCameraInverse);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /^*
     * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param inverseNormalMatrix The inverted normal matrix
     * @param inverseTextureMatrix The inverted texture matrix
     ^/
    @Unique
    private void transform(long ptr, int count, VertexFormatDescription format,
                           Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix) {
        long stride = format.stride();

        var offsetPosition = format.getElementOffset(CommonVertexAttribute.POSITION);
        var offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);
        var offsetNormal = format.getElementOffset(CommonVertexAttribute.NORMAL);
        var offsetTexture = format.getElementOffset(CommonVertexAttribute.TEXTURE);

        int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

        var normal = new Vector3f(Float.NaN);
        var position = new Vector4f(Float.NaN);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            position.x = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
            position.y = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
            position.z = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);
            position.w = 1.0f;

            int packedNormal = MemoryUtil.memGetInt(ptr + offsetNormal);
            normal.x = NormI8.unpackX(packedNormal);
            normal.y = NormI8.unpackY(packedNormal);
            normal.z = NormI8.unpackZ(packedNormal);

            Vector3f transformedNormal = inverseNormalMatrix.transform(normal);
            Direction direction = Direction.getNearest(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());

            Vector4f transformedTexture = inverseTextureMatrix.transform(position);
            transformedTexture.rotateY(3.1415927F);
            transformedTexture.rotateX(-1.5707964F);
            transformedTexture.rotate(JomlHelper.copy(jomlQuaternion, direction.getRotation()));

            float textureU = -transformedTexture.x();
            float textureV = -transformedTexture.y();

            ColorAttribute.set(ptr + offsetColor, color);
            TextureAttribute.put(ptr + offsetTexture, textureU, textureV);

            ptr += stride;
        }
    }
}
*///?} else if >=1.15 {

/*import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.TextureAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import net.minecraft.core.Direction;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.16 {
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
@Mixin(SheetedDecalTextureGenerator.class)
//?} else {
/^import com.mojang.blaze3d.vertex.BreakingTextureGenerator;
@Mixin(BreakingTextureGenerator.class)
^///?}
public class OverlayVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    //? if >=1.16 {
    private Matrix3f normalInversePose;
    //?} else {
    /^private Matrix3f normalPose;

    @Unique
    private Matrix3f normalInversePose;
    ^///?}

    @Shadow
    @Final
    private Matrix4f cameraInversePose;

    @Unique
    private boolean isFullWriter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
        //? if <1.16
        /^this.normalInversePose = this.normalPose;^/
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format,
                this.normalInversePose, this.cameraInversePose);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /^*
     * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param inverseNormalMatrix The inverted normal matrix
     * @param inverseTextureMatrix The inverted texture matrix
     ^/
    @Unique
    private void transform(long ptr, int count, VertexFormatDescription format,
                           Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix) {
        long stride = format.stride();

        var offsetPosition = format.getElementOffset(CommonVertexAttribute.POSITION);
        var offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);
        var offsetNormal = format.getElementOffset(CommonVertexAttribute.NORMAL);
        var offsetTexture = format.getElementOffset(CommonVertexAttribute.TEXTURE);

        int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

        var normal = new Vector3f();
        var position = new Vector4f();

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            position.set(
                    MemoryUtil.memGetFloat(ptr + offsetPosition + 0),
                    MemoryUtil.memGetFloat(ptr + offsetPosition + 4),
                    MemoryUtil.memGetFloat(ptr + offsetPosition + 8),
                    1.0f
            );

            int packedNormal = MemoryUtil.memGetInt(ptr + offsetNormal);
            normal.set(
                    NormI8.unpackX(packedNormal),
                    NormI8.unpackY(packedNormal),
                    NormI8.unpackX(packedNormal)
            );

            normal.transform(inverseNormalMatrix);
            Direction direction = Direction.getNearest(normal.x(), normal.y(), normal.z());

            position.transform(inverseTextureMatrix);
            position.transform(Vector3f.YP.rotation(3.1415927F));
            position.transform(Vector3f.XP.rotation(-1.5707964F));
            position.transform(direction.getRotation());

            float textureU = -position.x();
            float textureV = -position.y();

            ColorAttribute.set(ptr + offsetColor, color);
            TextureAttribute.put(ptr + offsetTexture, textureU, textureV);

            ptr += stride;
        }
    }
}
*///?}