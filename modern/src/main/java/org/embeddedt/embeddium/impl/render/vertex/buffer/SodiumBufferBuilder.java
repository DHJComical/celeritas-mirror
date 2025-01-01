package org.embeddedt.embeddium.impl.render.vertex.buffer;

//? if >=1.15 <1.21 {

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
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
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.embeddedt.embeddium.impl.Celeritas;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;

public class SodiumBufferBuilder implements VertexConsumer, VertexBufferWriter {
    private static final int ATTRIBUTE_NOT_PRESENT = -1;

    private static final int
            ATTRIBUTE_POSITION_BIT  = 1 << 0,
            ATTRIBUTE_COLOR_BIT     = 1 << 1,
            ATTRIBUTE_TEXTURE_BIT   = 1 << 2,
            ATTRIBUTE_OVERLAY_BIT   = 1 << 3,
            ATTRIBUTE_LIGHT_BIT     = 1 << 4,
            ATTRIBUTE_NORMAL_BIT    = 1 << 5;

    private final ExtendedBufferBuilder builder;

    private int
            attributeOffsetPosition,
            attributeOffsetColor,
            attributeOffsetTexture,
            attributeOffsetOverlay,
            attributeOffsetLight,
            attributeOffsetNormal;

    private int requiredAttributes, writtenAttributes;

    private int packedFixedColor;

    private static final Set<VertexFormatElement> SUPPORTED_ELEMENTS = new ReferenceOpenHashSet<>();
    private static final Int2ByteOpenHashMap SUPPORT_CACHE = new Int2ByteOpenHashMap();
    private static final byte SUPPORT_NOT_CHECKED = 127;

    static {
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.POSITION);
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.TEXTURE);
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.OVERLAY);
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.LIGHT);
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.NORMAL);
        SUPPORTED_ELEMENTS.add(CommonVertexAttribute.COLOR);

        SUPPORT_CACHE.defaultReturnValue(SUPPORT_NOT_CHECKED);
    }

    public static boolean canSupport(VertexFormatDescription vertexFormat) {
        // We cache this info for speed
        int id = vertexFormat.id();
        synchronized (SUPPORT_CACHE) {
            byte res = SUPPORT_CACHE.get(id);
            if (res == SUPPORT_NOT_CHECKED) {
                res = (byte)(checkSupport(vertexFormat) ? 1 : 0);
                SUPPORT_CACHE.put(id, res);
            }
            return res == 1;
        }
    }

    private static boolean checkSupport(VertexFormatDescription vertexFormat) {
        for (VertexFormatElement e : vertexFormat.getElements()) {
            if (!SUPPORTED_ELEMENTS.contains(e)) {
                return false;
            }
        }
        return true;
    }

    public SodiumBufferBuilder(ExtendedBufferBuilder builder) {
        this.builder = builder;
        this.resetAttributeBindings();
        this.updateAttributeBindings(this.builder.sodium$getFormatDescription());
    }

    private void resetAttributeBindings() {
        this.requiredAttributes = 0;

        this.attributeOffsetPosition = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetColor = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetTexture = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetOverlay = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetLight = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetNormal = ATTRIBUTE_NOT_PRESENT;
    }

    private void updateAttributeBindings(VertexFormatDescription desc) {
        this.resetAttributeBindings();

        if (desc.containsElement(CommonVertexAttribute.POSITION)) {
            this.requiredAttributes |= ATTRIBUTE_POSITION_BIT;
            this.attributeOffsetPosition = desc.getElementOffset(CommonVertexAttribute.POSITION);
        }

        if (desc.containsElement(CommonVertexAttribute.COLOR)) {
            this.requiredAttributes |= ATTRIBUTE_COLOR_BIT;
            this.attributeOffsetColor = desc.getElementOffset(CommonVertexAttribute.COLOR);
        }

        if (desc.containsElement(CommonVertexAttribute.TEXTURE)) {
            this.requiredAttributes |= ATTRIBUTE_TEXTURE_BIT;
            this.attributeOffsetTexture = desc.getElementOffset(CommonVertexAttribute.TEXTURE);
        }

        if (desc.containsElement(CommonVertexAttribute.OVERLAY)) {
            this.requiredAttributes |= ATTRIBUTE_OVERLAY_BIT;
            this.attributeOffsetOverlay = desc.getElementOffset(CommonVertexAttribute.OVERLAY);
        }

        if (desc.containsElement(CommonVertexAttribute.LIGHT)) {
            this.requiredAttributes |= ATTRIBUTE_LIGHT_BIT;
            this.attributeOffsetLight = desc.getElementOffset(CommonVertexAttribute.LIGHT);
        }

        if (desc.containsElement(CommonVertexAttribute.NORMAL)) {
            this.requiredAttributes |= ATTRIBUTE_NORMAL_BIT;
            this.attributeOffsetNormal = desc.getElementOffset(CommonVertexAttribute.NORMAL);
        }
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        this.builder.push(stack, ptr, count, format);
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.builder.canUseIntrinsics();
    }

    @Unique
    private void putPositionAttribute(float x, float y, float z) {
        if (this.attributeOffsetPosition == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_POSITION_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetPosition);
        PositionAttribute.put(offset, x, y, z);

        this.writtenAttributes |= ATTRIBUTE_POSITION_BIT;
    }


    @Unique
    private void putColorAttribute(int rgba) {
        if (this.attributeOffsetColor == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_COLOR_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetColor);
        ColorAttribute.set(offset, rgba);

        this.writtenAttributes |= ATTRIBUTE_COLOR_BIT;
    }

    @Unique
    private void putTextureAttribute(float u, float v) {
        if (this.attributeOffsetTexture == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_TEXTURE_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetTexture);
        TextureAttribute.put(offset, u, v);

        this.writtenAttributes |= ATTRIBUTE_TEXTURE_BIT;
    }

    @Unique
    private void putOverlayAttribute(int uv) {
        if (this.attributeOffsetOverlay == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_OVERLAY_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetOverlay);
        OverlayAttribute.set(offset, uv);

        this.writtenAttributes |= ATTRIBUTE_OVERLAY_BIT;
    }

    @Unique
    private void putLightAttribute(int uv) {
        if (this.attributeOffsetLight == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_LIGHT_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetLight);
        LightAttribute.set(offset, uv);

        this.writtenAttributes |= ATTRIBUTE_LIGHT_BIT;
    }

    @Unique
    private void putNormalAttribute(int normal) {
        if (this.attributeOffsetNormal == ATTRIBUTE_NOT_PRESENT || (this.writtenAttributes & ATTRIBUTE_NORMAL_BIT) != 0) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset() + this.attributeOffsetNormal);
        NormalAttribute.set(offset, normal);

        this.writtenAttributes |= ATTRIBUTE_NORMAL_BIT;
    }

    @Override
    public void vertex(float x, float y, float z,
                       float red, float green, float blue, float alpha,
                       float u, float v,
                       int overlay, int light,
                       float normalX, float normalY, float normalZ
    ) {
        if (this.builder.sodium$usingFixedColor()) {
            throw new IllegalStateException();
        }

        final long offset = MemoryUtil.memAddress(this.builder.sodium$getBuffer(), this.builder.sodium$getElementOffset());

        if (this.attributeOffsetPosition != ATTRIBUTE_NOT_PRESENT) {
            PositionAttribute.put(offset + this.attributeOffsetPosition, x, y, z);
        }

        if (this.attributeOffsetColor != ATTRIBUTE_NOT_PRESENT) {
            ColorAttribute.set(offset + this.attributeOffsetColor, ColorABGR.pack(red, green, blue, alpha));
        }

        if (this.attributeOffsetTexture != ATTRIBUTE_NOT_PRESENT) {
            TextureAttribute.put(offset + this.attributeOffsetTexture, u, v);
        }

        if (this.attributeOffsetOverlay != ATTRIBUTE_NOT_PRESENT) {
            OverlayAttribute.set(offset + this.attributeOffsetOverlay, overlay);
        }

        if (this.attributeOffsetLight != ATTRIBUTE_NOT_PRESENT) {
            LightAttribute.set(offset + this.attributeOffsetLight, light);
        }

        if (this.attributeOffsetNormal != ATTRIBUTE_NOT_PRESENT) {
            NormalAttribute.set(offset + this.attributeOffsetNormal, NormI8.pack(normalX, normalY, normalZ));
        }

        // It's okay to mark elements as "written to" even if the vertex format does not specify those elements.
        this.writtenAttributes = ATTRIBUTE_POSITION_BIT | ATTRIBUTE_COLOR_BIT | ATTRIBUTE_TEXTURE_BIT |
                ATTRIBUTE_OVERLAY_BIT | ATTRIBUTE_LIGHT_BIT | ATTRIBUTE_NORMAL_BIT;

        this.endVertex();
    }

    //? if >=1.17 {
    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        ((BufferBuilder)this.builder).defaultColor(red, green, blue, alpha);
        this.packedFixedColor = ColorABGR.pack(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        ((BufferBuilder)this.builder).unsetDefaultColor();
    }
    //?}

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.putPositionAttribute((float) x, (float) y, (float) z);

        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        if (this.builder.sodium$usingFixedColor()) {
            throw new IllegalStateException();
        }

        this.putColorAttribute(ColorABGR.pack(red, green, blue, alpha));

        return this;
    }

    //? if >=1.18 {
    @Override
    public VertexConsumer color(int argb) { // No, this isn't a typo. One method takes RGBA, but this one takes ARGB.
        if (this.builder.sodium$usingFixedColor()) {
            throw new IllegalStateException();
        }

        // This should be RGBA.
        // There is no reason it should be anything other than RGBA.
        // It should certainly never be ABGR.
        // But it is.
        // Why?
        this.putColorAttribute(ColorARGB.toABGR(argb));

        return this;
    }
    //?}

    @Override
    public VertexConsumer uv(float u, float v) {
        this.putTextureAttribute(u, v);

        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int uv) {
        this.putOverlayAttribute(uv);

        return this;
    }


    @Override
    public VertexConsumer uv2(int uv) {
        this.putLightAttribute(uv);

        return this;
    }
    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.putNormalAttribute(NormI8.pack(x, y, z));

        return this;
    }

    @Override
    public void endVertex() {
        if (this.builder.sodium$usingFixedColor()) {
            this.putColorAttribute(this.packedFixedColor);
        }

        if (!this.isVertexFinished()) {
            throw new IllegalStateException("Not filled all elements of the vertex");
        }

        this.builder.sodium$moveToNextVertex();

        this.writtenAttributes = 0;
    }

    public void reset() {
        this.writtenAttributes = 0;
    }

    public BufferBuilder getOriginalBufferBuilder() {
        return (BufferBuilder)this.builder;
    }

    private boolean isVertexFinished() {
        return (this.writtenAttributes & this.requiredAttributes) == this.requiredAttributes;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        return this.uv2(packU16x2(u, v));
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this.overlayCoords(packU16x2(u, v));
    }

    @Unique
    private static int packU16x2(int u, int v) {
        return (u & 0xFFFF) << 0 |
                (v & 0xFFFF) << 16;
    }
}

//?}