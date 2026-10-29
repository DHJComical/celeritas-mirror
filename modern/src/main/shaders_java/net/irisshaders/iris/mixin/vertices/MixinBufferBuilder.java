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
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.embeddedt.embeddium.impl.render.vertex.buffer.FastVertexBuilder;
import org.embeddedt.embeddium.impl.render.vertex.buffer.FastVertexExtension;
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

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements BlockSensitiveBufferBuilder, ExtendingBufferBuilder, FastVertexExtension {
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
	private byte currentLightEmission;
	@Unique
	private int currentLocalPosX;
	@Unique
	private int currentLocalPosY;
	@Unique
	private int currentLocalPosZ;
	@Unique
	private VertexFormat iris$extendedFormat;
	@Unique
	private VertexFormatDescription iris$description;
	@Unique
	private int iris$offsetPosition;
	@Unique
	private int iris$offsetNormal;
	@Unique
	private int iris$offsetEntity;
	@Unique
	private int iris$offsetMidBlock;
	@Unique
	private int iris$offsetMidTexture;
	@Unique
	private int iris$offsetTangent;
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
		iris$extendedFormat = null;
		iris$description = null;

		if (iris$shouldNotExtend || !WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
			return format;
		}

		if (format == DefaultVertexFormat.BLOCK || format == IrisVertexFormats.TERRAIN) {
			extending = true;
			iris$isTerrain = true;
			injectNormalAndUV1 = false;
			return iris$cacheOffsets(IrisVertexFormats.TERRAIN);
		} else if (format == DefaultVertexFormat.NEW_ENTITY || format == IrisVertexFormats.ENTITY) {
			extending = true;
			iris$isTerrain = false;
			injectNormalAndUV1 = false;
			return iris$cacheOffsets(IrisVertexFormats.ENTITY);
		} else if (format == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || format == IrisVertexFormats.GLYPH) {
			extending = true;
			iris$isTerrain = false;
			injectNormalAndUV1 = true;
			return iris$cacheOffsets(IrisVertexFormats.GLYPH);
		}

		return format;
	}

	/**
	 * Resolves the element offsets of the extended format once, instead of once per vertex.
	 */
	@Unique
	private VertexFormat iris$cacheOffsets(VertexFormat format) {
		VertexFormatDescription description = VertexFormatRegistry.instance().get(format);

		iris$offsetPosition = description.getElementOffset(DefaultVertexFormat.ELEMENT_POSITION);
		iris$offsetNormal = description.getElementOffset(DefaultVertexFormat.ELEMENT_NORMAL);
		iris$offsetMidTexture = description.getElementOffset(IrisVertexFormats.MID_TEXTURE_ELEMENT);
		iris$offsetTangent = description.getElementOffset(IrisVertexFormats.TANGENT_ELEMENT);

		if (iris$isTerrain) {
			iris$offsetEntity = description.getElementOffset(IrisVertexFormats.ENTITY_ELEMENT);
			iris$offsetMidBlock = description.getElementOffset(IrisVertexFormats.MID_BLOCK_ELEMENT);
		} else {
			iris$offsetEntity = description.getElementOffset(IrisVertexFormats.ENTITY_ID_ELEMENT);
			iris$offsetMidBlock = -1;
		}

		iris$description = description;
		iris$extendedFormat = format;

		return format;
	}

	@Inject(method = "reset()V", at = @At("HEAD"))
	private void iris$onReset(CallbackInfo ci) {
		vertexCount = 0;
	}

	@Inject(method = "endVertex", at = @At("HEAD"))
	private void iris$beforeNext(CallbackInfo ci) {
		if (!extending || ((FastVertexBuilder) (Object) this).embeddium$isFastPath()) {
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
			this.putInt(0, ExtendedDataHelper.computeMidBlock(x, y, z, currentLocalPosX, currentLocalPosY, currentLocalPosZ) | ((currentLightEmission & 0xFF) << 24));
			this.nextElement();
		}

		vertexCount++;

		if (mode == VertexFormat.Mode.QUADS && vertexCount == 4 || mode == VertexFormat.Mode.TRIANGLES && vertexCount == 3) {
			fillExtendedData(vertexCount);
		}
	}

    @Unique
    private final long[] vertexPointers = new long[4];

	/**
	 * Byte offsets rather than addresses: the builder may reallocate its buffer between the
	 * vertices of a primitive, which would leave raw pointers dangling.
	 */
    @Unique
    private final int[] fastVertexOffsets = new int[4];

	@Unique
	private void fillExtendedData(int vertexAmount) {
		vertexCount = 0;

		int stride = format.getVertexSize();
		long writePointer = MemoryUtil.memAddress(buffer, nextElementByte);
		for (int i = 0; i < 4; i++) {
			vertexPointers[i] = writePointer - (long) stride * (vertexAmount - i);
		}

		ExtendedDataHelper.fillExtendedData(polygon, normal, vertexPointers, vertexAmount,
				iris$offsetMidTexture, iris$offsetNormal, iris$offsetTangent, ImmediateState.isRenderingLevel);
	}

	@Unique
	private void putInt(int i, int value) {
		this.buffer.putInt(this.nextElementByte + i, value);
	}

	@Override
	public void beginBlock(short block, short renderType, byte lightEmission, int localPosX, int localPosY, int localPosZ) {
		this.currentBlock = block;
		this.currentRenderType = renderType;
		this.currentLightEmission = lightEmission;
		this.currentLocalPosX = localPosX;
		this.currentLocalPosY = localPosY;
		this.currentLocalPosZ = localPosZ;
	}

	@Override
	public void endBlock() {
		this.currentBlock = -1;
		this.currentRenderType = -1;
		this.currentLightEmission = 0;
		this.currentLocalPosX = 0;
		this.currentLocalPosY = 0;
		this.currentLocalPosZ = 0;
	}

	@Override
	public boolean canUseFastFormat(VertexFormatDescription description) {
		return extending && format == iris$extendedFormat;
	}

	@Override
	public int finishFastVertex(long pointer, int stride, int writtenAttributes) {
		if (!extending || format != iris$extendedFormat) {
			return writtenAttributes;
		}

		if (injectNormalAndUV1 && (writtenAttributes & (1 << 5)) == 0) {
			MemoryUtil.memPutInt(pointer + iris$offsetNormal, 0);
			writtenAttributes |= 1 << 5;
		}

		if (iris$isTerrain) {
			long entity = pointer + iris$offsetEntity;
			MemoryUtil.memPutShort(entity, currentBlock);
			MemoryUtil.memPutShort(entity + 2, currentRenderType);

			long position = pointer + iris$offsetPosition;
			int midBlock = ExtendedDataHelper.computeMidBlock(
					MemoryUtil.memGetFloat(position), MemoryUtil.memGetFloat(position + 4), MemoryUtil.memGetFloat(position + 8),
					currentLocalPosX, currentLocalPosY, currentLocalPosZ) | ((currentLightEmission & 0xFF) << 24);
			MemoryUtil.memPutInt(pointer + iris$offsetMidBlock, midBlock);
		} else {
			long entity = pointer + iris$offsetEntity;
			MemoryUtil.memPutShort(entity, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
			MemoryUtil.memPutShort(entity + 2, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
			MemoryUtil.memPutShort(entity + 4, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
		}

		long midTexture = pointer + iris$offsetMidTexture;
		MemoryUtil.memPutFloat(midTexture, 0.0f);
		MemoryUtil.memPutFloat(midTexture + 4, 0.0f);
		MemoryUtil.memPutInt(pointer + iris$offsetTangent, 0);

		if (mode == VertexFormat.Mode.QUADS || mode == VertexFormat.Mode.TRIANGLES) {
			if (vertexCount < fastVertexOffsets.length) {
				// The builder has not advanced past this vertex yet, so its offset is the current write position
				fastVertexOffsets[vertexCount++] = nextElementByte;
			}

			int primitiveSize;
			if (mode == VertexFormat.Mode.QUADS) {
				primitiveSize = 4;
			} else {
				primitiveSize = 3;
			}
			if (vertexCount == primitiveSize) {
				fillFastExtendedData(primitiveSize);
				vertexCount = 0;
			}
		} else {
			vertexCount = 0;
		}

		return writtenAttributes;
	}

	@Unique
	private void fillFastExtendedData(int vertexAmount) {
		// Resolve the addresses now; the buffer may have been reallocated while the primitive was being written
		for (int i = 0; i < vertexAmount; i++) {
			vertexPointers[i] = MemoryUtil.memAddress(buffer, fastVertexOffsets[i]);
		}

		ExtendedDataHelper.fillExtendedData(polygon, normal, vertexPointers, vertexAmount,
				iris$offsetMidTexture, iris$offsetNormal, iris$offsetTangent, ImmediateState.isRenderingLevel);
	}

	@Override
	public void resetFastVertexState() {
		vertexCount = 0;
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
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/^*
 * Extends the 1.21 pointer-based BufferBuilder without replacing its optimized
 * vertex writer. Iris only fills its additional attributes and derived data.
 ^/
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements VertexConsumer, BlockSensitiveBufferBuilder {
    private static final int DERIVED_ATTRIBUTES = IrisVertexFormats.MID_TEXTURE_ELEMENT.mask() | IrisVertexFormats.TANGENT_ELEMENT.mask();

    @Shadow
    private int elementsToFill;

    @Shadow
    public abstract VertexConsumer setNormal(float x, float y, float z);

    @Shadow
    protected abstract long beginElement(VertexFormatElement element);

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
    private long vertexPointer;

    @Shadow
    private int vertices;

    @Unique
    private final BufferBuilderPolygonView polygon = new BufferBuilderPolygonView();
    @Unique
    private final Vector3f normal = new Vector3f();
    @Unique
    private final long[] vertexPointers = new long[4];
    @Unique
    private int iris$offsetMidTexture = -1;
    @Unique
    private int iris$offsetNormal;
    @Unique
    private int iris$offsetTangent;
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
    private byte currentLightEmission;
    @Unique
    private int currentLocalPosX;
    @Unique
    private int currentLocalPosY;
    @Unique
    private int currentLocalPosZ;

    @ModifyVariable(method = "<init>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/VertexFormatElement;POSITION:Lcom/mojang/blaze3d/vertex/VertexFormatElement;", ordinal = 0), argsOnly = true)
    private VertexFormat iris$extendFormat(VertexFormat format) {
        extending = false;
        iris$isTerrain = false;
        injectNormalAndUV1 = false;

        if (ImmediateState.skipExtension.get() || !WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
            return format;
        }

        if (format == DefaultVertexFormat.BLOCK || format == IrisVertexFormats.TERRAIN) {
            extending = true;
            iris$isTerrain = true;
            return IrisVertexFormats.TERRAIN;
        } else if (format == DefaultVertexFormat.NEW_ENTITY || format == IrisVertexFormats.ENTITY) {
            extending = true;
            return IrisVertexFormats.ENTITY;
        } else if (format == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP || format == IrisVertexFormats.GLYPH) {
            extending = true;
            injectNormalAndUV1 = true;
            return IrisVertexFormats.GLYPH;
        }

        return format;
    }

    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"))
    private void iris$writeVertexMetadata(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!extending) {
            return;
        }

        if (iris$isTerrain) {
            long entity = beginElement(IrisVertexFormats.ENTITY_ELEMENT);
            if (entity != -1L) {
                MemoryUtil.memPutShort(entity, currentBlock);
                MemoryUtil.memPutShort(entity + 2L, currentRenderType);
            }

            long position = vertexPointer + offsetsByElement[VertexFormatElement.POSITION.id()];
            long midBlock = beginElement(IrisVertexFormats.MID_BLOCK_ELEMENT);
            if (midBlock != -1L) {
                MemoryUtil.memPutInt(midBlock, ExtendedDataHelper.computeMidBlock(
                        MemoryUtil.memGetFloat(position), MemoryUtil.memGetFloat(position + 4L), MemoryUtil.memGetFloat(position + 8L),
                        currentLocalPosX, currentLocalPosY, currentLocalPosZ) | ((currentLightEmission & 0xFF) << 24));
            }
        } else {
            long entity = beginElement(IrisVertexFormats.ENTITY_ID_ELEMENT);
            if (entity != -1L) {
                MemoryUtil.memPutShort(entity, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
                MemoryUtil.memPutShort(entity + 2L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
                MemoryUtil.memPutShort(entity + 4L, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
            }
        }
    }

    @Inject(method = "endLastVertex", at = @At("HEAD"))
    private void iris$finishVertex(CallbackInfo ci) {
        if (vertices == 0 || !extending) {
            return;
        }

        // These are derived from the completed primitive and must not be required
        // from callers or from the vanilla element completion check.
        elementsToFill &= ~DERIVED_ATTRIBUTES;

        if (injectNormalAndUV1 && (elementsToFill & VertexFormatElement.NORMAL.mask()) != 0) {
            setNormal(0.0f, 0.0f, 0.0f);
        }

        if (mode != VertexFormat.Mode.QUADS && mode != VertexFormat.Mode.TRIANGLES) {
            iris$vertexCount = 0;
            return;
        }

        iris$vertexCount++;
        int primitiveSize;
        if (mode == VertexFormat.Mode.QUADS) {
            primitiveSize = 4;
        } else {
            primitiveSize = 3;
        }
        if (iris$vertexCount == primitiveSize) {
            fillExtendedData(primitiveSize);
            iris$vertexCount = 0;
        }
    }

    @Unique
    private void fillExtendedData(int vertexAmount) {
        // The format is final, so these are resolved once rather than per primitive
        if (iris$offsetMidTexture < 0) {
            VertexFormatDescription description = VertexFormatRegistry.instance().get(format);
            iris$offsetMidTexture = description.getElementOffset(IrisVertexFormats.MID_TEXTURE_ELEMENT);
            iris$offsetNormal = description.getElementOffset(VertexFormatElement.NORMAL);
            iris$offsetTangent = description.getElementOffset(IrisVertexFormats.TANGENT_ELEMENT);
        }

        // The vertices of a primitive are contiguous, so the pointers are derived from the one that
        // just completed. They cannot be cached as they are written: reserving space for a later
        // vertex may reallocate the buffer and move every earlier vertex with it.
        int stride = format.getVertexSize();
        for (int i = 0; i < vertexAmount; i++) {
            vertexPointers[i] = vertexPointer - (long) stride * (vertexAmount - 1 - i);
        }

        // The 1.21 path historically replaces glyph normals even outside level rendering.
        ExtendedDataHelper.fillExtendedData(polygon, normal, vertexPointers, vertexAmount,
                iris$offsetMidTexture, iris$offsetNormal, iris$offsetTangent, true);
    }

    @Override
    public void beginBlock(short block, short renderType, byte lightEmission, int localPosX, int localPosY, int localPosZ) {
        currentBlock = block;
        currentRenderType = renderType;
        currentLightEmission = lightEmission;
        currentLocalPosX = localPosX;
        currentLocalPosY = localPosY;
        currentLocalPosZ = localPosZ;
    }

    @Override
    public void endBlock() {
        currentBlock = -1;
        currentRenderType = -1;
        currentLightEmission = 0;
        currentLocalPosX = 0;
        currentLocalPosY = 0;
        currentLocalPosZ = 0;
    }
}

*///?}
