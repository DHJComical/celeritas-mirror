package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

//? if >=1.15 <1.21 {
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.util.NormI8;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.LightAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.NormalAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.OverlayAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.PositionAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.TextureAttribute;
import org.embeddedt.embeddium.impl.render.vertex.buffer.FastVertexExtension;
//?}
import org.embeddedt.embeddium.impl.render.vertex.buffer.FastVertexBuilder;
import org.embeddedt.embeddium.api.memory.MemoryIntrinsics;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.embeddedt.embeddium.api.vertex.serializer.VertexSerializerRegistry;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin /*? if >=1.15 <1.21 {*/ extends DefaultedVertexConsumer /*?}*/ implements VertexBufferWriter, FastVertexBuilder /*? if >=1.15 <1.21 {*/, BufferVertexConsumer /*?}*/ {
    @Shadow
    private int vertices;

    //? if <1.21 {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int nextElementByte;

    @Shadow
    private int elementIndex;

    @Shadow
    private boolean building;

    @Shadow
    protected abstract void ensureCapacity(int size);

    @Shadow
    private VertexFormat format;

    //? if >=1.17 {
    @Shadow
    private VertexFormat.Mode mode;
    //?} else {
    /*@Shadow
    private int mode;
    *///?}

    @Unique
    private VertexFormatDescription embeddium$format;

    @Unique
    private int embeddium$stride;

    @Unique
    private int embeddium$attributeOffsetPosition = -1;
    @Unique
    private int embeddium$attributeOffsetColor = -1;
    @Unique
    private int embeddium$attributeOffsetTexture = -1;
    @Unique
    private int embeddium$attributeOffsetOverlay = -1;
    @Unique
    private int embeddium$attributeOffsetLight = -1;
    @Unique
    private int embeddium$attributeOffsetNormal = -1;

    @Unique
    private int embeddium$requiredAttributes;
    @Unique
    private int embeddium$writtenAttributes;
    @Unique
    private long embeddium$vertexPointer;
    @Unique
    private boolean embeddium$vertexActive;
    @Unique
    private boolean embeddium$fastFormat;
    //?}

    //? if >=1.21 {
    /*@Shadow
    @Final
    private com.mojang.blaze3d.vertex.ByteBufferBuilder buffer;

    @Shadow
    private long vertexPointer;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private int elementsToFill;

    @Shadow
    private boolean building;

    @Shadow
    private VertexFormat format;

    @Unique
    private VertexFormatDescription embeddium$format;
    *///?}

    //? if <1.21 {
    @Inject(method = /*? if >=1.15 {*/ "switchFormat" /*?} else {*/ /*{ "begin", "restoreState" } *//*?}*/, at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;format:Lcom/mojang/blaze3d/vertex/VertexFormat;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
    ))
    private void embeddium$onFormatChanged(CallbackInfo ci) {
        this.embeddium$format = VertexFormatRegistry.instance().get(this.format);
        //? if <1.21 {
        this.embeddium$stride = this.format.getVertexSize();
        this.embeddium$bindAttributes(this.embeddium$format);
        // Computed once per format change; this is queried several times per vertex
        this.embeddium$fastFormat = this.embeddium$computeFastFormat();
        //?}
    }

    @Inject(method = { "discard", "reset", "begin" }, at = @At("RETURN"))
    private void embeddium$resetVertexState(CallbackInfo ci) {
        this.embeddium$vertexActive = false;
        this.embeddium$writtenAttributes = 0;

        // switchFormat is a no-op when the format is unchanged, but an extension may have
        // changed its mind about supporting it in the meantime
        if (this.embeddium$format != null) {
            this.embeddium$fastFormat = this.embeddium$computeFastFormat();
        }

        if (this instanceof FastVertexExtension extension) {
            extension.resetFastVertexState();
        }
    }
    //?}

    //? if >=1.21 {
    /*@Inject(method = "<init>", at = @At("RETURN"))
    private void embeddium$onInit(com.mojang.blaze3d.vertex.ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        this.embeddium$format = VertexFormatRegistry.instance().get(format);
    }
    *///?}

    //? if <1.21 {
    @Unique
    private void embeddium$bindAttributes(VertexFormatDescription format) {
        this.embeddium$attributeOffsetPosition = format.containsElement(CommonVertexAttribute.POSITION) ? format.getElementOffset(CommonVertexAttribute.POSITION) : -1;
        this.embeddium$attributeOffsetColor = format.containsElement(CommonVertexAttribute.COLOR) ? format.getElementOffset(CommonVertexAttribute.COLOR) : -1;
        this.embeddium$attributeOffsetTexture = format.containsElement(CommonVertexAttribute.TEXTURE) ? format.getElementOffset(CommonVertexAttribute.TEXTURE) : -1;
        this.embeddium$attributeOffsetOverlay = format.containsElement(CommonVertexAttribute.OVERLAY) ? format.getElementOffset(CommonVertexAttribute.OVERLAY) : -1;
        this.embeddium$attributeOffsetLight = format.containsElement(CommonVertexAttribute.LIGHT) ? format.getElementOffset(CommonVertexAttribute.LIGHT) : -1;
        this.embeddium$attributeOffsetNormal = format.containsElement(CommonVertexAttribute.NORMAL) ? format.getElementOffset(CommonVertexAttribute.NORMAL) : -1;

        this.embeddium$requiredAttributes = 0;
        if (this.embeddium$attributeOffsetPosition != -1) this.embeddium$requiredAttributes |= 1 << 0;
        if (this.embeddium$attributeOffsetColor != -1) this.embeddium$requiredAttributes |= 1 << 1;
        if (this.embeddium$attributeOffsetTexture != -1) this.embeddium$requiredAttributes |= 1 << 2;
        if (this.embeddium$attributeOffsetOverlay != -1) this.embeddium$requiredAttributes |= 1 << 3;
        if (this.embeddium$attributeOffsetLight != -1) this.embeddium$requiredAttributes |= 1 << 4;
        if (this.embeddium$attributeOffsetNormal != -1) this.embeddium$requiredAttributes |= 1 << 5;
    }

    @Unique
    private boolean embeddium$computeFastFormat() {
        if (this.embeddium$format == null || !this.embeddium$format.isSimpleFormat()) {
            return false;
        }

        FastVertexExtension extension = this instanceof FastVertexExtension value ? value : null;
        if (extension != null && extension.canUseFastFormat(this.embeddium$format)) {
            return true;
        }

        for (var element : this.embeddium$format.getElements()) {
            if (element != CommonVertexAttribute.POSITION && element != CommonVertexAttribute.COLOR &&
                    element != CommonVertexAttribute.TEXTURE && element != CommonVertexAttribute.OVERLAY &&
                    element != CommonVertexAttribute.LIGHT && element != CommonVertexAttribute.NORMAL) {
                return false;
            }
        }

        return true;
    }

    @Unique
    private long embeddium$beginFastVertex() {
        if (!this.embeddium$vertexActive) {
            this.ensureCapacity(this.embeddium$stride);
            this.embeddium$vertexPointer = MemoryUtil.memAddress(this.buffer, this.nextElementByte);
            this.embeddium$vertexActive = true;
        }

        return this.embeddium$vertexPointer;
    }

    @Unique
    private void embeddium$putPosition(float x, float y, float z) {
        if (this.embeddium$attributeOffsetPosition != -1 && (this.embeddium$writtenAttributes & (1 << 0)) == 0) {
            PositionAttribute.put(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetPosition, x, y, z);
            this.embeddium$writtenAttributes |= 1 << 0;
        }
    }

    @Unique
    private void embeddium$putColor(int color) {
        if (this.embeddium$attributeOffsetColor != -1 && (this.embeddium$writtenAttributes & (1 << 1)) == 0) {
            ColorAttribute.set(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetColor, color);
            this.embeddium$writtenAttributes |= 1 << 1;
        }
    }

    @Unique
    private void embeddium$putTexture(float u, float v) {
        if (this.embeddium$attributeOffsetTexture != -1 && (this.embeddium$writtenAttributes & (1 << 2)) == 0) {
            TextureAttribute.put(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetTexture, u, v);
            this.embeddium$writtenAttributes |= 1 << 2;
        }
    }

    @Unique
    private void embeddium$putOverlay(int uv) {
        if (this.embeddium$attributeOffsetOverlay != -1 && (this.embeddium$writtenAttributes & (1 << 3)) == 0) {
            OverlayAttribute.set(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetOverlay, uv);
            this.embeddium$writtenAttributes |= 1 << 3;
        }
    }

    @Unique
    private void embeddium$putLight(int uv) {
        if (this.embeddium$attributeOffsetLight != -1 && (this.embeddium$writtenAttributes & (1 << 4)) == 0) {
            LightAttribute.set(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetLight, uv);
            this.embeddium$writtenAttributes |= 1 << 4;
        }
    }

    @Unique
    private void embeddium$putNormal(int normal) {
        if (this.embeddium$attributeOffsetNormal != -1 && (this.embeddium$writtenAttributes & (1 << 5)) == 0) {
            NormalAttribute.set(this.embeddium$beginFastVertex() + this.embeddium$attributeOffsetNormal, normal);
            this.embeddium$writtenAttributes |= 1 << 5;
        }
    }

    @Unique
    private boolean embeddium$shouldDuplicateVertices() {
        //? if >=1.17 {
        return this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP;
        //?} else
        //return false;
    }

    @Unique
    private void embeddium$finishFastVertex() {
        // The calls below force a reload of any field read across them, so the hot values are read once
        final int stride = this.embeddium$stride;

        if (this.defaultColorSet) {
            this.embeddium$putColor(ColorABGR.pack(
                    this.defaultR / 255.0f, this.defaultG / 255.0f,
                    this.defaultB / 255.0f, this.defaultA / 255.0f));
        }

        int written = this.embeddium$writtenAttributes;
        if (this instanceof FastVertexExtension extension) {
            written = extension.finishFastVertex(this.embeddium$beginFastVertex(), stride, written);
        }

        final int required = this.embeddium$requiredAttributes;
        if ((written & required) != required) {
            throw new IllegalStateException("Not filled all elements of the vertex");
        }

        // ensureCapacity reads nextElementByte, so the position must be stored before calling it
        int next = this.nextElementByte + stride;
        this.vertices++;
        this.nextElementByte = next;
        this.embeddium$vertexActive = false;
        this.embeddium$writtenAttributes = 0;
        this.ensureCapacity(stride);

        if (this.embeddium$shouldDuplicateVertices()) {
            final ByteBuffer buffer = this.buffer;
            MemoryIntrinsics.copyMemory(
                    MemoryUtil.memAddress(buffer, next - stride),
                    MemoryUtil.memAddress(buffer, next), stride);
            this.nextElementByte = next + stride;
            this.vertices++;
            this.ensureCapacity(stride);
        }
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.vertex(x, y, z);
        }

        this.embeddium$putPosition((float) x, (float) y, (float) z);
        return (VertexConsumer) (Object) this;
    }

    /**
     * @author JellySquid
     * @reason Write the color attribute directly when the vertex format allows it
     */
    @Overwrite
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        if (this.defaultColorSet) {
            throw new IllegalStateException();
        }
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.color(red, green, blue, alpha);
        }

        this.embeddium$putColor(ColorABGR.pack(red, green, blue, alpha));
        return (VertexConsumer) (Object) this;
    }

    //? if >=1.18 {
    @Override
    public VertexConsumer color(int argb) {
        if (this.defaultColorSet) {
            throw new IllegalStateException();
        }
        if (!this.embeddium$fastFormat) {
            return this.color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
        }

        this.embeddium$putColor(ColorARGB.toABGR(argb));
        return (VertexConsumer) (Object) this;
    }
    //?}

    @Override
    public VertexConsumer uv(float u, float v) {
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.uv(u, v);
        }

        this.embeddium$putTexture(u, v);
        return (VertexConsumer) (Object) this;
    }

    @Override
    public VertexConsumer overlayCoords(int uv) {
        if (!this.embeddium$fastFormat) {
            return this.overlayCoords(uv & 0xFFFF, (uv >>> 16) & 0xFFFF);
        }

        this.embeddium$putOverlay(uv);
        return (VertexConsumer) (Object) this;
    }

    @Override
    public VertexConsumer uv2(int uv) {
        if (!this.embeddium$fastFormat) {
            return this.uv2(uv & 0xFFFF, (uv >>> 16) & 0xFFFF);
        }

        this.embeddium$putLight(uv);
        return (VertexConsumer) (Object) this;
    }

    // The two-argument forms must be overridden as well; the interface defaults write through
    // currentElement()/nextElement(), which the fast path never advances.

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.overlayCoords(u, v);
        }

        this.embeddium$putOverlay(embeddium$packU16x2(u, v));
        return (VertexConsumer) (Object) this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.uv2(u, v);
        }

        this.embeddium$putLight(embeddium$packU16x2(u, v));
        return (VertexConsumer) (Object) this;
    }

    @Unique
    private static int embeddium$packU16x2(int u, int v) {
        return (u & 0xFFFF) | ((v & 0xFFFF) << 16);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        if (!this.embeddium$fastFormat) {
            return BufferVertexConsumer.super.normal(x, y, z);
        }

        this.embeddium$putNormal(NormI8.pack(x, y, z));
        return (VertexConsumer) (Object) this;
    }

    /**
     * @author JellySquid
     * @reason Avoid the per-element bookkeeping when the vertex format allows it
     */
    @Overwrite
    public void endVertex() {
        if (!this.embeddium$fastFormat) {
            if (this.elementIndex != 0) {
                throw new IllegalStateException("Not filled all elements of the vertex");
            }

            ++this.vertices;
            this.ensureCapacity(this.format.getVertexSize());
            if (this.embeddium$shouldDuplicateVertices()) {
                int stride = this.format.getVertexSize();
                MemoryIntrinsics.copyMemory(
                        MemoryUtil.memAddress(this.buffer, this.nextElementByte - stride),
                        MemoryUtil.memAddress(this.buffer, this.nextElementByte), stride);
                this.nextElementByte += stride;
                ++this.vertices;
                this.ensureCapacity(stride);
            }
            return;
        }

        this.embeddium$finishFastVertex();
    }

    /**
     * @author JellySquid
     * @reason Write the entire vertex in one pass when the vertex format allows it
     */
    @Overwrite
    public void vertex(float x, float y, float z,
                       float red, float green, float blue, float alpha,
                       float u, float v, int overlay, int light,
                       float normalX, float normalY, float normalZ) {
        if (this.defaultColorSet) {
            throw new IllegalStateException();
        }
        if (!this.embeddium$fastFormat) {
            this.vertex((double) x, (double) y, (double) z);
            this.color(red, green, blue, alpha);
            this.uv(u, v);
            this.overlayCoords(overlay);
            this.uv2(light);
            this.normal(normalX, normalY, normalZ);
            this.endVertex();
            return;
        }

        long pointer = this.embeddium$beginFastVertex();
        if (this.embeddium$attributeOffsetPosition != -1) PositionAttribute.put(pointer + this.embeddium$attributeOffsetPosition, x, y, z);
        if (this.embeddium$attributeOffsetColor != -1) ColorAttribute.set(pointer + this.embeddium$attributeOffsetColor, ColorABGR.pack(red, green, blue, alpha));
        if (this.embeddium$attributeOffsetTexture != -1) TextureAttribute.put(pointer + this.embeddium$attributeOffsetTexture, u, v);
        if (this.embeddium$attributeOffsetOverlay != -1) OverlayAttribute.set(pointer + this.embeddium$attributeOffsetOverlay, overlay);
        if (this.embeddium$attributeOffsetLight != -1) LightAttribute.set(pointer + this.embeddium$attributeOffsetLight, light);
        if (this.embeddium$attributeOffsetNormal != -1) NormalAttribute.set(pointer + this.embeddium$attributeOffsetNormal, NormI8.pack(normalX, normalY, normalZ));
        this.embeddium$writtenAttributes = 0x3F;
        this.embeddium$finishFastVertex();
    }

//?}

    //? if <1.21 {
    /**
     * Grows the vertex buffer geometrically instead of by a fixed step.
     */
    //? if >=1.17 {
    @ModifyArg(
        method = "ensureCapacity",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/MemoryTracker;resize(Ljava/nio/ByteBuffer;I)Ljava/nio/ByteBuffer;"),
        index = 1
    )
    //?} else {
    /*@ModifyArg(
        method = "ensureCapacity",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/MemoryTracker;createByteBuffer(I)Ljava/nio/ByteBuffer;")
    )
    *///?}
    private int embeddium$growGeometrically(int vanillaSize) {
        int current = this.buffer.capacity();
        int geometric = current + (current >> 1);

        // A buffer this close to Integer.MAX_VALUE can't grow geometrically without overflowing; let vanilla's
        // fixed step take it the rest of the way.
        if (geometric <= current) {
            return vanillaSize;
        }

        return Math.max(vanillaSize, geometric);
    }
    //?}

    @Override
    public boolean canUseIntrinsics() {
        //? if <1.21 {
        return this.embeddium$fastFormat;
        //?} else
        /*return this.embeddium$format != null && this.embeddium$format.isSimpleFormat();*/
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription sourceFormat) {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
        //? if <1.21 {
        if (this.embeddium$vertexActive) {
            throw new IllegalStateException("Cannot push vertices while a vertex is being written");
        }

        int length = count * this.embeddium$stride;
        this.ensureCapacity(length + this.embeddium$stride);
        long dst = MemoryUtil.memAddress(this.buffer, this.nextElementByte);
        if (sourceFormat == this.embeddium$format) {
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            VertexSerializerRegistry.instance().get(sourceFormat, this.embeddium$format).serialize(src, dst, count);
        }
        this.vertices += count;
        this.nextElementByte += length;
        //?} else {
        /*int length = count * this.vertexSize;
        long dst = this.buffer.reserve(length);
        if (sourceFormat == this.embeddium$format) {
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            VertexSerializerRegistry.instance().get(sourceFormat, this.embeddium$format).serialize(src, dst, count);
        }
        if (count > 0) {
            this.vertexPointer = dst + length - this.vertexSize;
        }
        this.elementsToFill = 0;
        this.vertices += count;
        *///?}
    }

    @Override
    public boolean embeddium$isFastPath() {
        //? if <1.21 {
        return this.building && this.embeddium$fastFormat;
        //?} else
        //return false;
    }
}
