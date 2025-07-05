package net.irisshaders.batchedentityrendering.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
//? if >=1.21
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;*/
import com.mojang.blaze3d.vertex.VertexFormat;
import org.embeddedt.embeddium.api.memory.MemoryIntrinsics;
import net.irisshaders.batchedentityrendering.impl.BufferBuilderExt;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(value = BufferBuilder.class, priority = 1010)
public class MixinBufferBuilder_SegmentRendering implements BufferBuilderExt {
    //? if <1.21 {
	@Shadow
	private ByteBuffer buffer;
    @Shadow
    private int nextElementByte;
    @Shadow
    private void ensureVertexCapacity() {
        throw new AssertionError("not shadowed");
    }
    //?} else {
    /*@Shadow
    private ByteBufferBuilder buffer;
    @Shadow
    @Final
    private int vertexSize;
    *///?}

	@Shadow
	private VertexFormat format;

	@Shadow
	private int vertices;

	@Unique
	private boolean dupeNextVertex;

    @Unique
    private boolean dupeNextVertexAfter;

	@Override
	public void splitStrip() {
		if (vertices == 0) {
			// no strip to split, not building.
			return;
		}

		duplicateLastVertex();
        //? if <1.21 {
		dupeNextVertex = true;
        //?} else {
        /*dupeNextVertexAfter = true;
        dupeNextVertex = false;
        *///?}
	}

	private void duplicateLastVertex() {
		int i = this.format.getVertexSize();
        //? if <1.21 {
		MemoryIntrinsics.copyMemory(MemoryUtil.memAddress(this.buffer, this.nextElementByte - i), MemoryUtil.memAddress(this.buffer, this.nextElementByte), i);
		this.nextElementByte += i;
        //?} else {
        /*long l = this.buffer.reserve(this.vertexSize);
        MemoryUtil.memCopy(l - (long)this.vertexSize, l, this.vertexSize);
        *///?}
		++this.vertices;
        //? if <1.21
		this.ensureVertexCapacity();
	}

    //? if <1.21 {
	@Inject(method = "end", at = @At("RETURN"))
	private void batchedentityrendering$onEnd(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> cir) {
		dupeNextVertex = false;
	}
    //?}

	@Inject(method = /*? if <1.21 {*/ "endVertex" /*?} else {*/ /*"endLastVertex" *//*?}*/, at = @At("RETURN"))
	private void batchedentityrendering$onNext(CallbackInfo ci) {
        //? if >=1.21 {
        /*if (dupeNextVertexAfter) {
            dupeNextVertexAfter = false;
            dupeNextVertex = true;
            return;
        }
        *///?}
		if (dupeNextVertex) {
			dupeNextVertex = false;
			duplicateLastVertex();
		}
	}

	@Dynamic
	@Inject(method = "sodium$moveToNextVertex", at = @At("RETURN"), require = 0)
	private void batchedentityrendering$onNextSodium(CallbackInfo ci) {
		if (dupeNextVertex) {
			dupeNextVertex = false;
			duplicateLastVertex();
		}
	}
}
