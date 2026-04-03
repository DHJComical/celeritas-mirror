package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

//? if >=1.15 <1.21 {
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import org.embeddedt.embeddium.impl.render.vertex.buffer.ExtendedBufferBuilder;
import org.embeddedt.embeddium.impl.render.vertex.buffer.SodiumBufferBuilder;
//?} else if >=1.21 {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
*///?}
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin /*? if >=1.15 <1.21 {*/ extends DefaultedVertexConsumer /*?}*/ implements VertexBufferWriter /*? if >=1.15 <1.21 {*/ , ExtendedBufferBuilder /*?}*/ {
    @Shadow
    private int vertices;

    //? if <1.21 {
    @Shadow
    private ByteBuffer buffer;

    //? if >=1.15 {
    @Shadow
    private int nextElementByte;
    //?}

    @Shadow
    protected abstract void ensureCapacity(int size);

    //? if >=1.17 {
    @Shadow
    private VertexFormat.Mode mode;
    //?} else {
    /*@Shadow
    private int mode;
    *///?}

    @Shadow
    private VertexFormat format;
    @Unique
    private VertexFormatDescription embeddium$format;

    @Unique
    private int stride;

    //? if >=1.15
    private SodiumBufferBuilder fastDelegate;

    @Inject(method = /*? if >=1.15 {*/ "switchFormat" /*?} else {*/ /*{ "begin", "restoreState" } *//*?}*/,
            at = @At(
                    value = "FIELD",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;format:Lcom/mojang/blaze3d/vertex/VertexFormat;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void onFormatChanged(CallbackInfo ci) {
        this.embeddium$format = VertexFormatRegistry.instance()
                .get(format);
        this.stride = format.getVertexSize();
        //? if >=1.15
        this.fastDelegate = this.embeddium$format.isSimpleFormat() && SodiumBufferBuilder.canSupport(this.embeddium$format) ? new SodiumBufferBuilder(this) : null;
    }

    //? if >=1.15 {
    @Inject(method = { "discard", "reset", "begin" }, at = @At("RETURN"))
    private void resetDelegate(CallbackInfo ci) {
        if (this.fastDelegate != null) {
            this.fastDelegate.reset();
        }
    }
    //?}
    //?} else {
    /*@Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    private long vertexPointer;

    @Unique
    private VertexFormatDescription embeddium$format;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private int elementsToFill;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onFormatChanged(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        this.embeddium$format = VertexFormatRegistry.instance().get(format);
    }

    *///?}

    @Override
    public boolean canUseIntrinsics() {
        return this.embeddium$format != null && this.embeddium$format.isSimpleFormat();
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        //? if <1.21 {
        var length = count * this.stride;

        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        this.ensureCapacity(length + this.stride);

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        //? if >=1.15 {
        var dst = MemoryUtil.memAddress(this.buffer, this.nextElementByte);
        //?} else {
        /*var dst = MemoryUtil.memAddress(this.buffer, this.vertices * this.format.getVertexSize());
        *///?}
        //?} else {
        /*var length = count * this.vertexSize;

        // Ensure that there is space for the data we're about to push
        long dst = this.buffer.reserve(length);
        *///?}

        if (format == this.embeddium$format) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertices += count;
        //? if <1.21 {
        this.nextElementByte += length;
        //?} else {
        /*this.vertexPointer = dst + length - this.vertexSize;
        this.elementsToFill = 0;
        *///?}
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.embeddium$format)
                .serialize(src, dst, count);
    }

    //? if >=1.15 <1.21 {

    // Begin ExtendedBufferBuilder impls

    @Override
    public ByteBuffer sodium$getBuffer() {
        return this.buffer;
    }

    @Override
    public int sodium$getElementOffset() {
        return this.nextElementByte;
    }

    @Override
    public VertexFormatDescription sodium$getFormatDescription() {
        return this.embeddium$format;
    }

    @Override
    public SodiumBufferBuilder sodium$getDelegate() {
        return this.fastDelegate;
    }

    @Unique
    private boolean shouldDuplicateVertices() {
        //? if >=1.17 {
        return this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP;
        //?} else
        /*return false;*/
    }

    @Unique
    private void duplicateVertex() {
        MemoryIntrinsics.copyMemory(
                MemoryUtil.memAddress(this.buffer, this.nextElementByte - this.stride),
                MemoryUtil.memAddress(this.buffer, this.nextElementByte),
                this.stride);

        this.nextElementByte += this.stride;
        this.vertices++;

        this.ensureCapacity(this.stride);
    }

    @Override
    public void sodium$moveToNextVertex() {
        this.vertices++;
        this.nextElementByte += this.stride;

        this.ensureCapacity(this.stride);

        if (this.shouldDuplicateVertices()) {
            this.duplicateVertex();
        }
    }

    @Override
    public boolean sodium$usingFixedColor() {
        return this.defaultColorSet;
    }

    //?}
}
