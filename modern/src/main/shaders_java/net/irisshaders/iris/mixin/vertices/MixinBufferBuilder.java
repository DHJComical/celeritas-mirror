package net.irisshaders.iris.mixin.vertices;

/**
 * Dynamically and transparently extends the vanilla vertex formats with additional data
 */
//? if <1.21 {
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.BufferBuilderPolygonView;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.ExtendingBufferBuilder;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisExtendedBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class) // TODO OCULUS: ???
public abstract class MixinBufferBuilder implements BlockSensitiveBufferBuilder, ExtendingBufferBuilder, IrisExtendedBufferBuilder {
	@Unique
	private final BufferBuilderPolygonView polygon = new BufferBuilderPolygonView();
	@Unique
	private final Vector3f normal = new Vector3f();
	@Unique
	private boolean iris$shouldNotExtend;
	@Unique
	private boolean extending;
	@Unique
	private boolean iris$isTerrain;
	@Unique
	private boolean injectNormalAndUV1;
	@Unique
	private int vertexCount;
	@Unique
	private short currentBlock = -1;
	@Unique
	private short currentRenderType = -1;
	@Unique
	private int currentLocalPosX;
	@Unique
	private int currentLocalPosY;
	@Unique
	private int currentLocalPosZ;
	@Shadow
	private ByteBuffer buffer;

	@Shadow
	private VertexFormat.Mode mode;

	@Shadow
	private VertexFormat format;

	@Shadow
	private int nextElementByte;

	@Shadow
	private @Nullable VertexFormatElement currentElement;

	@Shadow
	public abstract void begin(VertexFormat.Mode drawMode, VertexFormat vertexFormat);

	@Shadow
	public abstract void putShort(int i, short s);

	@Shadow
	public abstract void nextElement();

    @Shadow
    public abstract void putFloat(int index, float floatValue);

    @Shadow
    public abstract void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, int overlayUV, int lightmapUV, float normalX, float normalY, float normalZ);

    @Override
	public void iris$beginWithoutExtending(VertexFormat.Mode drawMode, VertexFormat vertexFormat) {
		iris$shouldNotExtend = true;
		begin(drawMode, vertexFormat);
		iris$shouldNotExtend = false;
	}

	@ModifyVariable(method = "begin", at = @At("HEAD"), argsOnly = true)
	private VertexFormat iris$extendFormat(VertexFormat format) {
		extending = false;
		iris$isTerrain = false;
		injectNormalAndUV1 = false;

		if (iris$shouldNotExtend || !WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
			return format;
		}

		if (format == DefaultVertexFormat.BLOCK || format == IrisVertexFormats.TERRAIN) {
			extending = true;
			iris$isTerrain = true;
			injectNormalAndUV1 = false;
			return IrisVertexFormats.TERRAIN;
		} else if (format == DefaultVertexFormat.NEW_ENTITY || format == IrisVertexFormats.ENTITY) {
			extending = true;
			iris$isTerrain = false;
			injectNormalAndUV1 = false;
			return IrisVertexFormats.ENTITY;
		} else if (format == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || format == IrisVertexFormats.GLYPH) {
			extending = true;
			iris$isTerrain = false;
			injectNormalAndUV1 = true;
			return IrisVertexFormats.GLYPH;
		}

		return format;
	}

	@Inject(method = "reset()V", at = @At("HEAD"))
	private void iris$onReset(CallbackInfo ci) {
		vertexCount = 0;
	}

	@Inject(method = "endVertex", at = @At("HEAD"))
	private void iris$beforeNext(CallbackInfo ci) {
		if (!extending) {
			return;
		}

		if (injectNormalAndUV1 && currentElement == DefaultVertexFormat.ELEMENT_NORMAL) {
			this.putInt(0, 0);
			this.nextElement();
		}

		if (iris$isTerrain) {
			// ENTITY_ELEMENT
			this.putShort(0, currentBlock);
			this.putShort(2, currentRenderType);
		} else {
			// ENTITY_ID_ELEMENT
			this.putShort(0, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
			this.putShort(2, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
			this.putShort(4, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
		}

		this.nextElement();

		// MID_TEXTURE_ELEMENT
		this.putFloat(0, 0);
		this.putFloat(4, 0);
		this.nextElement();
		// TANGENT_ELEMENT
		this.putInt(0, 0);
		this.nextElement();
		if (iris$isTerrain) {
			// MID_BLOCK_ELEMENT
			int posIndex = this.nextElementByte - 48;
			float x = buffer.getFloat(posIndex);
			float y = buffer.getFloat(posIndex + 4);
			float z = buffer.getFloat(posIndex + 8);
			this.putInt(0, ExtendedDataHelper.computeMidBlock(x, y, z, currentLocalPosX, currentLocalPosY, currentLocalPosZ));
			this.nextElement();
		}

		vertexCount++;

		if (mode == VertexFormat.Mode.QUADS && vertexCount == 4 || mode == VertexFormat.Mode.TRIANGLES && vertexCount == 3) {
			fillExtendedData(vertexCount);
		}
	}

    @Unique
    private final long[] vertexPointers = new long[4];

	@Unique
	private void fillExtendedData(int vertexAmount) {
		vertexCount = 0;

		int stride = format.getVertexSize();

        long writePointer = nextElementByte;

        for (int i = 0; i < 4; i++) {
            vertexPointers[i] = writePointer - (long)stride * (vertexAmount - i);
        }

		polygon.setup(MemoryUtil.memAddress(buffer), vertexPointers);

		float midU = 0;
		float midV = 0;

		for (int vertex = 0; vertex < vertexAmount; vertex++) {
			midU += polygon.u(vertex);
			midV += polygon.v(vertex);
		}

		midU /= vertexAmount;
		midV /= vertexAmount;

		int midUOffset;
		int midVOffset;
		int normalOffset;
		int tangentOffset;
		if (iris$isTerrain) {
			midUOffset = 16;
			midVOffset = 12;
			normalOffset = 24;
			tangentOffset = 8;
		} else {
			midUOffset = 14;
			midVOffset = 10;
			normalOffset = 24;
			tangentOffset = 6;
		}

		if (vertexAmount == 3) {
			// NormalHelper.computeFaceNormalTri(normal, polygon);	// Removed to enable smooth shaded triangles. Mods rendering triangles with bad normals need to recalculate their normals manually or otherwise shading might be inconsistent.

			for (int vertex = 0; vertex < vertexAmount; vertex++) {
				int packedNormal = buffer.getInt(nextElementByte - normalOffset - stride * vertex); // retrieve per-vertex normal

				int tangent = NormalHelper.computeTangentSmooth(NormI8.unpackX(packedNormal), NormI8.unpackY(packedNormal), NormI8.unpackZ(packedNormal), polygon);

				buffer.putFloat(nextElementByte - midUOffset - stride * vertex, midU);
				buffer.putFloat(nextElementByte - midVOffset - stride * vertex, midV);
				buffer.putInt(nextElementByte - tangentOffset - stride * vertex, tangent);
			}
		} else {
			// Only replace normals if rendering the level (fix from Iris 1.8)
			boolean replaceNormal = ImmediateState.isRenderingLevel;
			NormalHelper.computeFaceNormal(normal, polygon);
			int packedNormal = 0;
			if (replaceNormal) {
				packedNormal = NormI8.pack(normal.x, normal.y, normal.z, 0.0f);
			}
			int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, polygon);

			for (int vertex = 0; vertex < vertexAmount; vertex++) {
				buffer.putFloat(nextElementByte - midUOffset - stride * vertex, midU);
				buffer.putFloat(nextElementByte - midVOffset - stride * vertex, midV);
				if (replaceNormal) {
					buffer.putInt(nextElementByte - normalOffset - stride * vertex, packedNormal);
				}
				buffer.putInt(nextElementByte - tangentOffset - stride * vertex, tangent);
			}
		}
	}

	@Unique
	private void putInt(int i, int value) {
		this.buffer.putInt(this.nextElementByte + i, value);
	}

	@Override
	public void beginBlock(short block, short renderType, int localPosX, int localPosY, int localPosZ) {
		this.currentBlock = block;
		this.currentRenderType = renderType;
		this.currentLocalPosX = localPosX;
		this.currentLocalPosY = localPosY;
		this.currentLocalPosZ = localPosZ;
	}

	@Override
	public void endBlock() {
		this.currentBlock = -1;
		this.currentRenderType = -1;
		this.currentLocalPosX = 0;
		this.currentLocalPosY = 0;
		this.currentLocalPosZ = 0;
	}

	@Override
	public VertexFormat iris$format() {
		return format;
	}

	@Override
	public VertexFormat.Mode iris$mode() {
		return mode;
	}

	@Override
	public boolean iris$extending() {
		return extending;
	}

	@Override
	public boolean iris$isTerrain() {
		return iris$isTerrain;
	}

	@Override
	public boolean iris$injectNormalAndUV1() {
		return injectNormalAndUV1;
	}

	@Override
	public int iris$vertexCount() {
		return vertexCount;
	}

	@Override
	public void iris$incrementVertexCount() {
		vertexCount++;
	}

	@Override
	public void iris$resetVertexCount() {
		vertexCount = 0;
	}

	@Override
	public short iris$currentBlock() {
		return currentBlock;
	}

	@Override
	public short iris$currentRenderType() {
		return currentRenderType;
	}

	@Override
	public int iris$currentLocalPosX() {
		return currentLocalPosX;
	}

	@Override
	public int iris$currentLocalPosY() {
		return currentLocalPosY;
	}

	@Override
	public int iris$currentLocalPosZ() {
		return currentLocalPosZ;
	}
}
//?} else {
/*import com.mojang.blaze3d.vertex.*;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.BufferBuilderPolygonView;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer.ByteBufferBuilderAccessor;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/^*
 * Dynamically and transparently extends the vanilla vertex formats with additional data
 ^/
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements VertexConsumer, BlockSensitiveBufferBuilder {
    @Shadow
    private int elementsToFill;

    @Unique
    private boolean skipEndVertexOnce;

    @Shadow
    public abstract VertexConsumer setNormal(float f, float g, float h);

    @Shadow
    protected abstract long beginElement(VertexFormatElement vertexFormatElement);

    @Shadow
    @Final
    private VertexFormat.Mode mode;
    @Shadow
    @Final
    private VertexFormat format;
    @Shadow
    @Final
    private int[] offsetsByElement;
    @Shadow
    @Final
    private boolean fastFormat;
    @Shadow
    private long vertexPointer;
    @Shadow
    private int vertices;
    @Shadow
    @Final
    private ByteBufferBuilder buffer;
    @Unique
    private final BufferBuilderPolygonView polygon = new BufferBuilderPolygonView();
    @Unique
    private final Vector3f normal = new Vector3f();
    @Unique
    private boolean extending;
    @Unique
    private boolean iris$isTerrain;
    @Unique
    private boolean injectNormalAndUV1;
    @Unique
    private int iris$vertexCount;
    @Unique
    private short currentBlock = -1;
    @Unique
    private short currentRenderType = -1;
    @Unique
    private int currentLocalPosX;
    @Unique
    private int currentLocalPosY;
    @Unique
    private int currentLocalPosZ;

    @Unique
    private long[] vertexOffsets = new long[4];

    @ModifyVariable(method = "<init>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/VertexFormatElement;POSITION:Lcom/mojang/blaze3d/vertex/VertexFormatElement;", ordinal = 0), argsOnly = true)
    private VertexFormat iris$extendFormat(VertexFormat format) {
        iris$isTerrain = false;
        injectNormalAndUV1 = false;

        if (ImmediateState.skipExtension.get() || !WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
            return format;
        }

        if (format == DefaultVertexFormat.BLOCK || format == IrisVertexFormats.TERRAIN) {
            extending = true;
            iris$isTerrain = true;
            injectNormalAndUV1 = false;
            return IrisVertexFormats.TERRAIN;
        } else if (format == DefaultVertexFormat.NEW_ENTITY || format == IrisVertexFormats.ENTITY) {
            extending = true;
            iris$isTerrain = false;
            injectNormalAndUV1 = false;
            return IrisVertexFormats.ENTITY;
        } else if (format == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || format == IrisVertexFormats.GLYPH) {
            extending = true;
            iris$isTerrain = false;
            injectNormalAndUV1 = true;
            return IrisVertexFormats.GLYPH;
        }

        return format;
    }

    @Redirect(method = "addVertex(FFFIFFIIFFF)V", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;fastFormat:Z"))
    private boolean fastFormat(BufferBuilder instance) {
        return this.fastFormat && !extending;
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"))
    private void injectMidBlock(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if ((this.elementsToFill & IrisVertexFormats.MID_BLOCK_ELEMENT.mask()) != 0) {
            long midBlockOffset = this.beginElement(IrisVertexFormats.MID_BLOCK_ELEMENT);
            MemoryUtil.memPutInt(midBlockOffset, ExtendedDataHelper.computeMidBlock(x, y, z, currentLocalPosX, currentLocalPosY, currentLocalPosZ));
        }

        if ((this.elementsToFill & IrisVertexFormats.ENTITY_ELEMENT.mask()) != 0) {
            long offset = this.beginElement(IrisVertexFormats.ENTITY_ELEMENT);
            // ENTITY_ELEMENT
            MemoryUtil.memPutShort(offset, currentBlock);
            MemoryUtil.memPutShort(offset + 2, currentRenderType);
        } else if ((this.elementsToFill & IrisVertexFormats.ENTITY_ID_ELEMENT.mask()) != 0) {
            long offset = this.beginElement(IrisVertexFormats.ENTITY_ID_ELEMENT);
            // ENTITY_ID_ELEMENT
            MemoryUtil.memPutShort(offset, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
            MemoryUtil.memPutShort(offset + 2, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
            MemoryUtil.memPutShort(offset + 4, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
        }
    }

    @Dynamic("Used to skip endLastVertex if the last push was made by Sodium")
    @Inject(method = "push", at = @At("TAIL"), remap = false, require = 0)
    private void iris$skipSodiumChange(CallbackInfo ci) {
        skipEndVertexOnce = true;
    }

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void iris$beforeNext(CallbackInfo ci) {
        if (this.vertices == 0 || !extending) {
            return;
        }

        // We can't fill these yet.
        this.elementsToFill = this.elementsToFill & ~IrisVertexFormats.MID_TEXTURE_ELEMENT.mask();
        this.elementsToFill = this.elementsToFill & ~IrisVertexFormats.TANGENT_ELEMENT.mask();

        if (injectNormalAndUV1 && this.elementsToFill != (this.elementsToFill & ~VertexFormatElement.NORMAL.mask())) {
            this.setNormal(0, 0, 0);
        }

        if (skipEndVertexOnce) {
            skipEndVertexOnce = false;
            return;
        }

        vertexOffsets[iris$vertexCount] = vertexPointer - ((ByteBufferBuilderAccessor)this.buffer).getPointer();

        iris$vertexCount++;

        if (mode == VertexFormat.Mode.QUADS && iris$vertexCount == 4 || mode == VertexFormat.Mode.TRIANGLES && iris$vertexCount == 3) {
            fillExtendedData(iris$vertexCount);
        }
    }

    @Override
    public void beginBlock(short block, short renderType, int localPosX, int localPosY, int localPosZ) {
        this.currentBlock = block;
        this.currentRenderType = renderType;
        this.currentLocalPosX = localPosX;
        this.currentLocalPosY = localPosY;
        this.currentLocalPosZ = localPosZ;
    }

    @Override
    public void endBlock() {
        this.currentBlock = -1;
        this.currentRenderType = -1;
        this.currentLocalPosX = 0;
        this.currentLocalPosY = 0;
        this.currentLocalPosZ = 0;
    }

    @Unique
    private void fillExtendedData(int vertexAmount) {
        iris$vertexCount = 0;

        int stride = format.getVertexSize();

        polygon.setup(((ByteBufferBuilderAccessor)this.buffer).getPointer(), vertexOffsets);

        float midU = 0;
        float midV = 0;

        for (int vertex = 0; vertex < vertexAmount; vertex++) {
            midU += polygon.u(vertex);
            midV += polygon.v(vertex);
        }

        midU /= vertexAmount;
        midV /= vertexAmount;

        int midTexOffset = this.offsetsByElement[IrisVertexFormats.MID_TEXTURE_ELEMENT.id()];
        int normalOffset = this.offsetsByElement[VertexFormatElement.NORMAL.id()];
        int tangentOffset = this.offsetsByElement[IrisVertexFormats.TANGENT_ELEMENT.id()];

        long basePtr = ((ByteBufferBuilderAccessor)this.buffer).getPointer();

        if (vertexAmount == 3) {
            // NormalHelper.computeFaceNormalTri(normal, polygon);	// Removed to enable smooth shaded triangles. Mods rendering triangles with bad normals need to recalculate their normals manually or otherwise shading might be inconsistent.

            for (int vertex = 0; vertex < vertexAmount; vertex++) {
                long newPointer = basePtr + vertexOffsets[vertex];
                int vertexNormal = MemoryUtil.memGetInt(newPointer + normalOffset); // retrieve per-vertex normal

                int tangent = NormalHelper.computeTangentSmooth(NormI8.unpackX(vertexNormal), NormI8.unpackY(vertexNormal), NormI8.unpackZ(vertexNormal), polygon);

                MemoryUtil.memPutFloat(newPointer + midTexOffset, midU);
                MemoryUtil.memPutFloat(newPointer + midTexOffset + 4, midV);
                MemoryUtil.memPutInt(newPointer + tangentOffset, tangent);
            }
        } else {
            NormalHelper.computeFaceNormal(normal, polygon);
            int packedNormal = NormI8.pack(normal.x, normal.y, normal.z, 0.0f);
            int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, polygon);

            for (int vertex = 0; vertex < vertexAmount; vertex++) {
                long newPointer = basePtr + vertexOffsets[vertex];

                MemoryUtil.memPutFloat(newPointer + midTexOffset, midU);
                MemoryUtil.memPutFloat(newPointer + midTexOffset + 4, midV);
                MemoryUtil.memPutInt(newPointer + normalOffset, packedNormal);
                MemoryUtil.memPutInt(newPointer + tangentOffset, tangent);
            }
        }

        Arrays.fill(vertexOffsets, 0);
    }
}

*///?}