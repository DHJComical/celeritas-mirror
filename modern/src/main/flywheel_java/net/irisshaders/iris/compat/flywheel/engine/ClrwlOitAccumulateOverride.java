package net.irisshaders.iris.compat.flywheel.engine;

import net.irisshaders.iris.gl.texture.InternalTextureFormat;

import java.util.Optional;

public class ClrwlOitAccumulateOverride
{
    private final int drawBuffer;
    private Optional<Integer> coefficientId; // Empty = frontmost
    private InternalTextureFormat format;

    public ClrwlOitAccumulateOverride(int drawBuffer, Optional<Integer> coefficientId, InternalTextureFormat format)
    {
        this.drawBuffer = drawBuffer;
        this.coefficientId = coefficientId;
        this.format = format;
    }

    public ClrwlOitAccumulateOverride(int drawBuffer, Optional<Integer> coefficientId)
    {
        this(drawBuffer, coefficientId, InternalTextureFormat.RGBA8);
    }

    public ClrwlOitAccumulateOverride(int drawBuffer, InternalTextureFormat format)
    {
        this(drawBuffer, Optional.empty(), format);
    }

    public int drawBuffer()
    {
        return drawBuffer;
    }

    public Optional<Integer> coefficientId()
    {
        return coefficientId;
    }

    public void setCoefficientId(Optional<Integer> newValue)
    {
        this.coefficientId = newValue;
    }

    public InternalTextureFormat format()
    {
        return format;
    }

    public void setFormat(InternalTextureFormat format)
    {
        this.format = format;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClrwlOitAccumulateOverride rhs)
        {
            return this.drawBuffer == rhs.drawBuffer
                    && this.coefficientId == rhs.coefficientId
                    && this.format == rhs.format;
        }

        return false;
    }
}
